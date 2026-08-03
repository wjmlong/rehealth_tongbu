package com.rehealth.genie.rdi

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RdiPeriodImpactAggregatorTest {
    private val today = LocalDate.of(2026, 8, 3)

    @Test
    fun `top factor changes use the selected week month and quarter endpoints`() {
        val daily = (0 until 90).map { daysAgo ->
            RdiPeriodCalculation(
                date = today.minusDays(daysAgo.toLong()),
                calculation = calculation(
                    contribution(
                        "verified_activity_minutes",
                        "activity",
                        when (daysAgo) {
                            0 -> -2.0
                            6 -> 0.0
                            29 -> -1.7
                            89 -> -1.8
                            else -> -1.0
                        },
                    ),
                    contribution(
                        "resting_hr",
                        "recovery",
                        when (daysAgo) {
                            0 -> -1.0
                            6 -> -0.9
                            29 -> 2.5
                            89 -> -0.8
                            else -> 0.0
                        },
                    ),
                    contribution(
                        "steps",
                        "activity",
                        when (daysAgo) {
                            0 -> -0.5
                            6 -> -0.4
                            29 -> -0.4
                            89 -> 2.5
                            else -> 0.5
                        },
                    ),
                ),
            )
        }

        val week = RdiPeriodImpactAggregator.summarize(7, today, daily)
        val month = RdiPeriodImpactAggregator.summarize(30, today, daily)
        val quarter = RdiPeriodImpactAggregator.summarize(90, today, daily)

        assertEquals("verified_activity_minutes", week.factors.first().factorCode)
        assertEquals("resting_hr", month.factors.first().factorCode)
        assertEquals("steps", quarter.factors.first().factorCode)
        assertEquals(7, week.validDays)
        assertEquals(30, month.validDays)
        assertEquals(90, quarter.validDays)
        assertEquals(-2.0, week.factors.first().changePoints)
        assertEquals(-3.5, month.factors.first().changePoints)
        assertEquals(-3.0, quarter.factors.first().changePoints)
    }

    @Test
    fun `factor without evidence at both period endpoints is not presented as a change`() {
        val daily = (0 until 30).map { daysAgo ->
            RdiPeriodCalculation(
                date = today.minusDays(daysAgo.toLong()),
                calculation = calculation(
                    contribution("sleep_duration", "sleep", 0.0),
                    *listOfNotNull(
                        contribution("resting_hr", "recovery", 3.0).takeIf { daysAgo == 0 },
                    ).toTypedArray(),
                ),
            )
        }

        val result = RdiPeriodImpactAggregator.summarize(30, today, daily)

        assertTrue(result.factors.none { it.factorCode == "resting_hr" })
    }

    @Test
    fun `period contribution sign distinguishes risk reduction from risk increase`() {
        val daily = (0 until 7).map { daysAgo ->
            val progress = (6 - daysAgo) / 6.0
            RdiPeriodCalculation(
                today.minusDays(daysAgo.toLong()),
                calculation(
                    contribution("sleep_duration", "sleep", 2.2 - 2.3 * progress),
                    contribution("resting_hr", "recovery", -0.4 + 1.0 * progress),
                ),
            )
        }

        val result = RdiPeriodImpactAggregator.summarize(7, today, daily)

        assertEquals(-2.3, result.factors.single { it.factorCode == "sleep_duration" }.changePoints)
        assertEquals(1.0, result.factors.single { it.factorCode == "resting_hr" }.changePoints)
    }

    private fun calculation(vararg contributions: RdiContribution) = RdiCalculation(
        rawScore = 50.0 + contributions.sumOf { it.finalPoints },
        displayScore = 50.0 + contributions.sumOf { it.finalPoints },
        confidence = 0.9,
        status = RdiStatus.CONFIRMED,
        contributions = contributions.toList(),
    )

    private fun contribution(factorCode: String, domain: String, points: Double) = RdiContribution(
        factorCode = factorCode,
        domain = domain,
        source = "ROOM_WEARABLE",
        currentValue = 1.0,
        baselineValue = 1.0,
        unit = "unit",
        rawPoints = points,
        confidence = 0.9,
        finalPoints = points,
        evidenceText = "daily evidence",
        sourceFactorId = "$factorCode:$today",
    )
}
