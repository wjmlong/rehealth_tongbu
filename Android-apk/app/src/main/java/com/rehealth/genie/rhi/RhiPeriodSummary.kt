package com.rehealth.genie.rhi

import java.time.LocalDate

data class RhiDailyScore(
    val date: LocalDate,
    val score: Double,
    val confidence: Double,
)

data class RhiPeriodSummary(
    val periodDays: Int,
    val score: Double?,
    val confidence: Double?,
    val validDays: Int,
    val requiredValidDays: Int,
    val aggregation: RhiPeriodAggregation,
    val history: List<RhiDailyScore>,
    val algorithmVersion: String = RHI_LITE_ALGORITHM_VERSION,
)

enum class RhiPeriodAggregation {
    CURRENT_7_DAY,
    ROBUST_MEDIAN,
}

object RhiPeriodAggregator {
    fun summarize(
        periodDays: Int,
        current: RhiDailyScore?,
        dailyScores: List<RhiDailyScore>,
    ): RhiPeriodSummary {
        require(periodDays in SUPPORTED_PERIODS) { "RHI period must be 7, 30, or 90 days" }
        val ordered = dailyScores.sortedBy { it.date }.takeLast(periodDays)
        val required = when (periodDays) {
            7 -> 1
            30 -> 7
            else -> 14
        }
        val score = if (periodDays == 7) {
            current?.score
        } else {
            ordered.map { it.score }.takeIf { it.size >= required }?.median()
        }
        val confidence = if (periodDays == 7) {
            current?.confidence
        } else {
            ordered.map { it.confidence }.takeIf { it.size >= required }?.median()
        }
        return RhiPeriodSummary(
            periodDays = periodDays,
            score = score?.round1(),
            confidence = confidence?.round3(),
            validDays = ordered.size,
            requiredValidDays = required,
            aggregation = if (periodDays == 7) {
                RhiPeriodAggregation.CURRENT_7_DAY
            } else {
                RhiPeriodAggregation.ROBUST_MEDIAN
            },
            history = ordered,
        )
    }

    const val MIN_VALID_CONFIDENCE = 0.10
    private val SUPPORTED_PERIODS = setOf(7, 30, 90)
}

private fun List<Double>.median(): Double {
    val values = sorted()
    val middle = values.size / 2
    return if (values.size % 2 == 0) {
        (values[middle - 1] + values[middle]) / 2.0
    } else {
        values[middle]
    }
}

private fun Double.round1(): Double = kotlin.math.round(this * 10.0) / 10.0
private fun Double.round3(): Double = kotlin.math.round(this * 1_000.0) / 1_000.0
