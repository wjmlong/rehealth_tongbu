package com.rehealth.genie.rhi

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RhiPeriodAggregatorTest {
    private val today = LocalDate.of(2026, 7, 31)

    @Test
    fun `today period uses only the current RHI`() {
        val current = RhiDailyScore(today, 72.46, 0.82)
        val result = RhiPeriodAggregator.summarize(
            periodDays = 1,
            current = current,
            dailyScores = scores(7) { 60.0 + it },
        )

        assertEquals(72.5, result.score)
        assertEquals(RhiPeriodAggregation.CURRENT_DAY, result.aggregation)
        assertEquals(1, result.validDays)
    }

    @Test
    fun `seven day period uses a robust median distinct from today`() {
        val history = listOf(61.0, 62.0, 64.0, 66.0, 68.0, 70.0, 92.0)
            .mapIndexed { index, score -> RhiDailyScore(today.minusDays((6 - index).toLong()), score, 0.8) }
        val result = RhiPeriodAggregator.summarize(
            periodDays = 7,
            current = RhiDailyScore(today, 92.0, 0.8),
            dailyScores = history,
        )

        assertEquals(66.0, result.score)
        assertEquals(RhiPeriodAggregation.ROBUST_MEDIAN, result.aggregation)
        assertEquals(3, result.requiredValidDays)
    }

    @Test
    fun `seven day period waits for three valid days`() {
        val result = RhiPeriodAggregator.summarize(
            periodDays = 7,
            current = RhiDailyScore(today, 72.0, 0.8),
            dailyScores = scores(2) { 70.0 + it },
        )

        assertNull(result.score)
        assertEquals(2, result.validDays)
        assertEquals(3, result.requiredValidDays)
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

    @Test
    fun `summary exposes truthful period movement alongside the aggregate score`() {
        val history = listOf(
            RhiDailyScore(today.minusDays(2), 64.2, 0.8),
            RhiDailyScore(today.minusDays(1), 65.7, 0.8),
            RhiDailyScore(today, 68.9, 0.8),
        )

        val result = RhiPeriodAggregator.summarize(
            periodDays = 7,
            current = history.last(),
            dailyScores = history,
        )

        assertEquals(65.7, result.score)
        assertEquals(4.7, result.trendDelta)
    }

    private fun scores(count: Int, value: (Int) -> Double): List<RhiDailyScore> =
        (0 until count).map { index ->
            RhiDailyScore(today.minusDays(index.toLong()), value(index), 0.8)
        }
}
