package com.rehealth.genie.rdi

import java.time.LocalDate

data class RdiDailyScore(
    val date: LocalDate,
    val score: Double,
    val confidence: Double,
)

data class RdiPeriodSummary(
    val periodDays: Int,
    val score: Double?,
    val confidence: Double?,
    val validDays: Int,
    val requiredValidDays: Int,
    val aggregation: RdiPeriodAggregation,
    val history: List<RdiDailyScore>,
    val scenario: RdiScenarioForecast? = null,
    val impact: RdiPeriodImpact? = null,
)

enum class RdiPeriodAggregation {
    CURRENT_7_DAY,
    ROBUST_MEDIAN,
}

object RdiPeriodAggregator {
    fun summarize(
        periodDays: Int,
        currentScore: Double?,
        currentConfidence: Double?,
        dailyScores: List<RdiDailyScore>,
        scenario: RdiScenarioForecast? = null,
    ): RdiPeriodSummary {
        require(periodDays in SUPPORTED_PERIODS) { "RDI period must be 7, 30, or 90 days" }
        val ordered = dailyScores.sortedBy { it.date }.takeLast(periodDays)
        val required = when (periodDays) {
            7 -> 1
            30 -> 7
            else -> 14
        }
        val useCurrent = periodDays == 7
        val score = if (useCurrent) {
            currentScore?.takeIf { currentConfidence != null && currentConfidence >= MIN_VALID_CONFIDENCE }
        } else {
            ordered.map { it.score }.takeIf { it.size >= required }?.median()
        }
        val confidence = if (useCurrent) {
            currentConfidence?.takeIf { score != null }
        } else {
            ordered.map { it.confidence }.takeIf { it.size >= required }?.median()
        }
        return RdiPeriodSummary(
            periodDays = periodDays,
            score = score?.round1(),
            confidence = confidence?.round3(),
            validDays = ordered.size,
            requiredValidDays = required,
            aggregation = if (useCurrent) {
                RdiPeriodAggregation.CURRENT_7_DAY
            } else {
                RdiPeriodAggregation.ROBUST_MEDIAN
            },
            history = ordered,
            scenario = scenario,
        )
    }

    const val MIN_VALID_CONFIDENCE = 0.20
    private val SUPPORTED_PERIODS = setOf(7, 30, 90)
}

private fun List<Double>.median(): Double {
    val ordered = sorted()
    val middle = ordered.size / 2
    return if (ordered.size % 2 == 0) {
        (ordered[middle - 1] + ordered[middle]) / 2.0
    } else {
        ordered[middle]
    }
}

private fun Double.round1(): Double = kotlin.math.round(this * 10.0) / 10.0
private fun Double.round3(): Double = kotlin.math.round(this * 1_000.0) / 1_000.0
