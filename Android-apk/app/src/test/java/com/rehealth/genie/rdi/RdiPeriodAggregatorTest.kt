package com.rehealth.genie.rdi

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RdiPeriodAggregatorTest {
    private val today = LocalDate.of(2026, 7, 31)

    @Test
    fun `seven day period uses current score calculated from recent valid data`() {
        val result = RdiPeriodAggregator.summarize(
            periodDays = 7,
            currentScore = 47.64,
            currentConfidence = 0.82,
            dailyScores = scores(7) { 60.0 + it },
        )

        assertEquals(47.6, result.score)
        assertEquals(RdiPeriodAggregation.CURRENT_7_DAY, result.aggregation)
        assertEquals(7, result.validDays)
    }

    @Test
    fun `thirty day period uses median of valid daily scores`() {
        val values = listOf(45.0, 46.0, 47.0, 48.0, 49.0, 90.0, 50.0)
        val result = RdiPeriodAggregator.summarize(
            periodDays = 30,
            currentScore = 99.0,
            currentConfidence = 0.9,
            dailyScores = values.mapIndexed { index, value ->
                RdiDailyScore(today.minusDays(index.toLong()), value, 0.8)
            },
        )

        assertEquals(48.0, result.score)
        assertEquals(RdiPeriodAggregation.ROBUST_MEDIAN, result.aggregation)
        assertEquals(7, result.validDays)
    }

    @Test
    fun `ninety day period requires fourteen valid days`() {
        val result = RdiPeriodAggregator.summarize(
            periodDays = 90,
            currentScore = 47.6,
            currentConfidence = 0.9,
            dailyScores = scores(13) { 50.0 + it },
        )

        assertNull(result.score)
        assertEquals(13, result.validDays)
        assertEquals(14, result.requiredValidDays)
    }

    @Test
    fun `current score is hidden when confidence is below valid threshold`() {
        val result = RdiPeriodAggregator.summarize(
            periodDays = 7,
            currentScore = 47.6,
            currentConfidence = 0.19,
            dailyScores = emptyList(),
        )

        assertNull(result.score)
    }

    @Test
    fun `unsupported period is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            RdiPeriodAggregator.summarize(14, 50.0, 0.8, emptyList())
        }
    }

    private fun scores(count: Int, score: (Int) -> Double): List<RdiDailyScore> =
        (0 until count).map { index ->
            RdiDailyScore(
                date = today.minusDays(index.toLong()),
                score = score(index),
                confidence = 0.8,
            )
        }
}
