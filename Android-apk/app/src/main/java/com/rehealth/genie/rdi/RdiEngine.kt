package com.rehealth.genie.rdi

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

const val RDI_ALGORITHM_VERSION = "rdi-rule-1.0.0"

data class RdiCalculationInput(
    val scoredOn: LocalDate,
    val zoneId: ZoneId,
    val activities: List<RingActivityEntity>,
    val sleepSessions: List<RingSleepSessionEntity>,
    val measurements: List<RingMeasurementEntity>,
    val previousDisplayScore: Double?,
)

data class RdiContribution(
    val factorCode: String,
    val domain: String,
    val source: String,
    val currentValue: Double,
    val baselineValue: Double?,
    val unit: String,
    val rawPoints: Double,
    val confidence: Double,
    val finalPoints: Double,
    val evidenceText: String,
    val sourceFactorId: String,
)

data class RdiCalculation(
    val rawScore: Double,
    val displayScore: Double,
    val confidence: Double,
    val status: String,
    val contributions: List<RdiContribution>,
)

object RdiEngine {
    fun calculate(input: RdiCalculationInput): RdiCalculation {
        val activity = clampDomain(activityContributions(input), 4.0)
        val sleep = clampDomain(sleepContributions(input), 4.0)
        val recovery = clampDomain(recoveryContributions(input), 4.0)
        val contributions = activity + sleep + recovery
        val rawScore = (50.0 + contributions.sumOf { it.finalPoints }).coerceIn(0.0, 100.0)
        val activityConfidence = activity.maxOfOrNull { it.confidence } ?: 0.0
        val sleepConfidence = sleep.maxOfOrNull { it.confidence } ?: 0.0
        val recoveryConfidence = recovery.maxOfOrNull { it.confidence } ?: 0.0
        val confidence = (
            0.45 * activityConfidence +
                0.45 * sleepConfidence +
                0.10 * recoveryConfidence
            ).coerceIn(0.0, 1.0)
        val target = if (confidence < 0.40) {
            50.0 + confidence * (rawScore - 50.0)
        } else {
            rawScore
        }
        val display = when {
            input.previousDisplayScore == null -> target
            confidence < 0.35 -> input.previousDisplayScore
            else -> {
                val smoothed = 0.30 * target + 0.70 * input.previousDisplayScore
                smoothed.coerceIn(input.previousDisplayScore - 3.0, input.previousDisplayScore + 3.0)
            }
        }.coerceIn(0.0, 100.0)
        val status = when {
            confidence >= 0.75 -> "confirmed"
            confidence >= 0.40 -> "provisional"
            else -> "accumulating"
        }
        return RdiCalculation(
            rawScore = rawScore.round2(),
            displayScore = display.round2(),
            confidence = confidence.round3(),
            status = status,
            contributions = contributions.sortedByDescending { abs(it.finalPoints) },
        )
    }

    private fun activityContributions(input: RdiCalculationInput): List<RdiContribution> {
        val daily = input.activities.groupBy { it.startedAt.toDate(input.zoneId) }
            .mapValues { (_, records) ->
                val dailyAggregates = records.filter { it.activityType.isDailyAggregate() }
                val sessions = records.filterNot { it.activityType.isDailyAggregate() }
                val aggregateSteps = dailyAggregates.maxOfOrNull { it.steps } ?: 0
                val sessionSteps = sessions.sumOf { it.steps.coerceAtLeast(0) }
                val verifiedMinutes = sessions.filter { it.activityType.isExerciseSession() }
                    .sumOf { it.durationMinutes.coerceAtLeast(0) }
                ActivityDay(maxOf(aggregateSteps, sessionSteps), verifiedMinutes)
            }
        val currentDates = (0L..6L).map { input.scoredOn.minusDays(it) }
        val baselineDates = (7L..27L).map { input.scoredOn.minusDays(it) }
        val currentDays = currentDates.mapNotNull(daily::get)
        if (currentDays.isEmpty()) return emptyList()
        val baselineDays = baselineDates.mapNotNull(daily::get)
        val currentSteps = currentDays.map { it.steps.toDouble() }.average()
        val baselineSteps = baselineDays.map { it.steps.toDouble() }.medianOrNull()
        var rawStepPoints = baselineSteps?.let { -0.35 * (currentSteps - it) / 1_000.0 } ?: 0.0
        if (currentSteps < 2_000.0) rawStepPoints = maxOf(rawStepPoints, 1.0)
        val currentCoverage = (currentDays.size / 7.0).coerceIn(0.0, 1.0)
        val baselineCoverage = if (baselineSteps == null) 0.55 else (baselineDays.size / 14.0).coerceIn(0.55, 1.0)
        val sourcePenalty = if (input.activities.map { it.source }.distinct().size > 1) 0.80 else 1.0
        val stepConfidence = (currentCoverage * baselineCoverage * sourcePenalty).coerceIn(0.0, 1.0)
        val result = mutableListOf(
            contribution(
                factorCode = "steps",
                domain = "activity",
                source = "ROOM_WEARABLE",
                currentValue = currentSteps,
                baselineValue = baselineSteps,
                unit = "steps/day",
                rawPoints = rawStepPoints,
                confidence = stepConfidence,
                evidence = if (baselineSteps == null) {
                    "近7日日均${currentSteps.toInt()}步；个人基线仍在建立"
                } else {
                    "近7日日均${currentSteps.toInt()}步，个人基线${baselineSteps.toInt()}步"
                },
                sourceFactorId = "wearable:steps:${input.scoredOn}",
            ),
        )

        val currentMinutes = currentDays.sumOf { it.exerciseMinutes }.toDouble()
        val baselineMinutes = baselineDays.chunked(7).map { week -> week.sumOf { it.exerciseMinutes }.toDouble() }
            .medianOrNull()
        if (currentDays.any { it.exerciseMinutes > 0 }) {
            var rawMinutes = baselineMinutes?.let { -0.50 * (currentMinutes - it) / 30.0 } ?: 0.0
            if (currentMinutes < 30.0) rawMinutes = maxOf(rawMinutes, 0.5)
            if (currentSteps >= 6_000.0 && currentMinutes >= 150.0) rawMinutes -= 0.5
            result += contribution(
                factorCode = "verified_activity_minutes",
                domain = "activity",
                source = "ROOM_WEARABLE",
                currentValue = currentMinutes,
                baselineValue = baselineMinutes,
                unit = "min/7d",
                rawPoints = rawMinutes,
                confidence = (currentCoverage * sourcePenalty).coerceIn(0.0, 1.0),
                evidence = "近7天记录到${currentMinutes.toInt()}分钟明确运动；未将普通活动冒充中高强度运动",
                sourceFactorId = "wearable:activity_minutes:${input.scoredOn}",
            )
        }
        return result
    }

    private fun sleepContributions(input: RdiCalculationInput): List<RdiContribution> {
        val days = input.sleepSessions.mapNotNull { session ->
            val durationMinutes = ((session.endedAt - session.startedAt) / 60_000L).toInt()
            if (durationMinutes !in 120..900) return@mapNotNull null
            val asleep = (session.deepMinutes + session.lightMinutes + session.remMinutes).takeIf { it > 0 }
                ?: (durationMinutes - session.awakeMinutes).coerceAtLeast(0)
            SleepDay(
                date = session.endedAt.toDate(input.zoneId),
                durationMinutes = durationMinutes,
                bedtimeMinute = session.startedAt.bedtimeMinute(input.zoneId),
                efficiency = (asleep.toDouble() / durationMinutes * 100.0).coerceIn(0.0, 100.0),
                source = session.source,
            )
        }.groupBy { it.date }.mapValues { (_, values) -> values.maxBy { it.durationMinutes } }
        val current = (0L..6L).mapNotNull { days[input.scoredOn.minusDays(it)] }
        if (current.isEmpty()) return emptyList()
        val baseline = (7L..27L).mapNotNull { days[input.scoredOn.minusDays(it)] }
        val coverage = (current.size / 7.0).coerceIn(0.0, 1.0)
        val sourcePenalty = if ((current + baseline).map { it.source }.distinct().size > 1) 0.65 else 1.0
        val confidence = (coverage * sourcePenalty).coerceIn(0.0, 1.0)
        val duration = current.map { it.durationMinutes.toDouble() }.average()
        val durationRaw = when {
            duration < 420.0 -> 0.40 * ((420.0 - duration) / 30.0)
            duration > 540.0 -> 0.20 * ((duration - 540.0) / 30.0)
            else -> 0.0
        }
        val result = mutableListOf(
            contribution(
                "sleep_duration",
                "sleep",
                "ROOM_WEARABLE",
                duration,
                baseline.map { it.durationMinutes.toDouble() }.medianOrNull(),
                "min/night",
                durationRaw,
                confidence,
                "近7日平均睡眠${(duration / 60.0).round2()}小时",
                "wearable:sleep_duration:${input.scoredOn}",
            ),
        )
        if (current.size >= 3) {
            val regularity = current.map { it.bedtimeMinute.toDouble() }.standardDeviation()
            val baselineRegularity = baseline.map { it.bedtimeMinute.toDouble() }
                .takeIf { it.size >= 5 }?.standardDeviation()
            val raw = baselineRegularity?.let { 0.40 * (regularity - it) / 30.0 } ?: 0.0
            result += contribution(
                "sleep_regularity",
                "sleep",
                "ROOM_WEARABLE",
                regularity,
                baselineRegularity,
                "min_sd",
                raw,
                confidence,
                "近7日入睡时间波动约${regularity.toInt()}分钟",
                "wearable:sleep_regularity:${input.scoredOn}",
            )
        }
        val efficiency = current.map { it.efficiency }.average()
        val baselineEfficiency = baseline.map { it.efficiency }.medianOrNull()
        val efficiencyRaw = baselineEfficiency?.let { -0.30 * (efficiency - it) / 5.0 } ?: 0.0
        result += contribution(
            "sleep_efficiency",
            "sleep",
            "ROOM_WEARABLE",
            efficiency,
            baselineEfficiency,
            "%",
            efficiencyRaw,
            confidence,
            "近7日平均睡眠效率${efficiency.toInt()}%",
            "wearable:sleep_efficiency:${input.scoredOn}",
        )
        if (current.size >= 5 && duration in 420.0..540.0 &&
            current.map { it.bedtimeMinute.toDouble() }.standardDeviation() < 45.0 &&
            efficiency >= 85.0
        ) {
            result += contribution(
                "sleep_consistency_reward",
                "sleep",
                "ROOM_WEARABLE",
                current.size.toDouble(),
                null,
                "valid_days",
                -0.5,
                confidence,
                "连续有效睡眠满足时长、规律性和效率目标",
                "wearable:sleep_reward:${input.scoredOn}",
            )
        }
        return result
    }

    private fun recoveryContributions(input: RdiCalculationInput): List<RdiContribution> {
        val hrv = input.measurements.filter { it.metricType.equals(RingMetricType.HRV.name, true) }
        val daily = hrv.groupBy { it.measuredAt.toDate(input.zoneId) }.mapValues { (_, values) ->
            MeasurementDay(
                value = values.map { it.primaryValue }.medianOrNull() ?: return@mapValues null,
                source = values.maxBy { it.measuredAt }.source,
                quality = values.mapNotNull { it.quality?.toDouble() }.averageOrNull()?.let {
                    if (it > 1.0) it / 100.0 else it
                } ?: 0.75,
            )
        }.mapNotNull { (date, value) -> value?.let { date to it } }.toMap()
        val current = (0L..6L).mapNotNull { daily[input.scoredOn.minusDays(it)] }
        val baseline = (7L..27L).mapNotNull { daily[input.scoredOn.minusDays(it)] }
        if (current.size < 5 || baseline.size < 5) return emptyList()
        if ((current + baseline).map { it.source }.distinct().size != 1) return emptyList()
        val currentValue = current.map { it.value }.medianOrNull() ?: return emptyList()
        val baselineValue = baseline.map { it.value }.medianOrNull()?.takeIf { it > 0.0 } ?: return emptyList()
        val relativeChange = (currentValue - baselineValue) / baselineValue
        val raw = -0.40 * (relativeChange / 0.10)
        val confidence = (
            (current.size / 7.0).coerceIn(0.0, 1.0) *
                current.map { it.quality }.average().coerceIn(0.0, 1.0)
            ).coerceIn(0.0, 1.0)
        return listOf(
            contribution(
                "hrv_personal_trend",
                "recovery",
                "ROOM_WEARABLE_SAME_DEVICE",
                currentValue,
                baselineValue,
                "device_unit",
                raw,
                confidence,
                "同一设备近7日HRV中位数相对个人基线变化${(relativeChange * 100.0).round2()}%",
                "wearable:hrv:${input.scoredOn}",
            ),
        )
    }

    private fun clampDomain(items: List<RdiContribution>, cap: Double): List<RdiContribution> {
        val total = items.sumOf { it.finalPoints }
        if (abs(total) <= cap || total == 0.0) return items
        val scale = cap / abs(total)
        return items.map { it.copy(finalPoints = (it.finalPoints * scale).round3()) }
    }

    private fun contribution(
        factorCode: String,
        domain: String,
        source: String,
        currentValue: Double,
        baselineValue: Double?,
        unit: String,
        rawPoints: Double,
        confidence: Double,
        evidence: String,
        sourceFactorId: String,
    ): RdiContribution {
        val q = confidence.coerceIn(0.0, 1.0)
        return RdiContribution(
            factorCode = factorCode,
            domain = domain,
            source = source,
            currentValue = currentValue.round3(),
            baselineValue = baselineValue?.round3(),
            unit = unit,
            rawPoints = rawPoints.round3(),
            confidence = q.round3(),
            finalPoints = (rawPoints * q).round3(),
            evidenceText = evidence,
            sourceFactorId = sourceFactorId,
        )
    }
}

private data class ActivityDay(val steps: Int, val exerciseMinutes: Int)
private data class SleepDay(
    val date: LocalDate,
    val durationMinutes: Int,
    val bedtimeMinute: Int,
    val efficiency: Double,
    val source: String,
)
private data class MeasurementDay(val value: Double, val source: String, val quality: Double)

private fun Long.toDate(zoneId: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

private fun Long.bedtimeMinute(zoneId: ZoneId): Int {
    val time = Instant.ofEpochMilli(this).atZone(zoneId).toLocalTime()
    return ((time.hour - 12 + 24) % 24) * 60 + time.minute
}

private fun String.isDailyAggregate(): Boolean {
    val value = lowercase()
    return value.contains("daily") || value.contains("summary") || value == "steps"
}

private fun String.isExerciseSession(): Boolean {
    val value = lowercase()
    return listOf("walk", "run", "cycle", "workout", "exercise", "swim").any(value::contains)
}

private fun List<Double>.medianOrNull(): Double? {
    if (isEmpty()) return null
    val values = sorted()
    val middle = values.size / 2
    return if (values.size % 2 == 0) (values[middle - 1] + values[middle]) / 2.0 else values[middle]
}

private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

private fun List<Double>.standardDeviation(): Double {
    if (size < 2) return 0.0
    val mean = average()
    return sqrt(sumOf { (it - mean).pow(2) } / size)
}

private fun Double.round2(): Double = kotlin.math.round(this * 100.0) / 100.0
private fun Double.round3(): Double = kotlin.math.round(this * 1_000.0) / 1_000.0
