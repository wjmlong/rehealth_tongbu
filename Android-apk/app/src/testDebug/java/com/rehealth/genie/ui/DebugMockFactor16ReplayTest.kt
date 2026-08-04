package com.rehealth.genie.ui

import com.rehealth.genie.features.CvdFeatureFields
import com.rehealth.genie.features.CvdFeatureVector
import com.rehealth.genie.features.BaselineHealthProfile
import com.rehealth.genie.features.FeatureQuality
import com.rehealth.genie.features.FeatureSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DebugMockFactor16ReplayTest {
    @Test
    fun `normal 50 year old mock profile fills all 16 governed contributions`() {
        val result = requireNotNull(
            RuntimeFactor16Fallback.evaluate(
                vector = normalMockVector(),
                enabled = true,
                nowMillis = NOW,
            ),
        )

        assertEquals(DEBUG_MOCK_FACTOR16_RULE_VERSION, result.ruleVersion)
        assertEquals(
            mapOf(
                "age" to 0.96,
                "gender" to 0.9,
                "bmi" to 0.0,
                "sbp" to -0.192,
                "dbp" to -0.32,
                "fasting_glucose" to -0.96,
                "total_cholesterol" to -0.196,
                "ldl" to -1.728,
                "hdl" to -0.56,
                "triglycerides" to -0.576,
                "exercise_days" to -0.96,
                "smoking" to 0.0,
                "drinking" to 0.0,
                "diabetes_history" to 0.0,
                "hypertension_history" to 0.0,
                "family_history" to 0.0,
            ),
            result.contributions,
        )
        assertEquals(-0.192, result.measuredComponents.getValue("sbp"))
        assertEquals(-0.96, result.measuredComponents.getValue("fasting_glucose"))
        assertEquals(0.0, result.controlSupportComponents.getValue("sbp"))
        assertEquals(0.0, result.controlSupportComponents.getValue("fasting_glucose"))
    }

    @Test
    fun `replay is disabled unless caller explicitly selects mock mode`() {
        assertNull(
            RuntimeFactor16Fallback.evaluate(
                normalMockVector(),
                enabled = false,
                nowMillis = NOW,
            ),
        )
    }

    @Test
    fun `offline debug replay confirms factors but never confirms risk`() {
        val evaluation = RemoteFeatureEvaluateStatus(
            reachable = false,
            modelVersion = null,
            isMock = null,
            riskLevel = null,
            riskScore = null,
            factorContributions = mapOf("age" to 0.96),
            factorContributionVersion = DEBUG_MOCK_FACTOR16_RULE_VERSION,
            summary = "offline debug replay",
        ).toAttributionRiskEvaluation()

        assertTrue(evaluation.factorConfirmed)
        assertFalse(evaluation.confirmed)
        assertNull(evaluation.riskScore)
    }

    @Test
    fun `mock profile cache completes behavior fields through the profile entry`() {
        val completed = requireNotNull(
            RuntimeFactor16Fallback.completeBaselineProfile(
                profile = BaselineHealthProfile(
                    age = 50,
                    gender = "male",
                    heightCm = 175.0,
                    weightKg = 68.6,
                ),
                enabled = true,
                nowMillis = NOW,
            ),
        )

        assertEquals(false, completed.smoking)
        assertEquals(false, completed.drinking)
        assertEquals(false, completed.diabetesHistory)
        assertEquals(false, completed.hypertensionHistory)
        assertEquals(false, completed.familyHistory)
        assertEquals(NOW, completed.updatedAt)
    }

    private fun normalMockVector(): CvdFeatureVector {
        val quality = CvdFeatureFields.ALL.associateWith { field ->
            when (field) {
                CvdFeatureFields.SBP, CvdFeatureFields.DBP -> FeatureQuality.valid(
                    source = FeatureSource.CLINICAL_REPORT,
                    observedAt = NOW,
                    reason = "Validated upper-arm cuff 7-day mean from 7 valid days.",
                )
                CvdFeatureFields.FASTING_GLUCOSE,
                CvdFeatureFields.TOTAL_CHOLESTEROL,
                CvdFeatureFields.LDL,
                CvdFeatureFields.HDL,
                CvdFeatureFields.TRIGLYCERIDES,
                -> FeatureQuality.valid(
                    source = FeatureSource.CLINICAL_REPORT,
                    observedAt = NOW,
                    reason = "Confirmed dated hospital report.",
                )
                CvdFeatureFields.EXERCISE_DAYS -> FeatureQuality.valid(
                    source = FeatureSource.REAL_DEVICE,
                    observedAt = NOW,
                    reason = "Seven valid activity days.",
                )
                else -> FeatureQuality.valid(
                    source = FeatureSource.USER_REPORTED,
                    observedAt = NOW,
                    reason = "Trusted profile.",
                )
            }
        }
        return CvdFeatureVector(
            age = 50,
            gender = 1,
            bmi = 22.4,
            sbp = 118.0,
            dbp = 76.0,
            fastingGlucose = 5.0,
            totalCholesterol = 4.5,
            ldl = 2.5,
            hdl = 1.35,
            triglycerides = 1.1,
            exerciseDays = 7,
            smoking = 0,
            drinking = 0,
            diabetesHistory = 0,
            hypertensionHistory = 0,
            familyHistory = 0,
            featureQuality = quality,
        )
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
