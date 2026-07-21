package com.rehealth.genie.network.dto

import com.squareup.moshi.JsonClass

/**
 * Android CVD feature-evaluation DTOs.
 *
 * These mirror the contract in:
 *  - Android-apk/docs/FEATURE_EXTRACTOR.md
 *  - Android-apk/app/src/main/java/com/rehealth/genie/features/CvdFeatureVector.kt
 *  - backend/docs/MOBILE_API.md
 *  - model-service/docs/API_CONTRACT.md
 *
 * Field keys use snake_case to match the canonical model contract. Moshi serializes
 * Kotlin property names verbatim, so the data classes below intentionally use snake_case
 * property names. The C1 CvdFeatureVector uses camelCase locally; see
 * [com.rehealth.genie.features.CvdFeatureVectorDtoMapper] for the pure mapping.
 */

@JsonClass(generateAdapter = true)
data class FeatureQualityDto(
    val status: String,
    val source: String,
    val observedAt: Long? = null,
    val reason: String,
)

@JsonClass(generateAdapter = true)
data class CvdFeatureVectorDto(
    val age: Int? = null,
    val gender: Int? = null,
    val bmi: Double? = null,
    val sbp: Double? = null,
    val dbp: Double? = null,
    val fasting_glucose: Double? = null,
    val total_cholesterol: Double? = null,
    val ldl: Double? = null,
    val hdl: Double? = null,
    val triglycerides: Double? = null,
    val exercise_days: Int? = null,
    val smoking: Int? = null,
    val drinking: Int? = null,
    val diabetes_history: Int? = null,
    val hypertension_history: Int? = null,
    val family_history: Int? = null,
    val featureQuality: Map<String, FeatureQualityDto>,
)

@JsonClass(generateAdapter = true)
data class FeatureEvaluateRequest(
    val featureVector: CvdFeatureVectorDto,
    val requestId: String? = null,
)

@JsonClass(generateAdapter = true)
data class RiskResultDto(
    val risk_score: Double? = null,
    val riskScore: Double? = null,
    val risk_level: String? = null,
    val riskLevel: String? = null,
    val feature_contributions: Map<String, Double>? = null,
    val featureContributions: Map<String, Double>? = null,
    val model_version: String? = null,
    val modelVersion: String? = null,
    val is_mock: Boolean? = null,
    val isMock: Boolean? = null,
    val request_id: String? = null,
    val requestId: String? = null,
    val missing_fields: List<String>? = null,
    val missingFields: List<String>? = null,
    val quality_warnings: List<String>? = null,
    val qualityWarnings: List<String>? = null,
    val summary: String? = null,
) {
    val normalizedRiskScore: Double?
        get() = risk_score ?: riskScore

    val normalizedRiskLevel: String?
        get() = risk_level ?: riskLevel

    val normalizedFeatureContributions: Map<String, Double>
        get() = feature_contributions ?: featureContributions ?: emptyMap()

    val normalizedModelVersion: String?
        get() = model_version ?: modelVersion

    val normalizedIsMock: Boolean?
        get() = is_mock ?: isMock

    val normalizedRequestId: String?
        get() = request_id ?: requestId

    val normalizedMissingFields: List<String>
        get() = missing_fields ?: missingFields ?: emptyList()

    val normalizedQualityWarnings: List<String>
        get() = quality_warnings ?: qualityWarnings ?: emptyList()
}

@JsonClass(generateAdapter = true)
data class InterventionPlanDto(
    val plan_id: String? = null,
    val generated_at: String? = null,
    val priority_intervention: String? = null,
    val rationale: String? = null,
    val expected_impact: String? = null,
    val contraindications: List<String>? = null,
    val confidence: Double? = null,
    val model_version: String? = null,
    val is_mock: Boolean? = null,
    val medical_disclaimer: String? = null,
)

@JsonClass(generateAdapter = true)
data class InterventionFeedbackRequest(
    val status: String,
    val note: String? = null,
    val checkedAt: Long? = null,
)

@JsonClass(generateAdapter = true)
data class InterventionFeedbackResponse(
    val id: String? = null,
    val status: String? = null,
    val persistenceStatus: String? = null,
    val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class HealthCheckResponse(
    val status: String? = null,
    val service: String? = null,
    val model_version: String? = null,
)

@JsonClass(generateAdapter = true)
data class MobileConfigResponse(
    val apiVersion: String? = null,
    val endpoints: List<String>? = null,
    val modelContract: Map<String, String>? = null,
    val limitations: List<String>? = null,
)
