package com.rehealth.genie.rdi

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.round

data class RdiPeriodCalculation(
    val date: LocalDate,
    val calculation: RdiCalculation,
)

data class RdiPeriodImpactFactor(
    val factorCode: String,
    val domain: String,
    /** End-of-period contribution minus start-of-period contribution. */
    val changePoints: Double,
    val startPoints: Double,
    val endPoints: Double,
    val dataConfidence: Double,
    val latestEvidence: String,
)

data class RdiPeriodImpact(
    val periodDays: Int,
    val validDays: Int,
    val dataConfidence: Double?,
    val factors: List<RdiPeriodImpactFactor>,
)

/** Compares native RDI contributions at the two ends of the selected UI period. */
object RdiPeriodImpactAggregator {
    fun summarize(
        periodDays: Int,
        scoredOn: LocalDate,
        dailyCalculations: List<RdiPeriodCalculation>,
    ): RdiPeriodImpact {
        require(periodDays in setOf(7, 30, 90)) { "RDI period must be 7, 30, or 90 days" }
        val cutoff = scoredOn.minusDays((periodDays - 1).toLong())
        val selected = dailyCalculations.filter { it.date in cutoff..scoredOn }
            .filter { it.calculation.contributions.isNotEmpty() }
            .sortedBy { it.date }
        val validDays = selected.size
        val confidence = selected.map { it.calculation.confidence }.medianOrNull()?.round3()
        if (validDays < 2) {
            return RdiPeriodImpact(periodDays, validDays, confidence, emptyList())
        }
        val start = selected.first().calculation.contributions.eligibleByFactor()
        val end = selected.last().calculation.contributions.eligibleByFactor()
        val periodCoverage = (validDays.toDouble() / periodDays).coerceIn(0.0, 1.0)
        val factors = (start.keys intersect end.keys).mapNotNull { factorCode ->
                val startContribution = start.getValue(factorCode)
                val endContribution = end.getValue(factorCode)
                val changePoints = endContribution.finalPoints - startContribution.finalPoints
                val factorConfidence = minOf(
                    startContribution.confidence,
                    endContribution.confidence,
                ) * periodCoverage
                if (abs(changePoints) < MIN_DISPLAY_POINTS || factorConfidence < MIN_DISPLAY_CONFIDENCE) {
                    return@mapNotNull null
                }
                RdiPeriodImpactFactor(
                    factorCode = factorCode,
                    domain = endContribution.domain,
                    changePoints = changePoints.round1(),
                    startPoints = startContribution.finalPoints.round3(),
                    endPoints = endContribution.finalPoints.round3(),
                    dataConfidence = factorConfidence.round3(),
                    latestEvidence = endContribution.evidenceText,
                )
            }.sortedByDescending { abs(it.changePoints) }
            .take(3)
        return RdiPeriodImpact(
            periodDays = periodDays,
            validDays = validDays,
            dataConfidence = confidence,
            factors = factors,
        )
    }

    private fun List<RdiContribution>.eligibleByFactor(): Map<String, RdiContribution> =
        filter { contribution ->
            contribution.confidence >= MIN_FACTOR_CONFIDENCE && contribution.finalPoints.isFinite()
        }.associateBy { it.factorCode }

    private const val MIN_FACTOR_CONFIDENCE = 0.20
    private const val MIN_DISPLAY_CONFIDENCE = 0.30
    private const val MIN_DISPLAY_POINTS = 0.05
}

private fun List<Double>.medianOrNull(): Double? {
    if (isEmpty()) return null
    val ordered = sorted()
    val middle = ordered.size / 2
    return if (ordered.size % 2 == 0) {
        (ordered[middle - 1] + ordered[middle]) / 2.0
    } else {
        ordered[middle]
    }
}

private fun Double.round1(): Double = round(this * 10.0) / 10.0
private fun Double.round3(): Double = round(this * 1_000.0) / 1_000.0
