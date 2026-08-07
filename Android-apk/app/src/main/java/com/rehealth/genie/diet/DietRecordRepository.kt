package com.rehealth.genie.diet

import com.google.gson.Gson
import com.rehealth.genie.data.sync.SyncRepository
import com.rehealth.genie.data.sync.UploadQueueEntity
import com.rehealth.genie.network.dto.TelemetryBatchRequestDto
import com.rehealth.genie.network.dto.BehaviorRecordDto
import com.rehealth.genie.ring.provider.ActiveWearableBinding
import com.rehealth.genie.ring.provider.WearableCloudIdentity
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class DietRecordRepository(
    private val dao: DietRecordDao,
    private val syncRepository: SyncRepository,
    private val userIdProvider: () -> String?,
    private val wearableBindingProvider: () -> ActiveWearableBinding?,
    private val triggerSync: () -> Unit,
    private val gson: Gson = Gson(),
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    fun observeToday(zoneId: ZoneId = ZoneId.systemDefault()): Flow<List<DietRecordWithUploadState>> {
        val userId = userIdProvider()?.takeIf(String::isNotBlank) ?: return flowOf(emptyList())
        val today = Instant.ofEpochMilli(nowProvider()).atZone(zoneId).toLocalDate()
        val from = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val to = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return dao.observeBetween(userId, from, to)
    }

    suspend fun save(draft: DietRecordDraft): DietSaveResult {
        val userId = userIdProvider()?.takeIf(String::isNotBlank)
            ?: error("登录已失效，请重新登录后记录餐食。")
        validate(draft)
        val now = nowProvider()
        val record = DietRecordEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            consumedAt = draft.consumedAt,
            mealType = draft.mealType,
            description = draft.description.trim(),
            caloriesKcal = draft.caloriesKcal,
            proteinGrams = draft.proteinGrams,
            carbohydrateGrams = draft.carbohydrateGrams,
            fatGrams = draft.fatGrams,
            fiberGrams = draft.fiberGrams,
            sodiumMilligrams = draft.sodiumMilligrams,
            source = SOURCE,
            createdAt = now,
            uploadBatchId = null,
        )
        // Local persistence is authoritative for the user action. Queue/network failures must not lose it.
        dao.insert(record)
        val queued = runCatching { queue(record) }.getOrDefault(false)
        if (queued) triggerSync()
        return DietSaveResult(record, queued)
    }

    /**
     * Mirrors a successfully persisted FOOD photo analysis into the local-first meal log.
     * The server behavior id makes the import idempotent across Activity recreation/retries.
     * Non-food or nutrition-incomplete analyses remain behavior records and are not invented
     * into a meal with placeholder calories.
     */
    suspend fun saveAnalyzedFood(record: BehaviorRecordDto): DietSaveResult? {
        val draft = record.toDietRecordDraftOrNull() ?: return null
        val userId = userIdProvider()?.takeIf(String::isNotBlank)
            ?: error("登录已失效，请重新登录后记录餐食。")
        val sourceId = record.id?.takeIf(String::isNotBlank)
            ?: record.requestId?.takeIf(String::isNotBlank)
            ?: listOf(
                record.occurredAt,
                record.title,
                record.items.joinToString("|"),
                record.caloriesKcal,
            ).joinToString("|")
        val now = nowProvider()
        val entity = DietRecordEntity(
            id = UUID.nameUUIDFromBytes(
                "photo-diet|$userId|$sourceId".toByteArray(StandardCharsets.UTF_8),
            ).toString(),
            userId = userId,
            consumedAt = draft.consumedAt,
            mealType = draft.mealType,
            description = draft.description,
            caloriesKcal = draft.caloriesKcal,
            proteinGrams = draft.proteinGrams,
            carbohydrateGrams = draft.carbohydrateGrams,
            fatGrams = draft.fatGrams,
            fiberGrams = draft.fiberGrams,
            sodiumMilligrams = draft.sodiumMilligrams,
            source = PHOTO_SOURCE,
            createdAt = now,
            uploadBatchId = null,
        )
        val inserted = dao.insertIfAbsent(entity) != -1L
        if (!inserted) return DietSaveResult(entity, queued = false, inserted = false)
        val queued = runCatching { queue(entity) }.getOrDefault(false)
        if (queued) triggerSync()
        return DietSaveResult(entity, queued, inserted = true)
    }

    /**
     * Backfills nutrition-complete FOOD behavior records returned by the server into Room.
     * This covers app restarts and sign-ins where the photo callback is no longer available.
     */
    suspend fun restoreAnalyzedFoods(records: List<BehaviorRecordDto>): Int {
        var insertedCount = 0
        records.filter { it.category.equals("FOOD", ignoreCase = true) }.forEach { record ->
            if (saveAnalyzedFood(record)?.inserted == true) insertedCount += 1
        }
        return insertedCount
    }

    suspend fun preparePendingUploads(): Int {
        val userId = userIdProvider()?.takeIf(String::isNotBlank) ?: return 0
        val queuedCount = dao.findNotQueued(userId).count { record ->
            runCatching { queue(record) }.getOrDefault(false)
        }
        if (queuedCount > 0) triggerSync()
        return queuedCount
    }

    private suspend fun queue(record: DietRecordEntity): Boolean {
        val deviceId = WearableCloudIdentity.deviceId(wearableBindingProvider()) ?: return false
        val batchId = UUID.nameUUIDFromBytes(
            "diet|$deviceId|${record.id}".toByteArray(StandardCharsets.UTF_8),
        ).toString()
        if (syncRepository.queuedItem(batchId) == null) {
            val request = record.toTelemetryBatch(deviceId, batchId)
            val now = nowProvider()
            syncRepository.enqueue(
                UploadQueueEntity(
                    id = batchId,
                    kind = "telemetry_batch",
                    payloadJson = gson.toJson(request),
                    status = "pending",
                    createdAt = now,
                    nextRetryAt = now,
                ),
            )
        }
        dao.attachUploadBatch(record.id, record.userId, batchId)
        return true
    }

    private fun validate(draft: DietRecordDraft) {
        require(DietMealType.isSupported(draft.mealType)) { "请选择有效的餐次。" }
        require(draft.description.trim().isNotEmpty()) { "请输入餐食内容。" }
        require(draft.description.trim().length <= MAX_DESCRIPTION_LENGTH) { "餐食内容不能超过 256 个字符。" }
        require(draft.caloriesKcal.isFinite() && draft.caloriesKcal > 0.0) { "请输入大于 0 的餐食热量。" }
        listOf(
            draft.proteinGrams,
            draft.carbohydrateGrams,
            draft.fatGrams,
            draft.fiberGrams,
            draft.sodiumMilligrams,
        ).filterNotNull().forEach { value ->
            require(value.isFinite() && value >= 0.0) { "营养素数值不能为负数。" }
        }
    }

    private fun DietRecordEntity.toTelemetryBatch(deviceId: String, batchId: String) =
        TelemetryBatchRequestDto(
            schemaVersion = "telemetry-v2",
            batchId = batchId,
            deviceId = deviceId,
            collectedFrom = consumedAt,
            collectedTo = consumedAt,
            source = SOURCE,
            measurements = emptyList(),
            sleepSessions = emptyList(),
            activitySessions = emptyList(),
            dietRecords = listOf(
                mapOfNotNull(
                    "id" to id,
                    "consumedAt" to consumedAt,
                    "mealType" to mealType,
                    "description" to description,
                    "caloriesKcal" to caloriesKcal,
                    "proteinGrams" to proteinGrams,
                    "carbohydrateGrams" to carbohydrateGrams,
                    "fatGrams" to fatGrams,
                    "fiberGrams" to fiberGrams,
                    "sodiumMilligrams" to sodiumMilligrams,
                    "source" to source,
                ),
            ),
            signalChunks = emptyList(),
            quality = mapOf("provenance" to source, "rawSignalExcluded" to true),
        )

    private fun mapOfNotNull(vararg pairs: Pair<String, Any?>): Map<String, Any> = buildMap {
        pairs.forEach { (key, value) -> if (value != null) put(key, value) }
    }

    private companion object {
        const val SOURCE = "manual_diet_room"
        const val PHOTO_SOURCE = "camera_food_analysis"
        const val MAX_DESCRIPTION_LENGTH = 256
    }
}

internal fun BehaviorRecordDto.toDietRecordDraftOrNull(
    zoneId: ZoneId = ZoneId.systemDefault(),
): DietRecordDraft? {
    if (!category.equals("FOOD", ignoreCase = true)) return null
    val calories = caloriesKcal?.takeIf { it.isFinite() && it > 0.0 } ?: return null
    val timestamp = occurredAt?.takeIf { it > 0L } ?: System.currentTimeMillis()
    val description = when {
        items.isNotEmpty() -> items.filter(String::isNotBlank).joinToString(" + ")
        !title.isNullOrBlank() -> title
        !summary.isNullOrBlank() -> summary
        else -> null
    }?.trim()?.take(256) ?: return null
    if (description.isBlank()) return null
    val hour = Instant.ofEpochMilli(timestamp).atZone(zoneId).hour
    val mealType = when (hour) {
        in 5..10 -> DietMealType.BREAKFAST.wireValue
        in 11..14 -> DietMealType.LUNCH.wireValue
        in 17..21 -> DietMealType.DINNER.wireValue
        else -> DietMealType.SNACK.wireValue
    }
    return DietRecordDraft(
        mealType = mealType,
        description = description,
        caloriesKcal = calories,
        proteinGrams = proteinGrams?.takeIf { it.isFinite() && it >= 0.0 },
        carbohydrateGrams = carbohydrateGrams?.takeIf { it.isFinite() && it >= 0.0 },
        fatGrams = fatGrams?.takeIf { it.isFinite() && it >= 0.0 },
        consumedAt = timestamp,
    )
}

data class DietSaveResult(
    val record: DietRecordEntity,
    val queued: Boolean,
    val inserted: Boolean = true,
)
