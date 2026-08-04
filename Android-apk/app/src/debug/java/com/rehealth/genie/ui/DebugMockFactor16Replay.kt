package com.rehealth.genie.ui

import com.rehealth.genie.features.CvdFeatureFields
import com.rehealth.genie.features.CvdFeatureVector
import com.rehealth.genie.features.BaselineHealthProfile
import com.rehealth.genie.features.FeatureQuality
import com.rehealth.genie.features.FeatureQualityStatus
import com.rehealth.genie.features.FeatureSource
import kotlin.math.abs
import kotlin.math.round

internal const val DEBUG_MOCK_FACTOR16_RULE_VERSION = "factor16-rule-v1.0.0-debug-mock"

internal fun isRuntimeFactorContributionConfirmed(ruleVersion: String?): Boolean =
    ruleVersion == DEBUG_MOCK_FACTOR16_RULE_VERSION

internal object RuntimeFactor16Fallback {
    fun completeBaselineProfile(
        profile: BaselineHealthProfile?,
        enabled: Boolean,
        nowMillis: Long,
    ): BaselineHealthProfile? {
        if (!enabled || profile == null) return null
        return profile.copy(
            smoking = profile.smoking ?: false,
            drinking = profile.drinking ?: false,
            diabetesHistory = profile.diabetesHistory ?: false,
            hypertensionHistory = profile.hypertensionHistory ?: false,
            familyHistory = profile.familyHistory ?: false,
            updatedAt = profile.updatedAt ?: nowMillis,
        )
    }

    fun evaluate(
        vector: CvdFeatureVector,
        enabled: Boolean,
        nowMillis: Long,
    ): Factor16ContributionSnapshot? {
        if (!enabled) return null
        return DebugMockFactor16Engine(nowMillis).evaluate(vector)
    }
}

/**
 * Debug-only mirror of the server-owned Factor16 V1.0 display rule.
 *
 * This does not calculate CVD probability, RDI, RHI, SHAP, or PIAS. It exists
 * solely so the explicit mock-device QA flow can render all governed Factor16
 * rows while exercising the real feature extraction and remote request path.
 */
private class DebugMockFactor16Engine(
    private val nowMillis: Long,
) {
    fun evaluate(vector: CvdFeatureVector): Factor16ContributionSnapshot {
        val raw = linkedMapOf<String, Double>()

        put(raw, vector, CvdFeatureFields.AGE, linear(vector.age, 40.0, 5.0, 0.8, -2.4, 8.0))
        put(raw, vector, CvdFeatureFields.GENDER, if (vector.gender == 1) 1.5 else 0.0)
        put(raw, vector, CvdFeatureFields.BMI, bmi(vector.bmi))
        put(
            raw,
            vector,
            CvdFeatureFields.SBP,
            bloodPressure(vector.sbp, 120.0, 5.0, 0.6, -1.2, 6.0),
            requireCuff = true,
        )
        put(
            raw,
            vector,
            CvdFeatureFields.DBP,
            bloodPressure(vector.dbp, 80.0, 5.0, 0.5, -1.0, 4.0),
            requireCuff = true,
        )

        val hasDiabetes = vector.diabetesHistory == 1
        val glucoseTarget = if (hasDiabetes) 7.0 else 6.1
        put(
            raw,
            vector,
            CvdFeatureFields.FASTING_GLUCOSE,
            fastingGlucose(vector.fastingGlucose, glucoseTarget),
            lab = true,
        )
        val totalCholesterolMultiplier =
            if (usable(vector, CvdFeatureFields.LDL, lab = true)) 0.35 else 1.0
        put(
            raw,
            vector,
            CvdFeatureFields.TOTAL_CHOLESTEROL,
            linear(vector.totalCholesterol, 5.2, 0.5, 0.5, -1.0, 2.0) *
                totalCholesterolMultiplier,
            lab = true,
        )
        put(
            raw,
            vector,
            CvdFeatureFields.LDL,
            linear(vector.ldl, 3.4, 0.5, 1.2, -2.4, 4.0),
            lab = true,
        )
        put(
            raw,
            vector,
            CvdFeatureFields.HDL,
            reverseLinear(vector.hdl, 1.0, 0.2, 0.4, -1.2, 1.2),
            lab = true,
        )
        put(
            raw,
            vector,
            CvdFeatureFields.TRIGLYCERIDES,
            linear(vector.triglycerides, 1.7, 0.5, 0.6, -1.2, 2.4),
            lab = true,
        )
        put(
            raw,
            vector,
            CvdFeatureFields.EXERCISE_DAYS,
            linear(5.0 - number(vector.exerciseDays), 0.0, 1.0, 0.6, -1.2, 3.0),
        )
        put(raw, vector, CvdFeatureFields.SMOKING, if (vector.smoking == 1) 5.0 else 0.0)
        put(raw, vector, CvdFeatureFields.DRINKING, if (vector.drinking == 1) 0.8 else 0.0)
        put(raw, vector, CvdFeatureFields.DIABETES_HISTORY, if (hasDiabetes) 3.0 else 0.0)
        put(
            raw,
            vector,
            CvdFeatureFields.HYPERTENSION_HISTORY,
            if (vector.hypertensionHistory == 1) 2.5 else 0.0,
        )
        put(raw, vector, CvdFeatureFields.FAMILY_HISTORY, if (vector.familyHistory == 1) 2.0 else 0.0)

        applyDomainCap(
            raw,
            listOf(CvdFeatureFields.SBP, CvdFeatureFields.DBP, CvdFeatureFields.HYPERTENSION_HISTORY),
            -2.0,
            9.0,
        )
        applyDomainCap(
            raw,
            listOf(CvdFeatureFields.FASTING_GLUCOSE, CvdFeatureFields.DIABETES_HISTORY),
            -1.2,
            6.0,
        )
        applyDomainCap(
            raw,
            listOf(
                CvdFeatureFields.TOTAL_CHOLESTEROL,
                CvdFeatureFields.LDL,
                CvdFeatureFields.HDL,
                CvdFeatureFields.TRIGLYCERIDES,
            ),
            -4.0,
            8.0,
        )

        val measured = raw
            .filterKeys(CLINICAL_80_20_FIELDS::contains)
            .mapValues { (_, points) -> rounded(points * 0.8) }
        val support = measured.mapValues { 0.0 }
        val displayed = raw.mapValues { (field, points) ->
            rounded(measured.getOrElse(field) { points } + support.getOrElse(field) { 0.0 })
        }
        return Factor16ContributionSnapshot(
            contributions = displayed,
            measuredComponents = measured,
            controlSupportComponents = support,
            ruleVersion = DEBUG_MOCK_FACTOR16_RULE_VERSION,
        )
    }

    private fun put(
        output: MutableMap<String, Double>,
        vector: CvdFeatureVector,
        field: String,
        points: Double,
        requireCuff: Boolean = false,
        lab: Boolean = false,
    ) {
        if (!usable(vector, field, requireCuff, lab)) return
        val quality = requireNotNull(vector.featureQuality[field])
        val confidence = confidence(quality, lab)
        if (confidence > 0.0) output[field] = points * confidence
    }

    private fun usable(
        vector: CvdFeatureVector,
        field: String,
        requireCuff: Boolean = false,
        lab: Boolean = false,
    ): Boolean {
        val value = vector.asModelInput()[field]
        val quality = vector.featureQuality[field]
        if (value == null || quality?.status != FeatureQualityStatus.VALID) return false
        if (
            requireCuff &&
            (
                quality.source != FeatureSource.CLINICAL_REPORT ||
                    !quality.reason.contains("upper-arm cuff", ignoreCase = true)
                )
        ) {
            return false
        }
        if (lab && quality.source != FeatureSource.CLINICAL_REPORT) return false
        return true
    }

    private fun confidence(quality: FeatureQuality, lab: Boolean): Double {
        val sourceConfidence = when (quality.source) {
            FeatureSource.CLINICAL_REPORT -> 1.0
            FeatureSource.REAL_DEVICE, FeatureSource.DERIVED -> 0.8
            FeatureSource.USER_REPORTED -> 0.6
            FeatureSource.UNKNOWN -> 0.0
        }
        if (!lab || quality.observedAt == null) return sourceConfidence
        val ageDays = ((nowMillis - quality.observedAt).coerceAtLeast(0L)).toDouble() / MILLIS_PER_DAY
        val freshness = when {
            ageDays <= 90.0 -> 1.0
            ageDays <= 180.0 -> 0.8
            ageDays <= 365.0 -> 0.5
            else -> 0.2
        }
        return sourceConfidence * freshness
    }

    private fun bmi(value: Double?): Double {
        val measured = number(value)
        return when {
            measured < 18.5 -> clamp((18.5 - measured) * 0.5, 0.0, 2.0)
            measured < 24.0 -> 0.0
            measured < 28.0 -> clamp((measured - 24.0) * 0.5, 0.0, 2.0)
            else -> clamp(2.0 + (measured - 28.0) * 0.75, 2.0, 5.0)
        }
    }

    private fun bloodPressure(
        value: Double?,
        reference: Double,
        step: Double,
        pointsPerStep: Double,
        minimum: Double,
        maximum: Double,
    ): Double {
        val measured = number(value)
        if ((reference == 120.0 && measured < 90.0) || (reference == 80.0 && measured < 60.0)) {
            return 0.0
        }
        return clamp((measured - reference) / step * pointsPerStep, minimum, maximum)
    }

    private fun fastingGlucose(value: Double?, target: Double): Double {
        val measured = number(value)
        return if (measured < 3.9) 0.0 else linear(measured, target, 0.5, 0.6, -1.2, 4.0)
    }

    private fun linear(
        value: Number?,
        reference: Double,
        step: Double,
        pointsPerStep: Double,
        minimum: Double,
        maximum: Double,
    ): Double = clamp((number(value) - reference) / step * pointsPerStep, minimum, maximum)

    private fun reverseLinear(
        value: Number?,
        reference: Double,
        step: Double,
        pointsPerStep: Double,
        minimum: Double,
        maximum: Double,
    ): Double = clamp((reference - number(value)) / step * pointsPerStep, minimum, maximum)

    private fun applyDomainCap(
        contributions: MutableMap<String, Double>,
        fields: List<String>,
        minimum: Double,
        maximum: Double,
    ) {
        val present = fields.filter(contributions::containsKey)
        val total = present.sumOf { contributions.getValue(it) }
        val capped = clamp(total, minimum, maximum)
        if (present.isNotEmpty() && total != 0.0 && capped != total) {
            val scale = capped / total
            present.forEach { field -> contributions[field] = contributions.getValue(field) * scale }
        }
    }

    private fun number(value: Number?): Double =
        value?.toDouble()?.takeIf(Double::isFinite) ?: 0.0

    private fun clamp(value: Double, minimum: Double, maximum: Double): Double =
        value.coerceIn(minimum, maximum)

    private fun rounded(value: Double): Double {
        val result = round(value * 10_000.0) / 10_000.0
        return if (abs(result) < 0.00005) 0.0 else result
    }

    private companion object {
        val CLINICAL_80_20_FIELDS = setOf(
            CvdFeatureFields.SBP,
            CvdFeatureFields.DBP,
            CvdFeatureFields.FASTING_GLUCOSE,
            CvdFeatureFields.TOTAL_CHOLESTEROL,
            CvdFeatureFields.LDL,
            CvdFeatureFields.HDL,
            CvdFeatureFields.TRIGLYCERIDES,
        )
        const val MILLIS_PER_DAY = 86_400_000.0
    }
}
