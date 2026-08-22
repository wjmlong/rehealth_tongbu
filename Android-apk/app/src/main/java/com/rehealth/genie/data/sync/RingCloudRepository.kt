package com.rehealth.genie.data.sync

import com.google.gson.Gson
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.PatientInterventionPayload
import com.rehealth.genie.network.PatientMvpPayload
import com.rehealth.genie.network.PatientProfilePayload
import com.rehealth.genie.network.PatientRiskPayload
import com.rehealth.genie.network.SessionStore
import com.rehealth.genie.network.dto.DeviceBindRequestDto
import com.rehealth.genie.network.dto.DeviceBindResponseDto
import com.rehealth.genie.network.dto.InterventionGenerateRequestDto
import com.rehealth.genie.network.dto.InterventionPlanDto
import com.rehealth.genie.network.dto.PatientProfileDto
import com.rehealth.genie.network.dto.RecentTelemetryResponseDto
import com.rehealth.genie.network.dto.RiskResultDto
import com.rehealth.genie.network.dto.TelemetryBatchRequestDto
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataBatch
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import com.rehealth.genie.ring.preferredSleepSession
import com.rehealth.genie.ring.provider.ActiveWearableBinding
import com.rehealth.genie.ring.provider.WearableVendor
import com.rehealth.genie.ring.provider.WearableCloudIdentity
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.flow.first

class RingCloudRepository(
    private val dao: RingDataDao,
    private val syncRepository: SyncRepository,
    private val apiClient: AuthenticatedApiClient,
    private val sessionStore: SessionStore,
    private val triggerSync: () -> Unit,
    private val gson: Gson = Gson(),
    private val nowProvider: () -> Long = System::currentTimeMillis,
    private val wearableBindingProvider: () -> ActiveWearableBinding? = { null },
) {
    private var restoredSessionToken: String? = null

    suspend fun bindDevice(device: RingDevice): Result<DeviceBindResponseDto> = runCatching {
        val addressHash = WearableCloudIdentity.addressHash(device.address)
        val binding = wearableBindingProvider()
        val vendor = binding?.vendor ?: WearableVendor.MRD
        val request = DeviceBindRequestDto(
            deviceId = WearableCloudIdentity.deviceId(device.address, vendor),
            deviceName = device.name,
            manufacturer = vendor.name,
            model = binding?.modelCode ?: binding?.productCode ?: DEFAULT_MRD_MODEL,
            hardwareAddressHash = addressHash,
        )
        when (val result = apiClient.bindDevice(request)) {
            is ApiResult.Success -> result.data.takeIf { it.persisted }
                ?: error("设备绑定未持久化，请稍后重试。")
            else -> error(safeMessage(result, "设备绑定失败，请稍后重试。"))
        }
    }

    suspend fun enqueueLatestTelemetry(
        device: RingDevice,
        collectedAt: Long,
        trigger: String,
        triggerUpload: Boolean = true,
    ): Result<String> = runCatching {
        check(sessionStore.isLoggedIn) { "登录已失效，请重新登录后同步。" }
        val ownerUserId = sessionStore.userId?.takeIf(String::isNotBlank)
            ?: error("Authenticated user identity is unavailable.")
        val vendor = wearableBindingProvider()?.vendor ?: WearableVendor.MRD
        val measurements = dao.observeLatestMeasurementsForOwner(ownerUserId).first()
            .filter { entity -> entity.isLocallyCollectedFor(vendor) }
        val sleep = preferredSleepSession(
            dao.getSleepSessionsSinceForOwner(0L, ownerUserId)
                .filter { entity -> entity.isLocallyCollectedFor(vendor) },
        )
        val activity = dao.observeActivitiesForOwner(ownerUserId).first()
            .filter { entity -> entity.isLocallyCollectedFor(vendor) }
            .maxWithOrNull(
                compareBy<RingActivityEntity> { it.startedAt }
                    .thenBy { it.steps }
                    .thenBy { it.endedAt ?: it.startedAt },
            )
        val request = telemetryBatchPayload(device, collectedAt, trigger, measurements, sleep, activity, vendor)
        val now = nowProvider()
        syncRepository.enqueue(
            UploadQueueEntity(
                id = request.batchId ?: error("无法生成遥测批次编号。"),
                kind = "telemetry_batch",
                payloadJson = gson.toJson(request),
                status = "pending",
                createdAt = now,
                nextRetryAt = now,
            ),
        )
        if (triggerUpload) triggerSync()
        request.batchId
    }

    suspend fun fetchPatientMvp(): Result<PatientMvpPayload> = runCatching {
        val profile = apiClient.getProfile().successOrThrow("健康档案读取失败。")
        val latestHealthInterview = apiClient.getLatestHealthInterview().successOrNull()
        val risk = apiClient.getRiskLatest().successOrNull()
        val intervention = apiClient.getInterventionsToday().successOrNull()
        PatientMvpPayload(
            profile = profile?.toPayload(),
            risk = risk?.toPayload(),
            interventionPlan = intervention?.toPatientInterventionPayloads().orEmpty(),
            recentCheckins = emptyList(),
            updatedAt = nowProvider(),
            latestHealthInterview = latestHealthInterview,
        )
    }

    /**
     * Restores the authenticated user's newest cloud telemetry into Room once per login token.
     * A failed attempt is deliberately not remembered so a later foreground refresh can retry.
     */
    suspend fun restoreRecentTelemetryOncePerSession(): Result<Int> = runCatching {
        val token = sessionStore.token?.takeIf(String::isNotBlank)
            ?: error("Login is required before restoring cloud telemetry.")
        if (restoredSessionToken == token) return@runCatching 0
        val expectedUserId = sessionStore.userId?.takeIf(String::isNotBlank)
            ?: error("Authenticated user identity is unavailable.")
        val response = when (val result = apiClient.getRecentTelemetry(RECENT_TELEMETRY_LIMIT)) {
            is ApiResult.Success -> result.data
            else -> error(safeMessage(result, "Cloud telemetry restore failed."))
        }
        val responseUserId = response.userId?.takeIf(String::isNotBlank)
        check(responseUserId == null || responseUserId == expectedUserId) {
            "Cloud telemetry ownership did not match the authenticated user."
        }
        val batch = telemetryRestoreBatch(response, expectedUserId)
        dao.insertBatch(batch)
        restoredSessionToken = token
        batch.size
    }

    suspend fun generateIntervention(): Result<List<PatientInterventionPayload>> = runCatching {
        check(sessionStore.isLoggedIn) { "登录已失效，请重新登录后生成计划。" }
        val response = apiClient.generateIntervention(
            InterventionGenerateRequestDto(requestId = UUID.randomUUID().toString()),
        ).successOrThrow("个性化干预计划生成失败，请稍后重试。")
        response.toPatientInterventionPayloads().also { plans ->
            check(plans.isNotEmpty()) { "服务端未返回可展示的个性化干预计划。" }
        }
    }

    companion object {
        internal fun telemetryRestoreBatch(
            response: RecentTelemetryResponseDto,
            ownerUserId: String,
        ): RingDataBatch {
            val measurements = response.measurements.mapNotNull { record ->
                val metricType = record.metricType?.trim()?.takeIf(String::isNotEmpty)
                    ?: return@mapNotNull null
                val measuredAt = record.measuredAt?.takeIf { it > 0L } ?: return@mapNotNull null
                val primaryValue = record.primaryValue?.takeIf(Double::isFinite)
                    ?: return@mapNotNull null
                val deviceId = record.deviceId?.trim()?.takeIf(String::isNotEmpty)
                val source = record.source.normalizedCloudSource()
                RingMeasurementEntity(
                    id = stableCloudRecordId(
                        "measurement",
                        record.id,
                        "$ownerUserId|$deviceId|$metricType|$measuredAt|$source",
                    ),
                    metricType = metricType,
                    measuredAt = measuredAt,
                    primaryValue = primaryValue,
                    secondaryValue = record.secondaryValue?.takeIf(Double::isFinite),
                    unit = record.unit?.trim().orEmpty(),
                    quality = record.qualityCode.toRoomQuality(),
                    source = source,
                    ownerUserId = ownerUserId,
                    deviceId = deviceId,
                )
            }.distinctBy { it.id }
            val sleepSessions = response.sleepSessions.mapNotNull { record ->
                val startedAt = record.startedAt?.takeIf { it > 0L } ?: return@mapNotNull null
                val endedAt = record.endedAt?.takeIf { it > startedAt } ?: return@mapNotNull null
                val source = record.source.normalizedCloudSource()
                RingSleepSessionEntity(
                    id = stableCloudRecordId(
                        "sleep",
                        record.id,
                        "$ownerUserId|${record.deviceId}|$startedAt|$endedAt|$source",
                    ),
                    startedAt = startedAt,
                    endedAt = endedAt,
                    deepMinutes = record.deepMinutes.nonNegative(),
                    lightMinutes = record.lightMinutes.nonNegative(),
                    awakeMinutes = record.awakeMinutes.nonNegative(),
                    remMinutes = record.remMinutes.nonNegative(),
                    interruptionMinutes = record.interruptionMinutes.nonNegative(),
                    source = source,
                    ownerUserId = ownerUserId,
                    deviceId = record.deviceId?.trim()?.takeIf(String::isNotEmpty),
                )
            }.groupBy { it.id }.values.map { duplicateRecords ->
                duplicateRecords.maxWith(
                    compareBy<RingSleepSessionEntity> { it.endedAt }
                        .thenBy { it.deepMinutes + it.lightMinutes + it.remMinutes }
                        .thenBy { it.startedAt },
                )
            }
            val activities = response.activities.mapNotNull { record ->
                val startedAt = record.startedAt?.takeIf { it > 0L } ?: return@mapNotNull null
                val endedAt = record.endedAt?.takeIf { it >= startedAt }
                val activityType = record.activityType?.trim()?.takeIf(String::isNotEmpty)
                    ?: return@mapNotNull null
                val source = record.source.normalizedCloudSource()
                RingActivityEntity(
                    id = stableCloudRecordId(
                        "activity",
                        record.id,
                        "$ownerUserId|${record.deviceId}|$startedAt|$activityType|$source",
                    ),
                    startedAt = startedAt,
                    endedAt = endedAt,
                    activityType = activityType,
                    steps = record.steps.nonNegative(),
                    distanceMeters = record.distanceMeters.nonNegativeFinite(),
                    caloriesKcal = record.caloriesKcal.nonNegativeFinite(),
                    durationMinutes = record.durationMinutes.nonNegative(),
                    averageHeartRate = record.averageHeartRate?.takeIf { it.isFinite() && it > 0.0 },
                    source = source,
                    ownerUserId = ownerUserId,
                    deviceId = record.deviceId?.trim()?.takeIf(String::isNotEmpty),
                )
            }.groupBy { it.id }.values.map { duplicateRecords ->
                duplicateRecords.maxWith(
                    compareBy<RingActivityEntity> { it.steps }
                        .thenBy { it.endedAt ?: it.startedAt }
                        .thenBy { it.durationMinutes },
                )
            }
            return RingDataBatch(
                measurements = measurements,
                sleepSessions = sleepSessions,
                activities = activities,
            )
        }

        internal fun telemetryBatchPayload(
        device: RingDevice,
        collectedAt: Long,
        trigger: String,
        measurements: List<RingMeasurementEntity>,
        sleep: RingSleepSessionEntity?,
        activity: RingActivityEntity?,
        vendor: WearableVendor = WearableVendor.MRD,
    ): TelemetryBatchRequestDto {
        require(measurements.isNotEmpty() || sleep != null || activity != null) {
            "没有可上传的本地健康记录。"
        }
        val effectiveDeviceId = WearableCloudIdentity.deviceId(device.address, vendor)
        val timestamps = buildList {
            addAll(measurements.map { it.measuredAt })
            sleep?.let { add(it.startedAt); add(it.endedAt) }
            activity?.let { add(it.startedAt); it.endedAt?.let(::add) }
        }
        val containsNonProductionInput =
            vendor == WearableVendor.MOCK ||
            measurements.any { it.source.equals("ring_sim", true) } ||
            measurements.any { it.source.contains("mock", true) || it.source.contains("synthetic", true) } ||
            sleep?.source?.equals("ring_sim", true) == true ||
            sleep?.source?.let { it.contains("mock", true) || it.contains("synthetic", true) } == true ||
            activity?.source?.equals("ring_sim", true) == true ||
            activity?.source?.let { it.contains("mock", true) || it.contains("synthetic", true) } == true
        val provenance = runtimeTelemetryProvenance(vendor, containsNonProductionInput)
        val batchId = UUID.nameUUIDFromBytes(
            "$effectiveDeviceId|$collectedAt|$trigger".toByteArray(StandardCharsets.UTF_8),
        ).toString()
        return TelemetryBatchRequestDto(
            schemaVersion = "telemetry-v2",
            batchId = batchId,
            deviceId = effectiveDeviceId,
            collectedFrom = timestamps.minOrNull() ?: collectedAt,
            collectedTo = timestamps.maxOrNull() ?: collectedAt,
            source = provenance,
            measurements = measurements.map {
                mapOfNotNull(
                    "id" to it.id,
                    "metricType" to it.metricType,
                    "measuredAt" to it.measuredAt,
                    "primaryValue" to it.primaryValue,
                    "secondaryValue" to it.secondaryValue,
                    "unit" to it.unit,
                    "quality" to it.quality,
                    "source" to it.source,
                )
            },
            sleepSessions = sleep?.let {
                listOf(
                    mapOf(
                        "id" to it.id,
                        "startedAt" to it.startedAt,
                        "endedAt" to it.endedAt,
                        "deepMinutes" to it.deepMinutes,
                        "lightMinutes" to it.lightMinutes,
                        "awakeMinutes" to it.awakeMinutes,
                        "remMinutes" to it.remMinutes,
                        "interruptionMinutes" to it.interruptionMinutes,
                        "source" to it.source,
                    ),
                )
            }.orEmpty(),
            activitySessions = activity?.let {
                listOf(
                    mapOfNotNull(
                        "id" to it.id,
                        "startedAt" to it.startedAt,
                        "endedAt" to it.endedAt,
                        "activityType" to it.activityType,
                        "steps" to it.steps,
                        "distanceMeters" to it.distanceMeters,
                        "caloriesKcal" to it.caloriesKcal,
                        "durationMinutes" to it.durationMinutes,
                        "averageHeartRate" to it.averageHeartRate,
                        "source" to it.source,
                    ),
                )
            }.orEmpty(),
            dietRecords = emptyList(),
            signalChunks = emptyList(),
            quality = mapOf(
                "provenance" to provenance,
                "rawSignalExcluded" to true,
            ),
        )
    }

        private const val DEFAULT_MRD_MODEL = "MR11"
        private const val RECENT_TELEMETRY_LIMIT = 200
    }
}

private fun stableCloudRecordId(kind: String, serverId: String?, identity: String): String =
    "cloud-$kind-${UUID.nameUUIDFromBytes(
        "${serverId?.trim().orEmpty()}|$identity".toByteArray(StandardCharsets.UTF_8),
    )}"

private fun String?.normalizedCloudSource(): String =
    this?.trim()?.takeIf(String::isNotEmpty) ?: "cloud_restore"

private fun String?.toRoomQuality(): Int? = when (this?.trim()?.uppercase()) {
    null, "" -> null
    "VALID", "GOOD", "HIGH" -> 100
    "FAIR", "MEDIUM" -> 70
    "LOW", "POOR" -> 30
    else -> toIntOrNull()?.coerceIn(0, 100)
}

private fun Int?.nonNegative(): Int = this?.coerceAtLeast(0) ?: 0

private fun Double?.nonNegativeFinite(): Double =
    this?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0

private fun String.matchesVendor(vendor: WearableVendor): Boolean = when (vendor) {
    WearableVendor.RWFIT -> startsWith("rwfit", ignoreCase = true)
    WearableVendor.MRD -> startsWith("mrd", ignoreCase = true)
    WearableVendor.MOCK ->
        equals("ring_sim", ignoreCase = true) ||
            contains("mock", ignoreCase = true) ||
            contains("synthetic", ignoreCase = true)
    WearableVendor.HBAND -> startsWith("hband", ignoreCase = true)
    WearableVendor.VIOMI_CLOUD -> startsWith("viomi_cloud", ignoreCase = true)
}

private fun RingMeasurementEntity.isLocallyCollectedFor(vendor: WearableVendor): Boolean =
    !id.startsWith("cloud-") && source.matchesVendor(vendor)

private fun RingSleepSessionEntity.isLocallyCollectedFor(
    vendor: WearableVendor,
): Boolean = !id.startsWith("cloud-") && source.matchesVendor(vendor)

private fun RingActivityEntity.isLocallyCollectedFor(
    vendor: WearableVendor,
): Boolean = !id.startsWith("cloud-") && source.matchesVendor(vendor)

private fun <T> ApiResult<T>.successOrThrow(fallback: String): T = when (this) {
    is ApiResult.Success -> data
    else -> error(safeMessage(this, fallback))
}

private fun <T> ApiResult<T>.successOrNull(): T? = (this as? ApiResult.Success)?.data

private fun safeMessage(result: ApiResult<*>, fallback: String): String = when (result) {
    is ApiResult.Unauthorized -> "登录已失效，请重新登录。"
    is ApiResult.Forbidden -> "当前账号无权执行此操作。"
    is ApiResult.InvalidRequest -> result.message
    is ApiResult.InvalidResponse -> fallback
    is ApiResult.ServiceUnavailable -> fallback
    is ApiResult.NetworkError -> "网络连接失败，请稍后重试。"
    is ApiResult.Success -> fallback
}

private fun PatientProfileDto.toPayload(): PatientProfilePayload = PatientProfilePayload(
    patientId = patientId,
    name = name,
    gender = gender,
    age = age,
    heightCm = heightCm,
    weightKg = weightKg,
    bmi = bmi,
    diagnoses = diagnoses,
    medications = medications,
    allergies = allergies,
    familyHistory = familyHistory,
    smoking = smoking,
    drinking = drinking,
    diabetesHistory = diabetesHistory,
    hypertensionHistory = hypertensionHistory,
    updatedAt = updatedAt,
)

private fun RiskResultDto.toPayload(): PatientRiskPayload = PatientRiskPayload(
    mode = if (normalizedIsMock == true) "mock" else "remote_model",
    modelVersion = normalizedModelVersion,
    riskScore = normalizedRiskScore,
    riskLevel = normalizedRiskLevel,
    summary = summary,
    generatedAt = null,
)

internal fun InterventionPlanDto.toPatientInterventionPayloads(): List<PatientInterventionPayload> {
    val planId = normalizedPlanId?.takeIf(String::isNotBlank) ?: return emptyList()
    val structured = items.orEmpty()
        .sortedBy { it.priority ?: Int.MAX_VALUE }
        .mapNotNull { item ->
            val title = item.title?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val action = item.action?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            PatientInterventionPayload(
                id = planId,
                title = title,
                goal = item.target,
                action = action,
                duration = item.timing,
                reason = item.rationale,
                status = "active",
            )
    }
    if (structured.isNotEmpty()) return structured
    return listOf(
        PatientInterventionPayload(
            id = planId,
            title = normalizedPriorityIntervention,
            goal = normalizedExpectedImpact,
            action = rationale,
            duration = null,
            reason = normalizedMedicalDisclaimer,
            status = "active",
        ),
    )
}

private fun mapOfNotNull(vararg pairs: Pair<String, Any?>): Map<String, Any> =
    buildMap {
        pairs.forEach { (key, value) ->
            if (value != null) put(key, value)
        }
    }
