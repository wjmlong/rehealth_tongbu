package com.rehealth.genie.features

import com.rehealth.genie.network.dto.CvdFeatureVectorDto
import com.rehealth.genie.network.dto.FeatureEvaluateRequest
import com.rehealth.genie.network.dto.FeatureQualityDto
import java.util.UUID

/**
 * Pure mapping from the local C1 [CvdFeatureVector] (camelCase) into the E1/mobile
 * request DTO contract (snake_case field keys + nested featureQuality map), aligned
 * with `Android-apk/docs/FEATURE_EXTRACTOR.md` and `model-service/docs/API_CONTRACT.md`.
 *
 * This mapper does not depend on Android or networking; it is safe to unit test on the JVM.
 */
object CvdFeatureVectorDtoMapper {

    fun toFeatureEvaluateRequest(
        vector: CvdFeatureVector,
        requestId: String? = null,
    ): FeatureEvaluateRequest {
        validateQualityCoverage(vector)
        return FeatureEvaluateRequest(
            featureVector = vector.toDto(),
            requestId = requestId ?: generateRequestId(),
        )
    }

    fun CvdFeatureVector.toDto(): CvdFeatureVectorDto =
        CvdFeatureVectorDto(
            age = age,
            gender = gender,
            bmi = bmi,
            sbp = sbp,
            dbp = dbp,
            fasting_glucose = fastingGlucose,
            total_cholesterol = totalCholesterol,
            ldl = ldl,
            hdl = hdl,
            triglycerides = triglycerides,
            exercise_days = exerciseDays,
            smoking = smoking,
            drinking = drinking,
            diabetes_history = diabetesHistory,
            hypertension_history = hypertensionHistory,
            family_history = familyHistory,
            featureQuality = featureQuality.toDtoMap(),
        )

    fun FeatureQuality.toDto(): FeatureQualityDto =
        FeatureQualityDto(
            status = status.name,
            source = source.name,
            observedAt = observedAt,
            reason = reason,
        )

    fun Map<String, FeatureQuality>.toDtoMap(): Map<String, FeatureQualityDto> =
        this.mapValues { (_, quality) -> quality.toDto() }

    private fun generateRequestId(): String =
        runCatching { UUID.randomUUID().toString() }.getOrElse { "rehealth-${System.currentTimeMillis()}" }

    /**
     * Defensive check: every CVD 16 field must have a featureQuality entry. The model
     * contract requires this; if the extractor is ever misconfigured, surface it here
     * rather than letting the backend/model-service reject via HTTP 422.
     */
    private fun validateQualityCoverage(vector: CvdFeatureVector) {
        val missing = CvdFeatureFields.ALL.filter { it !in vector.featureQuality }
        if (missing.isNotEmpty()) {
            throw IllegalStateException(
                "CvdFeatureVector is missing featureQuality entries for: ${missing.joinToString()}.",
            )
        }
    }
}
