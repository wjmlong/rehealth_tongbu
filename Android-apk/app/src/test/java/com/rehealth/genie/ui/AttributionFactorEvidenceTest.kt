package com.rehealth.genie.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttributionFactorEvidenceTest {
    @Test
    fun `all sixteen factors provide value aware explanation and recommendation`() {
        val evidence = AttributionUiMapper.CANONICAL_FACTOR_KEYS.map { key ->
            attributionFactorEvidence(factor(key = key, value = "测试值"))
        }

        assertEquals(16, evidence.size)
        assertTrue(evidence.all { it.explanation.contains("测试值") })
        assertTrue(evidence.all { it.recommendation.isNotBlank() })
        assertTrue(evidence.map { it.recommendation }.distinct().size >= 12)
    }

    @Test
    fun `clinical explanations describe confirmed inputs without claiming diagnosis`() {
        val systolic = attributionFactorEvidence(factor("sbp", "118 mmHg"))
        val glucose = attributionFactorEvidence(factor("fasting_glucose", "5.2 mmol/L"))

        assertTrue(systolic.explanation.contains("经确认输入"))
        assertTrue(glucose.explanation.contains("经确认报告值"))
        assertFalse(systolic.explanation.contains("诊断为"))
    }

    @Test
    fun `missing values remain explicit in explanations`() {
        val systolic = attributionFactorEvidence(factor("sbp", null))
        val exercise = attributionFactorEvidence(factor("exercise_days", null))

        assertTrue(systolic.explanation.contains("当前值未提供"))
        assertTrue(exercise.explanation.contains("当前值未提供"))
    }

    private fun factor(
        key: String,
        value: String?,
    ): AttributionFactorUi =
        AttributionFactorUi(
            key = key,
            label = key,
            section = "test",
            value = value,
            contribution = 0.1,
            contributionRuleVersion = "factor16-rule-v1.0.0",
            measuredComponent = null,
            controlSupportComponent = null,
        )
}
