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
import com.rehealth.genie.network.dto.RiskEvaluateResponseDto
import com.rehealth.genie.network.dto.RiskResultDto
import com.rehealth.genie.network.dto.TelemetryBatchRequestDto
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
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
) {
    suspend fun bindDevice(device: RingDevice): Result<DeviceBindResponseDto> = runCatching {
        val addressHash = sha256(device.address)
        val request = DeviceBindRequestDto(
            deviceId = deviceId(addressHash),
            deviceName = device.name,
            manufacturer = "MRD",
            model = "MR11",
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
        val measurements = dao.observeLatestMeasurements().first()
        val sleep = dao.observeLatestSleepSession().first()
        val activity = dao.observeLatestActivity().first()
        val request = telemetryBatchPayload(device, collectedAt, trigger, measurements, sleep, activity)
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
        val risk = apiClient.getRiskLatest().successOrThrow("风险结果读取失败。")
        var intervention = apiClient.getInterventionsToday().successOrThrow("今日干预读取失败。")
        if (intervention == null && risk?.normalizedRiskScore != null) {
            intervention = apiClient.generateIntervention(
                InterventionGenerateRequestDto(
                    riskResult = risk.toGenerateRiskDto(),
                    patientContext = profile?.toPatientContext().orEmpty(),
                ),
            ).successOrThrow("今日干预生成失败。")
        }
        PatientMvpPayload(
            profile = profile?.toPayload(),
            risk = risk?.toPayload(),
            interventionPlan = intervention?.let { listOf(it.toPayload()) }.orEmpty(),
            recentCheckins = emptyList(),
            updatedAt = nowProvider(),
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
    ): TelemetryBatchRequestDto {
        require(measurements.isNotEmpty() || sleep != null || activity != null) {
            "没有可上传的本地健康记录。"
        }
        val addressHash = sha256(device.address)
        val effectiveDeviceId = deviceId(addressHash)
        val timestamps = buildList {
            addAll(measurements.map { it.measuredAt })
            sleep?.let { add(it.startedAt); add(it.endedAt) }
            activity?.let { add(it.startedAt); it.endedAt?.let(::add) }
        }
        val provenance = if (
            measurements.any { it.source.contains("mock", true) || it.source.contains("synthetic", true) } ||
            sleep?.source?.let { it.contains("mock", true) || it.contains("synthetic", true) } == true ||
            activity?.source?.let { it.contains("mock", true) || it.contains("synthetic", true) } == true
        ) "synthetic_qa" else "mrd_room"
        val batchId = UUID.nameUUIDFromBytes(
            "$effectiveDeviceId|$collectedAt|$trigger".toByteArray(StandardCharsets.UTF_8),
        ).toString()
        return TelemetryBatchRequestDto(
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
            signalChunks = emptyList(),
            quality = mapOf(
                "provenance" to provenance,
                "rawSignalExcluded" to true,
            ),
        )
    }

        private fun deviceId(addressHash: String): String = "mrd-${addressHash.take(24)}"

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(StandardCharsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}

private fun <T> ApiResult<T>.successOrThrow(fallback: String): T = when (this) {
    is ApiResult.Success -> data
    else -> error(safeMessage(this, fallback))
}

private fun safeMessage(result: ApiResult<*>, fallback: String): String = when (result) {
    is ApiResult.Unauthorized -> "登录已失效，请重新登录。"
    is ApiResult.Forbidden -> "当前账号无权执行此操作。"
    is ApiResult.InvalidRequest -> result.message
    is ApiResult.InvalidResponse -> fallback
    is ApiResult.ServiceUnavailable -> fallback
    is ApiResult.NetworkError -> "网络连接失败，请稍后重试。"
    is ApiResult.Success -> fallback
}

private fun RiskResultDto.toGenerateRiskDto(): RiskEvaluateResponseDto =
    RiskEvaluateResponseDto(
        riskScore = normalizedRiskScore,
        riskLevel = normalizedRiskLevel,
        featureContributions = normalizedFeatureContributions,
        modelVersion = normalizedModelVersion,
        isMock = normalizedIsMock,
        missingFields = normalizedMissingFields,
        qualityWarnings = normalizedQualityWarnings,
        requestId = normalizedRequestId,
        summary = summary,
    )

private fun PatientProfileDto.toPatientContext(): Map<String, Any> = mapOfNotNull(
    "age" to age,
    "gender" to gender,
    "bmi" to bmi,
    "smoking" to smoking,
    "drinking" to drinking,
    "diabetes_history" to diabetesHistory,
    "hypertension_history" to hypertensionHistory,
)

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

private fun InterventionPlanDto.toPayload(): PatientInterventionPayload = PatientInterventionPayload(
    id = plan_id,
    title = priority_intervention,
    goal = expected_impact,
    action = rationale,
    duration = null,
    reason = medical_disclaimer,
    status = "active",
)

private fun mapOfNotNull(vararg pairs: Pair<String, Any?>): Map<String, Any> =
    buildMap {
        pairs.forEach { (key, value) ->
            if (value != null) put(key, value)
        }
    }
