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
    val calculationSource: RhiCalculationSource = RhiCalculationSource.LOCAL,
    /** Current display score minus the score 7 days earlier; null when absent. */
    val delta7d: Double? = null,
    /** Current display score minus the score 28 days earlier; null when absent. */
    val delta28d: Double? = null,
    /** Earliest valid personal baseline inside the latest 90-day horizon. */
    val baseline90d: RhiDailyScore? = null,
) {
    val trendDelta: Double?
        get() = history.takeIf { it.size >= 2 }
            ?.let { (it.last().score - it.first().score).round1() }
}

enum class RhiCalculationSource {
    LOCAL,
    REMOTE,
}

enum class RhiPeriodAggregation {
    CURRENT_7_DAY,
    ROBUST_MEDIAN,
}

object RhiPeriodAggregator {
    fun summarize(
        periodDays: Int,
        current: RhiDailyScore?,
        dailyScores: List<RhiDailyScore>,
        algorithmVersion: String = RHI_LITE_ALGORITHM_VERSION,
        calculationSource: RhiCalculationSource = RhiCalculationSource.LOCAL,
        delta7d: Double? = null,
        delta28d: Double? = null,
        baseline90d: RhiDailyScore? = null,
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
            algorithmVersion = algorithmVersion,
            calculationSource = calculationSource,
            delta7d = delta7d?.round1(),
            delta28d = delta28d?.round1(),
            baseline90d = baseline90d,
        )
    }

    /**
     * Momentum against a fixed lookback rather than the first day of whatever
     * window the UI happens to show. A 90-day view and a 7-day view must report
     * the same 7-day change.
     */
    fun delta(
        scores: Map<java.time.LocalDate, Double>,
        scoredOn: java.time.LocalDate,
        lookbackDays: Long,
    ): Double? {
        val current = scores[scoredOn] ?: return null
        val past = scores[scoredOn.minusDays(lookbackDays)] ?: return null
        return current - past
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
