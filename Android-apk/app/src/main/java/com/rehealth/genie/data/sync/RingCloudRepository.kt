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
import com.rehealth.genie.network.dto.RiskResultDto
import com.rehealth.genie.network.dto.TelemetryBatchRequestDto
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
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
    ): Result<String> = runCatching {
        check(sessionStore.isLoggedIn) { "登录已失效，请重新登录后同步。" }
        val vendor = wearableBindingProvider()?.vendor ?: WearableVendor.MRD
        val measurements = dao.observeLatestMeasurements().first()
            .filter { entity -> entity.source.matchesVendor(vendor) }
        val sleep = dao.observeLatestSleepSession().first()
            ?.takeIf { entity -> entity.source.matchesVendor(vendor) }
        val activity = dao.observeLatestActivity().first()
            ?.takeIf { entity -> entity.source.matchesVendor(vendor) }
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
        triggerSync()
        request.batchId
    }

    suspend fun fetchPatientMvp(): Result<PatientMvpPayload> = runCatching {
        val profile = apiClient.getProfile().successOrThrow("健康档案读取失败。")
        val latestHealthInterview = apiClient.getLatestHealthInterview().successOrNull()
        val risk = apiClient.getRiskLatest().successOrNull()
        var intervention = apiClient.getInterventionsToday().successOrNull()
        if (intervention == null && risk?.normalizedRiskScore != null) {
            intervention = apiClient.generateIntervention(
                InterventionGenerateRequestDto(
                    requestId = UUID.randomUUID().toString(),
                ),
            ).successOrNull()
        }
        PatientMvpPayload(
            profile = profile?.toPayload(),
            risk = risk?.toPayload(),
            interventionPlan = intervention?.toPatientInterventionPayloads().orEmpty(),
            recentCheckins = emptyList(),
            updatedAt = nowProvider(),
            latestHealthInterview = latestHealthInterview,
        )
    }

    companion object {
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
    }
}

private fun String.matchesVendor(vendor: WearableVendor): Boolean = when (vendor) {
    WearableVendor.RWFIT -> startsWith("rwfit", ignoreCase = true)
    WearableVendor.MRD -> startsWith("mrd", ignoreCase = true)
    WearableVendor.MOCK ->
        equals("ring_sim", ignoreCase = true) ||
            contains("mock", ignoreCase = true) ||
            contains("synthetic", ignoreCase = true)
    WearableVendor.HBAND -> startsWith("hband", ignoreCase = true)
}

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
    val planId = plan_id?.takeIf(String::isNotBlank) ?: return emptyList()
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
            title = priority_intervention,
            goal = expected_impact,
            action = rationale,
            duration = null,
            reason = medical_disclaimer,
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
