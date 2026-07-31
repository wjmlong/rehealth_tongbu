package com.rehealth.genie.rhi

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RhiPeriodAggregatorTest {
    private val today = LocalDate.of(2026, 7, 31)

    @Test
    fun `seven day period uses the current RHI`() {
        val current = RhiDailyScore(today, 72.46, 0.82)
        val result = RhiPeriodAggregator.summarize(
            periodDays = 7,
            current = current,
            dailyScores = scores(7) { 60.0 + it },
        )

        assertEquals(72.5, result.score)
        assertEquals(RhiPeriodAggregation.CURRENT_7_DAY, result.aggregation)
    }

    @Test
    fun `thirty day period uses robust median and rejects an outlier`() {
        val values = listOf(65.0, 66.0, 67.0, 68.0, 69.0, 100.0, 70.0)
        val result = RhiPeriodAggregator.summarize(
            periodDays = 30,
            current = null,
            dailyScores = values.mapIndexed { index, value ->
                RhiDailyScore(today.minusDays(index.toLong()), value, 0.8)
            },
        )

        assertEquals(68.0, result.score)
        assertEquals(RhiPeriodAggregation.ROBUST_MEDIAN, result.aggregation)
    }

    @Test
    fun `ninety day period waits for fourteen valid days`() {
        val insufficient = RhiPeriodAggregator.summarize(
            periodDays = 90,
            current = null,
            dailyScores = scores(13) { 65.0 + it },
        )
        val sufficient = RhiPeriodAggregator.summarize(
            periodDays = 90,
            current = null,
            dailyScores = scores(14) { 65.0 + it },
        )

        assertNull(insufficient.score)
        assertEquals(14, insufficient.requiredValidDays)
        assertEquals(71.5, sufficient.score)
    }

    private fun scores(count: Int, value: (Int) -> Double): List<RhiDailyScore> =
        (0 until count).map { index ->
            RhiDailyScore(today.minusDays(index.toLong()), value(index), 0.8)
        }
}
