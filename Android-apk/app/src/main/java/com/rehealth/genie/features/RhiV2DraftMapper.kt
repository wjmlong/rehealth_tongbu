package com.rehealth.genie.features

import com.rehealth.genie.network.dto.FeatureQualityDto
import com.rehealth.genie.network.dto.RhiV2EvaluateRequestDto
import com.rehealth.genie.network.dto.RhiV2FeatureFields
import com.rehealth.genie.network.dto.RhiV2FeatureVectorDto
import java.util.UUID

/**
 * Additive CVD-16 -> RHI v2 draft migration helper.
 *
 * Only fields with compatible meaning are carried forward. A one-off SBP/DBP
 * measurement is not relabeled as a seven-day home average; exercise_days is not
 * relabeled as steps or MVPA; hypertension history is not relabeled as treatment.
 */
object RhiV2DraftMapper {
    fun fromCvdV1(
        vector: CvdFeatureVector,
        productTier: String = "lite",
        requestId: String = UUID.randomUUID().toString(),
    ): RhiV2EvaluateRequestDto {
        val mappedQuality = RhiV2FeatureFields.ALL.associateWith { field ->
            when (field) {
                "age" -> vector.qualityDto(CvdFeatureFields.AGE)
                "biological_sex" -> vector.qualityDto(CvdFeatureFields.GENDER)
                "bmi" -> vector.qualityDto(CvdFeatureFields.BMI)
                "total_cholesterol" -> vector.qualityDto(CvdFeatureFields.TOTAL_CHOLESTEROL)
                "hdl_c" -> vector.qualityDto(CvdFeatureFields.HDL)
                "ldl_c" -> vector.qualityDto(CvdFeatureFields.LDL)
                "triglycerides" -> vector.qualityDto(CvdFeatureFields.TRIGLYCERIDES)
                "glycemia_value" -> vector.qualityDto(CvdFeatureFields.FASTING_GLUCOSE)
                "nicotine_exposure" -> vector.qualityDto(CvdFeatureFields.SMOKING)
                "diabetes_status" -> vector.qualityDto(CvdFeatureFields.DIABETES_HISTORY)
                else -> missingQuality(field)
            }
        }
        return RhiV2EvaluateRequestDto(
            featureVector = RhiV2FeatureVectorDto(
                age = vector.age,
                biologicalSex = when (vector.gender) {
                    1 -> "male"
                    0 -> "female"
                    else -> null
                },
                bmi = vector.bmi,
                totalCholesterol = vector.totalCholesterol,
                hdlC = vector.hdl,
                ldlC = vector.ldl,
                triglycerides = vector.triglycerides,
                glycemiaValue = vector.fastingGlucose,
                nicotineExposure = vector.smoking,
                diabetesStatus = vector.diabetesHistory,
            ),
            featureQuality = mappedQuality,
            productTier = productTier,
            glycemiaMetric = vector.fastingGlucose?.let { "fasting_glucose_mmol_l" },
            requestId = requestId,
        )
    }

    private fun CvdFeatureVector.qualityDto(field: String): FeatureQualityDto {
        val quality = featureQuality[field] ?: return missingQuality(field)
        return FeatureQualityDto(
            status = quality.status.name,
            source = quality.source.name,
            observedAt = quality.observedAt,
            reason = quality.reason,
        )
    }

    private fun missingQuality(field: String): FeatureQualityDto =
        FeatureQualityDto(
            status = "MISSING",
            source = "UNKNOWN",
            reason = "RHI v2 field '$field' is not available from the CVD-16 contract.",
        )
}
