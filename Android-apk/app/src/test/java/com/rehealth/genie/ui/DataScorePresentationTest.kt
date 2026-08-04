package com.rehealth.genie.ui

import com.rehealth.genie.rhi.RHI_LITE_ALGORITHM_VERSION
import com.rehealth.genie.rhi.RhiPeriodAggregation
import com.rehealth.genie.rhi.RhiPeriodSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataScorePresentationTest {
    @Test
    fun `real RDI sixteen feature risk is displayed`() {
        val presentation = dataRiskPresentation(
            RemoteFeatureEvaluateStatus(
                reachable = true,
                modelVersion = "cvd-core16-v1",
                isMock = false,
                riskLevel = "low",
                riskScore = 0.028,
                summary = "评估完成",
            ),
        )

        assertEquals("2.8%", presentation.scoreText)
        assertEquals("RDI-16 · 风险概率", presentation.sourceText)
        assertEquals("低", presentation.riskLevelText)
    }

    @Test
    fun `mock risk is not displayed as a real score`() {
        val presentation = dataRiskPresentation(
            RemoteFeatureEvaluateStatus(
                reachable = true,
                modelVersion = "cvd-mock-rules-v1",
                isMock = true,
                riskLevel = "low",
                riskScore = 0.12,
                summary = "模拟评估",
            ),
        )

        assertEquals("--", presentation.scoreText)
        assertEquals("结果不可用", presentation.sourceText)
    }

    @Test
    fun `invalid RDI risk value is not displayed`() {
        val presentation = dataRiskPresentation(
            RemoteFeatureEvaluateStatus(
                reachable = true,
                modelVersion = "cvd-core16-v1",
                isMock = false,
                riskLevel = "low",
                riskScore = Double.NaN,
                summary = "异常结果",
            ),
        )

        assertEquals("--", presentation.scoreText)
        assertEquals("待评估", presentation.riskLevelText)
    }

    @Test
    fun `health index displays current RHI with one decimal`() {
        val presentation = dataHealthIndexPresentation(
            summary = summary(
                periodDays = 7,
                score = 72.4,
                aggregation = RhiPeriodAggregation.CURRENT_7_DAY,
            ),
            error = null,
        )

        assertEquals("72.4", presentation.scoreText)
        assertEquals("良好", presentation.statusText)
        assertEquals("RHI-100 · 近7日有效数据", presentation.supportingText)
        assertEquals(260.64f, presentation.sweepAngle, absoluteTolerance = 0.01f)
    }

    @Test
    fun `missing RHI remains in accumulating state`() {
        val presentation = dataHealthIndexPresentation(
            summary = summary(
                periodDays = 30,
                score = null,
                aggregation = RhiPeriodAggregation.ROBUST_MEDIAN,
                validDays = 4,
                requiredDays = 7,
            ),
            error = null,
        )

        assertEquals("--", presentation.scoreText)
        assertEquals("积累中", presentation.statusText)
        assertTrue(presentation.supportingText.contains("4/7"))
    }

    private fun summary(
        periodDays: Int,
        score: Double?,
        aggregation: RhiPeriodAggregation,
        validDays: Int = 7,
        requiredDays: Int = 1,
    ): RhiPeriodSummary =
        RhiPeriodSummary(
            periodDays = periodDays,
            score = score,
            confidence = 0.8,
            validDays = validDays,
            requiredValidDays = requiredDays,
            aggregation = aggregation,
            history = emptyList(),
            algorithmVersion = RHI_LITE_ALGORITHM_VERSION,
        )
}
