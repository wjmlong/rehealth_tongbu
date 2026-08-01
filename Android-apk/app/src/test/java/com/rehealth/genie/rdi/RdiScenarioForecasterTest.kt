package com.rehealth.genie.rdi

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RdiScenarioForecasterTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 8, 1)

    @Test
    fun `runs both thirty day arms through RDI and returns deterministic interval`() {
        val first = forecast(
            interventions = listOf(
                RdiScenarioIntervention(id = "walking_zone2", status = "active"),
                RdiScenarioIntervention(id = "sleep_baseline", status = "active"),
            ),
        )
        val second = forecast(
            interventions = listOf(
                RdiScenarioIntervention(id = "walking_zone2", status = "active"),
                RdiScenarioIntervention(id = "sleep_baseline", status = "active"),
            ),
        )

        assertNotNull(first)
        assertEquals(first, second)
        assertEquals(31, first.noAction.size)
        assertEquals(31, first.withPlan.size)
        assertEquals(31, first.ciLower.size)
        assertEquals(31, first.ciUpper.size)
        assertEquals(50.0, first.noAction.first(), 0.001)
        assertEquals(50.0, first.withPlan.first(), 0.001)
        assertTrue(first.noAction.all { it == 50.0 })
        assertTrue(first.d30WithPlan < first.d30NoAction)
        assertTrue(first.expectedReduction > 0.0)
        first.ciLower.indices.forEach { index ->
            assertTrue(first.ciLower[index] <= first.ciUpper[index])
            assertTrue(first.withPlan[index] in first.ciLower[index]..first.ciUpper[index])
        }
        assertEquals(RdiScenarioForecast.INTERVAL_METHOD, first.intervalMethod)
    }

    @Test
    fun `selected period score anchors a distinct horizontal no action baseline`() {
        val seven = assertNotNull(forecast(interventions = supportedPlan(), referenceDays = 7, currentScore = 47.3))
        val ninety = assertNotNull(forecast(interventions = supportedPlan(), referenceDays = 90, currentScore = 53.6))

        assertTrue(seven.noAction.all { it == 47.3 })
        assertTrue(ninety.noAction.all { it == 53.6 })
        assertTrue(seven.d30WithPlan != ninety.d30WithPlan)
    }

    @Test
    fun `does not invent a forecast without an explicit supported plan`() {
        assertNull(forecast(interventions = emptyList()))
        assertNull(
            forecast(
                interventions = listOf(
                    RdiScenarioIntervention(id = "bp_monitor", title = "血压监测", status = "active"),
                ),
            ),
        )
    }

    private fun supportedPlan() = listOf(
        RdiScenarioIntervention(id = "walking_zone2", status = "active"),
        RdiScenarioIntervention(id = "sleep_baseline", status = "active"),
    )

    private fun forecast(
        interventions: List<RdiScenarioIntervention>,
        referenceDays: Int = 30,
        currentScore: Double = 50.0,
    ): RdiScenarioForecast? =
        RdiScenarioForecaster.forecast(
            scoredOn = today,
            zoneId = zone,
            activities = (0L..89L).map(::activity),
            sleepSessions = (0L..89L).map(::sleep),
            measurements = (0L..89L).map(::hrv),
            currentScore = currentScore,
            referenceDays = referenceDays,
            anchoredBaselines = mapOf(
                "steps" to 5_000.0,
                "verified_activity_minutes" to 140.0,
                "sleep_duration" to 420.0,
                "sleep_efficiency" to 90.0,
                "hrv_personal_trend" to 45.0,
                "resting_hr" to 95.0,
            ),
            bloodPressure = null,
            confirmedLabs = emptyList(),
            confirmedMeals = emptyList(),
            interventions = interventions,
            isMock = true,
        )

    private fun activity(daysAgo: Long): RingActivityEntity {
        val startedAt = today.minusDays(daysAgo).atTime(18, 0).atZone(zone).toInstant().toEpochMilli()
        return RingActivityEntity(
            id = "activity-$daysAgo",
            startedAt = startedAt,
            endedAt = startedAt + 20 * 60_000L,
            activityType = "walking",
            steps = 5_000 + ((daysAgo % 4) * 180).toInt(),
            distanceMeters = 3_500.0,
            caloriesKcal = 190.0,
            durationMinutes = 20 + (daysAgo % 3).toInt(),
            averageHeartRate = 95.0 + (daysAgo % 3),
            source = "ring-a",
        )
    }

    private fun sleep(daysAgo: Long): RingSleepSessionEntity {
        val endedAt = today.minusDays(daysAgo).atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
        val duration = 420 + ((daysAgo % 3) * 10).toInt()
        val awake = 35 + (daysAgo % 2).toInt()
        val asleep = duration - awake
        return RingSleepSessionEntity(
            id = "sleep-$daysAgo",
            startedAt = endedAt - duration * 60_000L,
            endedAt = endedAt,
            deepMinutes = asleep * 22 / 100,
            lightMinutes = asleep * 58 / 100,
            awakeMinutes = awake,
            remMinutes = asleep - asleep * 22 / 100 - asleep * 58 / 100,
            interruptionMinutes = 8,
            source = "ring-a",
            totalSleepMinutes = duration,
        )
    }

    private fun hrv(daysAgo: Long): RingMeasurementEntity = RingMeasurementEntity(
        id = "hrv-$daysAgo",
        metricType = RingMetricType.HRV.name,
        measuredAt = today.minusDays(daysAgo).atTime(3, 0).atZone(zone).toInstant().toEpochMilli(),
        primaryValue = 45.0 + (daysAgo % 4),
        unit = "ms",
        quality = 95,
        source = "ring-a",
    )
}
