package com.rehealth.genie.network.dto

import com.google.gson.annotations.SerializedName
import com.squareup.moshi.JsonClass

data class RiskEvaluateRequestDto(
    @SerializedName("featureVector") val featureVector: CvdFeatureVectorDto? = null,
    @SerializedName("requestId") val requestId: String? = null,
)

data class RiskEvaluateResponseDto(
    @SerializedName("risk_score") val riskScore: Double? = null,
    @SerializedName("risk_level") val riskLevel: String? = null,
    @SerializedName("feature_contributions") val featureContributions: Map<String, Double>? = null,
    @SerializedName("model_version") val modelVersion: String? = null,
    @SerializedName("is_mock") val isMock: Boolean? = null,
    @SerializedName("missing_fields") val missingFields: List<String>? = null,
    @SerializedName("quality_warnings") val qualityWarnings: List<String>? = null,
    @SerializedName("request_id") val requestId: String? = null,
    @SerializedName("contribution_method") val contributionMethod: String? = null,
    @SerializedName("summary") val summary: String? = null,
)

data class InterventionGenerateRequestDto(
    @SerializedName("request_id") val requestId: String? = null,
    @SerializedName("riskResult") val riskResult: RiskEvaluateResponseDto? = null,
    @SerializedName("featureVector") val featureVector: CvdFeatureVectorDto? = null,
    @SerializedName("patientContext") val patientContext: Map<String, Any>? = null,
)

data class InterventionGenerateResponseDto(
    @SerializedName("plan_id") val planId: String? = null,
    @SerializedName("generated_at") val generatedAt: String? = null,
    @SerializedName("priority_intervention") val priorityIntervention: String? = null,
    @SerializedName("rationale") val rationale: String? = null,
    @SerializedName("expected_impact") val expectedImpact: String? = null,
    @SerializedName("contraindications") val contraindications: List<String>? = null,
    @SerializedName("confidence") val confidence: Double? = null,
    @SerializedName("model_version") val modelVersion: String? = null,
    @SerializedName("is_mock") val isMock: Boolean? = null,
    @SerializedName("medical_disclaimer") val medicalDisclaimer: String? = null,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("items") val items: List<InterventionActionResponseDto>? = null,
    @SerializedName("focus_date") val focusDate: String? = null,
    @SerializedName("context_version") val contextVersion: String? = null,
    @SerializedName("context_generated_at") val contextGeneratedAt: Long? = null,
    @SerializedName("latest_data_at") val latestDataAt: Long? = null,
)

data class InterventionActionResponseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("action") val action: String? = null,
    @SerializedName("rationale") val rationale: String? = null,
    @SerializedName("target") val target: String? = null,
    @SerializedName("timing") val timing: String? = null,
    @SerializedName("priority") val priority: Int? = null,
    @SerializedName("evidenceRefs") val evidenceRefs: List<String>? = null,
)

data class FeedbackRequestDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("adherence") val adherence: Double? = null,
    @SerializedName("note") val note: String? = null,
    @SerializedName("checkedAt") val checkedAt: Long? = null,
)

data class DeviceBindRequestDto(
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("deviceName") val deviceName: String? = null,
    @SerializedName("manufacturer") val manufacturer: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("firmwareVersion") val firmwareVersion: String? = null,
    @SerializedName("hardwareAddressHash") val hardwareAddressHash: String? = null,
)

data class DeviceBindResponseDto(
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("persisted") val persisted: Boolean = false,
    @SerializedName("persistenceStage") val persistenceStage: String? = null,
)

@JsonClass(generateAdapter = true)
data class PatientProfileDto(
    val patientId: String? = null,
    val name: String? = null,
    val gender: String? = null,
    val age: Int? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bmi: Double? = null,
    val diagnoses: List<String>? = null,
    val medications: List<String>? = null,
    val allergies: List<String>? = null,
    val familyHistory: Boolean? = null,
    val smoking: Boolean? = null,
    val drinking: Boolean? = null,
    val diabetesHistory: Boolean? = null,
    val hypertensionHistory: Boolean? = null,
    val updatedAt: Long? = null,
    val version: Long? = null,
)

@JsonClass(generateAdapter = true)
data class TelemetryBatchRequestDto(
    @SerializedName("schemaVersion") val schemaVersion: String? = null,
    @SerializedName("batchId") val batchId: String? = null,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("collectedFrom") val collectedFrom: Long? = null,
    @SerializedName("collectedTo") val collectedTo: Long? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("measurements") val measurements: List<Map<String, Any>>? = null,
    @SerializedName("sleepSessions") val sleepSessions: List<Map<String, Any>>? = null,
    @SerializedName("activitySessions") val activitySessions: List<Map<String, Any>>? = null,
    @SerializedName("dietRecords") val dietRecords: List<Map<String, Any>>? = null,
    @SerializedName("signalChunks") val signalChunks: List<Map<String, Any>>? = null,
    @SerializedName("quality") val quality: Map<String, Any>? = null,
)

@JsonClass(generateAdapter = true)
data class TelemetryBatchResponseDto(
    @SerializedName("batchId") val batchId: String? = null,
    @SerializedName("receiptId") val receiptId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("accepted") val accepted: Boolean = false,
    @SerializedName("persisted") val persisted: Boolean = false,
    @SerializedName("recordCount") val recordCount: Int = 0,
    @SerializedName("dietRecordCount") val dietRecordCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class RecentTelemetryResponseDto(
    val userId: String? = null,
    val limit: Int = 0,
    val measurements: List<RecentMeasurementDto> = emptyList(),
    val sleepSessions: List<RecentSleepSessionDto> = emptyList(),
    val activities: List<RecentActivityDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RecentMeasurementDto(
    val id: String? = null,
    val deviceId: String? = null,
    val metricType: String? = null,
    val measuredAt: Long? = null,
    val primaryValue: Double? = null,
    val secondaryValue: Double? = null,
    val unit: String? = null,
    val qualityCode: String? = null,
    val source: String? = null,
)

@JsonClass(generateAdapter = true)
data class RecentSleepSessionDto(
    val id: String? = null,
    val deviceId: String? = null,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val deepMinutes: Int? = null,
    val lightMinutes: Int? = null,
    val awakeMinutes: Int? = null,
    val remMinutes: Int? = null,
    val interruptionMinutes: Int? = null,
    val source: String? = null,
)

@JsonClass(generateAdapter = true)
data class RecentActivityDto(
    val id: String? = null,
    val deviceId: String? = null,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val activityType: String? = null,
    val steps: Int? = null,
    val distanceMeters: Double? = null,
    val caloriesKcal: Double? = null,
    val durationMinutes: Int? = null,
    val averageHeartRate: Double? = null,
    val source: String? = null,
)

@JsonClass(generateAdapter = true)
data class ViomiBindRequestDto(val imei: String, val productCode: String)

@JsonClass(generateAdapter = true)
data class ViomiBindResponseDto(
    val deviceId: String,
    val status: String? = null,
    val persisted: Boolean = false,
    val persistenceStage: String? = null,
)

@JsonClass(generateAdapter = true)
data class ViomiSyncRequestDto(
    val imei: String,
    val beginAt: Long,
    val endAt: Long,
    val metrics: Set<String>,
)

@JsonClass(generateAdapter = true)
data class ViomiMeasurementDto(
    val id: String,
    val metricType: String,
    val measuredAt: Long,
    val primaryValue: Double,
    val secondaryValue: Double? = null,
    val unit: String,
    val source: String = "viomi_cloud",
)

@JsonClass(generateAdapter = true)
data class ViomiSyncResponseDto(
    val deviceId: String,
    val status: String? = null,
    val persisted: Boolean = false,
    val recordCount: Int = 0,
    val measurements: List<ViomiMeasurementDto> = emptyList(),
)

data class AttributionEventItemDto(
    @SerializedName("date") val date: String? = null,
    @SerializedName("risk_score") val riskScore: Double? = null,
    @SerializedName("intervention_id") val interventionId: String? = null,
    @SerializedName("adherence") val adherence: Double? = null,
)

data class AttributionEventsRequestDto(
    @SerializedName("events") val events: List<AttributionEventItemDto>? = null,
    @SerializedName("baselineRiskScore") val baselineRiskScore: Double? = null,
)

@JsonClass(generateAdapter = true)
data class BehaviorRecordDto(
    val id: String? = null,
    val requestId: String? = null,
    val category: String? = null,
    val title: String? = null,
    val summary: String? = null,
    val items: List<String> = emptyList(),
    val caloriesKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbohydrateGrams: Double? = null,
    val fatGrams: Double? = null,
    val ocrText: String? = null,
    val confidence: Double? = null,
    val modelVersion: String? = null,
    val occurredAt: Long? = null,
    val createdAt: Long? = null,
)
