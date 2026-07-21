package com.rehealth.genie.network.dto

import com.google.gson.annotations.SerializedName

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
    @SerializedName("id") val id: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("deviceId") val deviceId: String? = null,
)

data class TelemetryBatchRequestDto(
    @SerializedName("batchId") val batchId: String? = null,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("collectedFrom") val collectedFrom: Long? = null,
    @SerializedName("collectedTo") val collectedTo: Long? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("measurements") val measurements: List<Map<String, Any>>? = null,
    @SerializedName("sleepSessions") val sleepSessions: List<Map<String, Any>>? = null,
    @SerializedName("activitySessions") val activitySessions: List<Map<String, Any>>? = null,
    @SerializedName("signalChunks") val signalChunks: List<Map<String, Any>>? = null,
    @SerializedName("quality") val quality: Map<String, Any>? = null,
)

data class TelemetryBatchResponseDto(
    @SerializedName("batchId") val batchId: String? = null,
    @SerializedName("receiptId") val receiptId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("accepted") val accepted: Boolean = false,
    @SerializedName("persisted") val persisted: Boolean = false,
    @SerializedName("recordCount") val recordCount: Int = 0,
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
