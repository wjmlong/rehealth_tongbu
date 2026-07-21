package com.rehealth.genie.phm

import com.rehealth.genie.features.CvdFeatureFields

/**
 * Deterministic, transparent CVD risk heuristic used ONLY for the offline / computed
 * fallback path (when the real backend model-service is unavailable). It is NOT a medical
 * diagnosis and must never be presented as one; it exists so the app's local fallback
 * returns values COMPUTED from the available feature vector instead of canned/random
 * numbers (Requirement C: "用计算得到的数据发模拟数据").
 *
 * The model mirrors the direction of the canonical 16-feature contract
 * (CvdFeatureFields.ALL / model-service API_CONTRACT.md): age, blood pressure, BMI,
 * smoking, diabetes/hypertension/family history, glucose, and exercise all move the
 * risk in the expected direction. Output is a logistic squash of a weighted logit into
 * the inclusive 0.0..1.0 range, which is stable and reproducible for the same inputs.
 */
object CvdRiskHeuristic {

    /** @return risk score in 0.0..1.0, deterministic for the same [vector]. */
    fun score(vector: CvdFeatureVector): Double {
        var logit = BASE_LOGIT

        vector.age?.takeIf { it in 18..120 }?.let { logit += (it - 40) * 0.025 }
        vector.gender?.takeIf { it == 1 }?.let { logit += 0.15 } // male baseline adjust
        vector.sbp?.takeIf { it in 70.0..250.0 }?.let { logit += (it - 118) * 0.018 }
        vector.dbp?.takeIf { it in 40.0..150.0 }?.let { logit += (it - 78) * 0.03 }
        vector.bmi?.takeIf { it in 10.0..80.0 }?.let { logit += (it - 24) * 0.045 }
        vector.fastingGlucose?.takeIf { it > 0 }?.let { logit += if (it >= 6.1) 0.5 else 0.0 }
        vector.totalCholesterol?.takeIf { it > 0 }?.let { logit += (it - 5.0) * 0.06 }
        vector.ldl?.takeIf { it > 0 }?.let { logit += (it - 3.0) * 0.08 }
        vector.hdl?.takeIf { it > 0 }?.let { logit -= (it - 1.3) * 0.1 } // protective
        vector.triglycerides?.takeIf { it > 0 }?.let { logit += (it - 1.7) * 0.05 }
        vector.exerciseDays?.takeIf { it in 0..7 }?.let { logit -= it * 0.09 } // protective
        vector.smoking?.takeIf { it == 1 }?.let { logit += 0.6 }
        vector.drinking?.takeIf { it == 1 }?.let { logit += 0.2 }
        vector.diabetesHistory?.takeIf { it == 1 }?.let { logit += 0.8 }
        vector.hypertensionHistory?.takeIf { it == 1 }?.let { logit += 0.7 }
        vector.familyHistory?.takeIf { it == 1 }?.let { logit += 0.4 }

        return sigmoid(logit).coerceIn(0.0, 1.0)
    }

    /** Maps a [score] to the canonical risk level string. */
    fun level(score: Double): String = when {
        score < LOW_THRESHOLD -> "low"
        score < MODERATE_THRESHOLD -> "moderate"
        score < HIGH_THRESHOLD -> "high"
        else -> "very_high"
    }

    /** Human-readable label (zh) for a [score]. */
    fun label(score: Double): String = when (level(score)) {
        "low" -> "良好"
        "moderate" -> "中等"
        "high" -> "偏高"
        else -> "高"
    }

    /** Conservative, non-diagnostic summary text for a computed fallback score. */
    fun summary(score: Double, vector: CvdFeatureVector): String {
        val missing = CvdFeatureFields.ALL.count { field ->
            when (field) {
                CvdFeatureFields.AGE -> vector.age == null
                CvdFeatureFields.GENDER -> vector.gender == null
                CvdFeatureFields.BMI -> vector.bmi == null
                CvdFeatureFields.SBP -> vector.sbp == null
                CvdFeatureFields.DBP -> vector.dbp == null
                CvdFeatureFields.FASTING_GLUCOSE -> vector.fastingGlucose == null
                CvdFeatureFields.TOTAL_CHOLESTEROL -> vector.totalCholesterol == null
                CvdFeatureFields.LDL -> vector.ldl == null
                CvdFeatureFields.HDL -> vector.hdl == null
                CvdFeatureFields.TRIGLYCERIDES -> vector.triglycerides == null
                CvdFeatureFields.EXERCISE_DAYS -> vector.exerciseDays == null
                CvdFeatureFields.SMOKING -> vector.smoking == null
                CvdFeatureFields.DRINKING -> vector.drinking == null
                CvdFeatureFields.DIABETES_HISTORY -> vector.diabetesHistory == null
                CvdFeatureFields.HYPERTENSION_HISTORY -> vector.hypertensionHistory == null
                CvdFeatureFields.FAMILY_HISTORY -> vector.familyHistory == null
                else -> true
            }
        }
        return if (missing == 0) {
            "根据本地可计算特征估算的心血管风险为${label(score)}，仅供参考，不作诊断。"
        } else {
            "根据本地可计算特征估算的风险为${label(score)}；尚有 $missing 项特征缺失，结果仅供参考。"
        }
    }

    private fun sigmoid(x: Double): Double = 1.0 / (1.0 + kotlin.math.exp(-x))

    private const val BASE_LOGIT = -4.2
    private const val LOW_THRESHOLD = 0.15
    private const val MODERATE_THRESHOLD = 0.35
    private const val HIGH_THRESHOLD = 0.6
}
