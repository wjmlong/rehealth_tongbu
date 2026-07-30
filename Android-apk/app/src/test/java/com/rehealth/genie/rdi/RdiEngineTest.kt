package com.rehealth.genie.rdi

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RdiEngineTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 7, 30)

    @Test
    fun `higher recent steps reduce impact score`() {
        val activities = (0L..27L).map { daysAgo ->
            activity(daysAgo, if (daysAgo <= 6) 6_000 else 3_000)
        }

        val result = calculate(activities = activities)
        val steps = result.contributions.single { it.factorCode == "steps" }

        assertTrue(steps.finalPoints < 0.0)
        assertTrue(result.rawScore < 50.0)
    }

    @Test
    fun `short sleep increases impact score`() {
        val sleeps = (0L..27L).map { daysAgo ->
            sleep(daysAgo, if (daysAgo <= 6) 6 * 60 else 8 * 60)
        }

        val result = calculate(sleepSessions = sleeps)
        val duration = result.contributions.single { it.factorCode == "sleep_duration" }

        assertTrue(duration.finalPoints > 0.0)
        assertTrue(result.rawScore > 50.0)
    }

    @Test
    fun `partial activity data shrinks contribution by confidence`() {
        val activities = (0L..2L).map { activity(it, 1_000) }

        val result = calculate(activities = activities)
        val steps = result.contributions.single { it.factorCode == "steps" }

        assertEquals(1.0, steps.rawPoints, 0.001)
        assertTrue(steps.confidence < 0.60)
        assertEquals(steps.rawPoints * steps.confidence, steps.finalPoints, 0.001)
    }

    @Test
    fun `device source change excludes HRV contribution`() {
        val measurements = (0L..13L).map { daysAgo ->
            measurement(
                daysAgo = daysAgo,
                value = if (daysAgo <= 6) 50.0 else 45.0,
                source = if (daysAgo <= 6) "ring-a" else "ring-b",
            )
        }

        val result = calculate(measurements = measurements)

        assertTrue(result.contributions.none { it.factorCode == "hrv_personal_trend" })
    }

    @Test
    fun `missing data freezes previous display score`() {
        val result = calculate(previousDisplay = 44.0)

        assertEquals(44.0, result.displayScore, 0.001)
        assertEquals("accumulating", result.status)
    }

    @Test
    fun `ordinary daily display change is capped at three points`() {
        val activities = (0L..27L).map { daysAgo ->
            activity(daysAgo, if (daysAgo <= 6) 1_000 else 10_000)
        }
        val sleeps = (0L..27L).map { daysAgo ->
            sleep(daysAgo, if (daysAgo <= 6) 4 * 60 else 8 * 60)
        }

        val result = calculate(
            activities = activities,
            sleepSessions = sleeps,
            previousDisplay = 40.0,
        )

        assertEquals(43.0, result.displayScore, 0.001)
    }

    private fun calculate(
        activities: List<RingActivityEntity> = emptyList(),
        sleepSessions: List<RingSleepSessionEntity> = emptyList(),
        measurements: List<RingMeasurementEntity> = emptyList(),
        previousDisplay: Double? = null,
    ): RdiCalculation = RdiEngine.calculate(
        RdiCalculationInput(
            scoredOn = today,
            zoneId = zone,
            activities = activities,
            sleepSessions = sleepSessions,
            measurements = measurements,
            previousDisplayScore = previousDisplay,
        ),
    )

    private fun activity(daysAgo: Long, steps: Int): RingActivityEntity {
        val startedAt = today.minusDays(daysAgo).atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
        return RingActivityEntity(
            id = "activity-$daysAgo",
            startedAt = startedAt,
            endedAt = startedAt,
            activityType = "daily",
            steps = steps,
            distanceMeters = 0.0,
            caloriesKcal = 0.0,
            durationMinutes = 0,
            averageHeartRate = null,
            source = "ring-a",
        )
    }

    private fun sleep(daysAgo: Long, durationMinutes: Int): RingSleepSessionEntity {
        val endedAt = today.minusDays(daysAgo).atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
        val startedAt = endedAt - durationMinutes * 60_000L
        return RingSleepSessionEntity(
            id = "sleep-$daysAgo",
            startedAt = startedAt,
            endedAt = endedAt,
            deepMinutes = durationMinutes / 4,
            lightMinutes = durationMinutes / 2,
            awakeMinutes = durationMinutes / 10,
            remMinutes = durationMinutes / 5,
            interruptionMinutes = 0,
            source = "ring-a",
        )
    }

    private fun measurement(daysAgo: Long, value: Double, source: String): RingMeasurementEntity {
        val measuredAt = today.minusDays(daysAgo).atTime(3, 0).atZone(zone).toInstant().toEpochMilli()
        return RingMeasurementEntity(
            id = "hrv-$daysAgo",
            metricType = RingMetricType.HRV.name,
            measuredAt = measuredAt,
            primaryValue = value,
            unit = "ms",
            quality = 95,
            source = source,
        )
    }
}
