package com.rehealth.genie.ring

import com.rehealth.genie.ring.data.RingActivityEntity
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyActivityStepsTest {
    @Test
    fun `uses highest cumulative watch total from current local calendar day`() {
        val localNoon = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 30, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val previousDay = Calendar.getInstance().apply {
            timeInMillis = localNoon
            add(Calendar.DAY_OF_MONTH, -1)
        }.timeInMillis

        val total = aggregateLocalDayActivitySteps(
            listOf(
                activity("morning", localNoon - 2 * 60 * 60 * 1_000L, 1_200),
                activity("afternoon", localNoon + 2 * 60 * 60 * 1_000L, 2_300),
                activity("yesterday", previousDay, 9_999),
            ),
            now = localNoon,
        )

        assertEquals(2_300L, total)
        assertEquals(
            listOf(2_300L, 9_999L),
            dailyActivityStepTotals(
                listOf(
                    activity("morning", localNoon - 2 * 60 * 60 * 1_000L, 1_200),
                    activity("afternoon", localNoon + 2 * 60 * 60 * 1_000L, 2_300),
                    activity("yesterday", previousDay, 9_999),
                ),
            ).values.sorted(),
        )
    }

    @Test
    fun `returns null when the local day has no activity rows`() {
        assertNull(aggregateLocalDayActivitySteps(emptyList(), now = 1_700_000_000_000L))
    }

    private fun activity(id: String, startedAt: Long, steps: Int) = RingActivityEntity(
        id = id,
        startedAt = startedAt,
        endedAt = null,
        activityType = "walking",
        steps = steps,
        distanceMeters = 0.0,
        caloriesKcal = 0.0,
        durationMinutes = 0,
        averageHeartRate = null,
        source = "test",
    )
}
