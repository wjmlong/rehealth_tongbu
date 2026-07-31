package com.rehealth.genie.rhi

import com.rehealth.genie.ring.data.RingActivityEntity
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RhiLiteEngineTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 7, 31)

    @Test
    fun `more recent steps produce a higher RHI than fewer steps`() {
        val improved = calculate(currentSteps = 8_000)
        val worsened = calculate(currentSteps = 1_000)

        assertEquals(52.5, improved.rawScore)
        assertEquals(47.4, worsened.rawScore)
        assertTrue(improved.rawScore > worsened.rawScore)
        assertTrue(improved.domains.getValue("activity_fitness")!! > 50.0)
        assertTrue(worsened.domains.getValue("activity_fitness")!! < 50.0)
    }

    @Test
    fun `missing wearable data stays neutral with zero confidence`() {
        val result = RhiLiteEngine.calculate(
            RhiLiteCalculationInput(
                scoredOn = today,
                zoneId = zoneId,
                activities = emptyList(),
                sleepSessions = emptyList(),
                measurements = emptyList(),
                previousDisplayScore = null,
            ),
        )

        assertEquals(50.0, result.rawScore)
        assertEquals(0.0, result.confidence)
        assertEquals(0, result.availableFeatureCount)
    }

    @Test
    fun `display score uses RHI quarter smoothing`() {
        val raw = calculate(currentSteps = 8_000, previousDisplay = null)
        val smoothed = calculate(currentSteps = 8_000, previousDisplay = 50.0)

        assertEquals(
            kotlin.math.round((0.25 * raw.rawScore + 0.75 * 50.0) * 10.0) / 10.0,
            smoothed.displayScore,
        )
    }

    @Test
    fun `falling confidence cannot improve the displayed RHI`() {
        val result = RhiLiteEngine.calculate(
            RhiLiteCalculationInput(
                scoredOn = today,
                zoneId = zoneId,
                activities = emptyList(),
                sleepSessions = emptyList(),
                measurements = emptyList(),
                previousDisplayScore = 40.0,
                previousConfidence = 0.8,
            ),
        )

        assertEquals(40.0, result.displayScore)
    }

    private fun calculate(
        currentSteps: Int,
        previousDisplay: Double? = null,
    ): RhiLiteCalculation {
        val activities = (0L..34L).map { daysAgo ->
            val date = today.minusDays(daysAgo)
            RingActivityEntity(
                id = "steps-$daysAgo-$currentSteps",
                startedAt = date.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                endedAt = null,
                activityType = "daily_summary",
                steps = if (daysAgo <= 6) currentSteps else 3_000,
                distanceMeters = 0.0,
                caloriesKcal = 0.0,
                durationMinutes = 0,
                averageHeartRate = null,
                source = "TEST_DEVICE",
            )
        }
        return RhiLiteEngine.calculate(
            RhiLiteCalculationInput(
                scoredOn = today,
                zoneId = zoneId,
                activities = activities,
                sleepSessions = emptyList(),
                measurements = emptyList(),
                previousDisplayScore = previousDisplay,
            ),
        )
    }
}
