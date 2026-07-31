package com.rehealth.genie.rhi

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tanh

const val RHI_LITE_ALGORITHM_VERSION = "rhi-deterministic-preview-2.0.0-android-lite"

data class RhiLiteCalculationInput(
    val scoredOn: LocalDate,
    val zoneId: ZoneId,
    val activities: List<RingActivityEntity>,
    val sleepSessions: List<RingSleepSessionEntity>,
    val measurements: List<RingMeasurementEntity>,
    val previousDisplayScore: Double?,
    val previousConfidence: Double? = null,
)

data class RhiLiteCalculation(
    val rawScore: Double,
    val displayScore: Double,
    val confidence: Double,
    val availableDays: Int,
    val availableFeatureCount: Int,
    val domains: Map<String, Double?>,
)

/**
 * Android RHI Lite evaluator.
 *
 * This is a transparent, local port of the RHI-100 preview curves for wearable
 * fields whose meaning can be derived safely from Room. Unsupported Lite fields
 * remain missing (neutral score, zero confidence); they are never imputed as normal.
 */
object RhiLiteEngine {
    private const val DISPLAY_ALPHA = 0.25

    private val domainWeights = linkedMapOf(
        "hemodynamic" to 0.25,
        "activity_fitness" to 0.25,
        "sleep_recovery" to 0.20,
        "behavior_adherence" to 0.10,
    )

    private val indicators = listOf(
        Indicator(
            "resting_hr_14d_median",
            "hemodynamic",
            points(40 to 60, 55 to 95, 70 to 100, 80 to 80, 90 to 50, 110 to 10),
            0.5,
            -1,
        ),
        Indicator("nocturnal_hrv_14d_median", "hemodynamic", null, 0.0, 1),
        Indicator(
            "resting_hr_change_28d_pct",
            "hemodynamic",
            points(-15 to 100, -5 to 80, 0 to 50, 5 to 25, 15 to 0),
            0.5,
            -1,
        ),
        Indicator(
            "hrv_change_28d_pct",
            "hemodynamic",
            points(-30 to 0, -10 to 25, 0 to 50, 10 to 75, 30 to 100),
            0.5,
            1,
        ),
        Indicator(
            "steps_7d_mean",
            "activity_fitness",
            points(0 to 0, 3000 to 35, 5000 to 55, 7000 to 75, 10000 to 100),
            0.5,
            1,
        ),
        Indicator(
            "mvpa_minutes_7d",
            "activity_fitness",
            points(0 to 0, 75 to 45, 150 to 85, 300 to 100),
            0.5,
            1,
        ),
        Indicator(
            "sedentary_hours_7d_mean",
            "activity_fitness",
            points(4 to 100, 6 to 85, 8 to 60, 10 to 30, 14 to 0),
            0.5,
            -1,
        ),
        Indicator(
            "active_day_regularity_14d_pct",
            "activity_fitness",
            points(0 to 0, 40 to 40, 70 to 75, 90 to 100),
            0.5,
            1,
        ),
        Indicator(
            "cardiorespiratory_fitness_score",
            "activity_fitness",
            points(0 to 0, 50 to 50, 80 to 80, 100 to 100),
            0.5,
            1,
        ),
        Indicator(
            "sleep_duration_7d_mean_hours",
            "sleep_recovery",
            points(3 to 0, 5 to 35, 6 to 70, 7 to 100, 9 to 100, 10 to 70, 12 to 20),
            0.5,
            0,
        ),
        Indicator(
            "sleep_regularity_14d_pct",
            "sleep_recovery",
            points(0 to 0, 50 to 45, 75 to 80, 90 to 100),
            0.5,
            1,
        ),
        Indicator(
            "sleep_efficiency_14d_pct",
            "sleep_recovery",
            points(50 to 0, 75 to 50, 85 to 85, 95 to 100),
            0.5,
            1,
        ),
        Indicator(
            "nocturnal_spo2_drop_burden_14d_pct",
            "sleep_recovery",
            points(0 to 100, 2 to 90, 5 to 65, 10 to 30, 20 to 0),
            0.5,
            -1,
        ),
        Indicator(
            "nicotine_exposure",
            "behavior_adherence",
            points(0 to 100, 1 to 0),
            1.0,
            -1,
        ),
        Indicator(
            "adherence_composite_28d_pct",
            "behavior_adherence",
            points(0 to 0, 50 to 50, 80 to 80, 100 to 100),
            1.0,
            1,
        ),
    )

    fun calculate(input: RhiLiteCalculationInput): RhiLiteCalculation {
        val activityDays = input.activities.toActivityDays(input.zoneId)
        val sleepDays = input.sleepSessions.toSleepDays(input.zoneId)
        val nightMeasurements = input.measurements.toNightMeasurements(input.sleepSessions, input.zoneId)
        val features = mutableMapOf<String, FeatureValue>()

        activityFeatures(input.scoredOn, activityDays).forEach { (name, value) -> features[name] = value }
        sleepFeatures(input.scoredOn, sleepDays).forEach { (name, value) -> features[name] = value }
        recoveryFeatures(input.scoredOn, nightMeasurements).forEach { (name, value) -> features[name] = value }

        val domainValues = domainWeights.keys.associateWith { mutableListOf<Double>() }
        indicators.forEach { indicator ->
            val feature = features[indicator.name]
            val absolute = feature?.let { interpolate(it.value, indicator.points) } ?: 50.0
            val personal = feature?.personalScore(indicator.improvementDirection) ?: 50.0
            val rawIndicator =
                indicator.lambdaAbsolute * absolute + (1.0 - indicator.lambdaAbsolute) * personal
            val confidence = feature?.confidence?.coerceIn(0.0, 1.0) ?: 0.0
            domainValues.getValue(indicator.domain) +=
                (50.0 + confidence * (rawIndicator - 50.0)).coerceIn(0.0, 100.0)
        }
        val confidence = features.values.sumOf { it.confidence } / indicators.size
        val domains = domainValues.mapValues { (_, values) ->
            values.takeIf { it.isNotEmpty() }?.average()?.round1()
        }
        val applicable = domainWeights.filterKeys { domains[it] != null }
        val weightTotal = applicable.values.sum()
        val rawScore = applicable.entries.sumOf { (domain, weight) ->
            domains.getValue(domain)!! * weight
        } / weightTotal
        val smoothedDisplay = input.previousDisplayScore?.let { previous ->
            DISPLAY_ALPHA * rawScore + (1.0 - DISPLAY_ALPHA) * previous
        } ?: rawScore
        val displayScore = if (
            input.previousDisplayScore != null &&
            input.previousConfidence != null &&
            confidence < input.previousConfidence &&
            smoothedDisplay > input.previousDisplayScore
        ) {
            input.previousDisplayScore
        } else {
            smoothedDisplay
        }
        val currentDates = buildSet {
            addAll(activityDays.keys)
            addAll(sleepDays.keys)
            addAll(nightMeasurements.values.flatten().map { it.date })
        }.count { it in input.scoredOn.minusDays(6)..input.scoredOn }
        return RhiLiteCalculation(
            rawScore = rawScore.coerceIn(0.0, 100.0).round1(),
            displayScore = displayScore.coerceIn(0.0, 100.0).round1(),
            confidence = confidence.round3(),
            availableDays = currentDates,
            availableFeatureCount = features.size,
            domains = domains,
        )
    }

    private fun activityFeatures(
        scoredOn: LocalDate,
        days: Map<LocalDate, ActivityDay>,
    ): Map<String, FeatureValue> {
        val current = (0L..6L).mapNotNull { days[scoredOn.minusDays(it)] }
        if (current.isEmpty()) return emptyMap()
        val currentSources = current.map { it.source }.distinct()
        val sourceFactor = if (currentSources.size == 1) 0.95 else 0.60
        val coverage = current.size / 7.0
        val baseline = (7L..34L).mapNotNull { days[scoredOn.minusDays(it)] }
        val result = mutableMapOf<String, FeatureValue>()
        result["steps_7d_mean"] = FeatureValue(
            value = current.map { it.steps.toDouble() }.average(),
            confidence = coverage * sourceFactor,
            baselineSamples = baseline.map { it.steps.toDouble() },
        )
        if (current.any { it.exerciseMinutes > 0 }) {
            result["mvpa_minutes_7d"] = FeatureValue(
                value = current.sumOf { it.exerciseMinutes }.toDouble(),
                confidence = coverage * sourceFactor,
                baselineSamples = baseline.map { it.exerciseMinutes * 7.0 },
            )
        }
        return result
    }

    private fun sleepFeatures(
        scoredOn: LocalDate,
        days: Map<LocalDate, SleepDay>,
    ): Map<String, FeatureValue> {
        val current7 = (0L..6L).mapNotNull { days[scoredOn.minusDays(it)] }
        if (current7.isEmpty()) return emptyMap()
        val current14 = (0L..13L).mapNotNull { days[scoredOn.minusDays(it)] }
        val baseline = (14L..41L).mapNotNull { days[scoredOn.minusDays(it)] }
        val sourceFactor = if ((current14 + baseline).map { it.source }.distinct().size <= 1) 0.95 else 0.60
        val result = mutableMapOf<String, FeatureValue>()
        result["sleep_duration_7d_mean_hours"] = FeatureValue(
            value = current7.map { it.durationMinutes / 60.0 }.average(),
            confidence = current7.size / 7.0 * sourceFactor,
            baselineSamples = baseline.map { it.durationMinutes / 60.0 },
        )
        if (current14.size >= 3) {
            val bedtimeDeviation = current14.map { it.bedtimeMinute.toDouble() }.circularStandardDeviation()
            result["sleep_regularity_14d_pct"] = FeatureValue(
                value = (100.0 - bedtimeDeviation / 180.0 * 100.0).coerceIn(0.0, 100.0),
                confidence = current14.size / 14.0 * sourceFactor,
            )
            result["sleep_efficiency_14d_pct"] = FeatureValue(
                value = current14.map { it.efficiency }.average(),
                confidence = current14.size / 14.0 * sourceFactor,
                baselineSamples = baseline.map { it.efficiency },
            )
        }
        return result
    }

    private fun recoveryFeatures(
        scoredOn: LocalDate,
        byMetric: Map<String, List<NightMeasurement>>,
    ): Map<String, FeatureValue> {
        val result = mutableMapOf<String, FeatureValue>()
        val restingHr = byMetric[RingMetricType.HEART_RATE.name].orEmpty()
        addRecoveryPair(
            result = result,
            scoredOn = scoredOn,
            records = restingHr,
            medianName = "resting_hr_14d_median",
            changeName = "resting_hr_change_28d_pct",
        )
        val hrv = byMetric[RingMetricType.HRV.name].orEmpty()
        addRecoveryPair(
            result = result,
            scoredOn = scoredOn,
            records = hrv,
            medianName = "nocturnal_hrv_14d_median",
            changeName = "hrv_change_28d_pct",
        )
        return result
    }

    private fun addRecoveryPair(
        result: MutableMap<String, FeatureValue>,
        scoredOn: LocalDate,
        records: List<NightMeasurement>,
        medianName: String,
        changeName: String,
    ) {
        val daily = records.groupBy { it.date }.mapValues { (_, values) ->
            NightValue(
                value = values.map { it.value }.median(),
                source = values.maxBy { it.measuredAt }.source,
                quality = values.map { it.quality }.average(),
            )
        }
        val current = (0L..13L).mapNotNull { daily[scoredOn.minusDays(it)] }
        val baseline = (14L..41L).mapNotNull { daily[scoredOn.minusDays(it)] }
        if (current.size < 5 || (current + baseline).map { it.source }.distinct().size != 1) return
        val currentMedian = current.map { it.value }.median()
        val confidence = (current.size / 14.0 * 0.95 * current.map { it.quality }.average())
            .coerceIn(0.0, 1.0)
        result[medianName] = FeatureValue(
            value = currentMedian,
            confidence = confidence,
            baselineSamples = baseline.map { it.value },
        )
        if (baseline.size >= 5) {
            val baselineMedian = baseline.map { it.value }.median().takeIf { it > 0.0 } ?: return
            result[changeName] = FeatureValue(
                value = (currentMedian - baselineMedian) / baselineMedian * 100.0,
                confidence = confidence * (baseline.size / 14.0).coerceIn(0.0, 1.0),
            )
        }
    }

    private fun FeatureValue.personalScore(direction: Int): Double {
        if (direction == 0 || baselineSamples.size < 7) return 50.0
        val median = baselineSamples.median()
        val mad = baselineSamples.map { kotlin.math.abs(it - median) }.median()
        val z = direction * (value - median) / (1.4826 * mad + 1e-6)
        return (50.0 + 50.0 * tanh(z / 2.0)).coerceIn(0.0, 100.0)
    }

    private fun interpolate(value: Double, curve: List<Pair<Double, Double>>?): Double {
        if (curve == null) return 50.0
        if (value <= curve.first().first) return curve.first().second
        if (value >= curve.last().first) return curve.last().second
        curve.zipWithNext().forEach { (lower, upper) ->
            if (value <= upper.first) {
                val fraction = (value - lower.first) / (upper.first - lower.first)
                return lower.second + fraction * (upper.second - lower.second)
            }
        }
        return curve.last().second
    }

    private fun points(vararg values: Pair<Int, Int>): List<Pair<Double, Double>> =
        values.map { it.first.toDouble() to it.second.toDouble() }
}

private data class Indicator(
    val name: String,
    val domain: String,
    val points: List<Pair<Double, Double>>?,
    val lambdaAbsolute: Double,
    val improvementDirection: Int,
)

private data class FeatureValue(
    val value: Double,
    val confidence: Double,
    val baselineSamples: List<Double> = emptyList(),
)

private data class ActivityDay(
    val steps: Int,
    val exerciseMinutes: Int,
    val source: String,
)

private data class SleepDay(
    val durationMinutes: Int,
    val bedtimeMinute: Int,
    val efficiency: Double,
    val source: String,
)

private data class NightMeasurement(
    val metricType: String,
    val date: LocalDate,
    val measuredAt: Long,
    val value: Double,
    val quality: Double,
    val source: String,
)

private data class NightValue(
    val value: Double,
    val source: String,
    val quality: Double,
)

private fun List<RingActivityEntity>.toActivityDays(zoneId: ZoneId): Map<LocalDate, ActivityDay> =
    groupBy { it.startedAt.toDate(zoneId) }.mapValues { (_, records) ->
        val aggregates = records.filter { it.activityType.isDailyAggregate() }
        val sessions = records.filterNot { it.activityType.isDailyAggregate() }
        ActivityDay(
            steps = maxOf(
                aggregates.maxOfOrNull { it.steps.coerceAtLeast(0) } ?: 0,
                sessions.sumOf { it.steps.coerceAtLeast(0) },
            ),
            exerciseMinutes = sessions.filter { it.activityType.isExerciseSession() }
                .sumOf { it.durationMinutes.coerceAtLeast(0) },
            source = records.maxBy { it.startedAt }.source,
        )
    }

private fun List<RingSleepSessionEntity>.toSleepDays(zoneId: ZoneId): Map<LocalDate, SleepDay> =
    mapNotNull { session ->
        val duration = ((session.endedAt - session.startedAt) / 60_000L).toInt()
        if (duration !in 120..900) return@mapNotNull null
        val asleep = (session.deepMinutes + session.lightMinutes + session.remMinutes).takeIf { it > 0 }
            ?: (duration - session.awakeMinutes).coerceAtLeast(0)
        session.endedAt.toDate(zoneId) to SleepDay(
            durationMinutes = duration,
            bedtimeMinute = session.startedAt.bedtimeMinute(zoneId),
            efficiency = (asleep.toDouble() / duration * 100.0).coerceIn(0.0, 100.0),
            source = session.source,
        )
    }.groupBy({ it.first }, { it.second }).mapValues { (_, values) -> values.maxBy { it.durationMinutes } }

private fun List<RingMeasurementEntity>.toNightMeasurements(
    sleepSessions: List<RingSleepSessionEntity>,
    zoneId: ZoneId,
): Map<String, List<NightMeasurement>> {
    val intervals = sleepSessions.filter { it.endedAt > it.startedAt }
    return asSequence()
        .filter {
            it.metricType.equals(RingMetricType.HEART_RATE.name, true) ||
                it.metricType.equals(RingMetricType.HRV.name, true)
        }
        .filter { measurement ->
            intervals.any { measurement.measuredAt in it.startedAt..it.endedAt }
        }
        .filter { it.primaryValue.isFinite() && it.primaryValue > 0.0 }
        .map {
            NightMeasurement(
                metricType = it.metricType.uppercase(),
                date = it.measuredAt.toDate(zoneId),
                measuredAt = it.measuredAt,
                value = it.primaryValue,
                quality = (it.quality?.toDouble()?.let { quality ->
                    if (quality > 1.0) quality / 100.0 else quality
                } ?: 0.75).coerceIn(0.0, 1.0),
                source = it.source,
            )
        }
        .groupBy { it.metricType }
}

private fun Long.toDate(zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

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

private fun List<Double>.median(): Double {
    val values = sorted()
    val middle = values.size / 2
    return if (values.size % 2 == 0) {
        (values[middle - 1] + values[middle]) / 2.0
    } else {
        values[middle]
    }
}

private fun List<Double>.circularStandardDeviation(): Double {
    if (size < 2) return 0.0
    val mean = average()
    return sqrt(sumOf { (it - mean).pow(2) } / size)
}

private fun Double.round1(): Double = kotlin.math.round(this * 10.0) / 10.0
private fun Double.round3(): Double = kotlin.math.round(this * 1_000.0) / 1_000.0
