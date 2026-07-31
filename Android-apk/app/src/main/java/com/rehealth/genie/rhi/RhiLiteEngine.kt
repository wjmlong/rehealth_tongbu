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

const val RHI_LITE_ALGORITHM_VERSION = "rhi-deterministic-preview-2.1.0-android-lite"

data class RhiLiteCalculationInput(
    val scoredOn: LocalDate,
    val zoneId: ZoneId,
    val activities: List<RingActivityEntity>,
    val sleepSessions: List<RingSleepSessionEntity>,
    val measurements: List<RingMeasurementEntity>,
    val previousDisplayScore: Double?,
    val previousConfidence: Double? = null,
    val context: RhiContextInput = RhiContextInput(),
)

data class RhiContextInput(
    val manual: RhiManualHealthInputEntity? = null,
    val profileBmi: Double? = null,
    val profileObservedAt: Long? = null,
    val age: Int? = null,
    val biologicalSex: String? = null,
    val nicotineExposure: Int? = null,
    val diabetesStatus: Int? = null,
    val antihypertensiveMedication: Int? = null,
    val lipidLoweringMedication: Int? = null,
    val prematureCvdFamilyHistory: Int? = null,
    val adherencePercent: Double? = null,
    val adherenceConfidence: Double = 0.0,
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
 * This is a transparent, local port of the RHI-100 preview curves for values
 * whose provenance can be established from Room, a confirmed clinical report,
 * or the trusted user profile. Unsupported fields remain missing (neutral score,
 * zero confidence); they are never imputed as normal.
 */
object RhiLiteEngine {
    private const val DISPLAY_ALPHA = 0.25

    private val domainWeights = linkedMapOf(
        "hemodynamic" to 0.25,
        "activity_fitness" to 0.25,
        "sleep_recovery" to 0.20,
        "metabolic_control" to 0.20,
        "behavior_adherence" to 0.10,
    )

    private val indicators = listOf(
        Indicator(
            "sbp_7d_mean",
            "hemodynamic",
            points(90 to 65, 110 to 100, 120 to 95, 130 to 75, 140 to 50, 160 to 20, 180 to 0),
            0.8,
            -1,
        ),
        Indicator(
            "dbp_7d_mean",
            "hemodynamic",
            points(50 to 65, 70 to 100, 80 to 95, 90 to 60, 100 to 25, 120 to 0),
            0.8,
            -1,
        ),
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
            "bmi",
            "metabolic_control",
            points(15 to 20, 18.5 to 80, 21 to 100, 24 to 90, 28 to 55, 32 to 25, 40 to 0),
            0.8,
            0,
        ),
        Indicator(
            "waist_circumference_cm",
            "metabolic_control",
            points(55 to 100, 75 to 100, 85 to 80, 90 to 60, 100 to 25, 120 to 0),
            0.8,
            -1,
        ),
        Indicator(
            "weight_change_28d_pct",
            "metabolic_control",
            points(-6 to 50, -3 to 90, -1 to 100, 0 to 85, 2 to 55, 5 to 20),
            0.5,
            0,
        ),
        Indicator("glycemia_value", "metabolic_control", null, 0.8, -1),
        Indicator(
            "ldl_c",
            "metabolic_control",
            points(1 to 100, 2.6 to 90, 3.4 to 65, 4.1 to 35, 5 to 0),
            0.8,
            -1,
        ),
        Indicator(
            "triglycerides",
            "metabolic_control",
            points(0.5 to 100, 1.7 to 90, 2.3 to 65, 5 to 20, 8 to 0),
            0.8,
            -1,
        ),
        Indicator(
            "hdl_c",
            "metabolic_control",
            points(0.5 to 0, 1 to 60, 1.3 to 85, 1.6 to 100),
            0.8,
            1,
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

        activityFeatures(input.scoredOn, input.zoneId, activityDays, input.context)
            .forEach { (name, value) -> features[name] = value }
        sleepFeatures(input.scoredOn, sleepDays).forEach { (name, value) -> features[name] = value }
        recoveryFeatures(input.scoredOn, nightMeasurements).forEach { (name, value) -> features[name] = value }
        bloodPressureFeatures(input).forEach { (name, value) -> features[name] = value }
        metabolicFeatures(input).forEach { (name, value) -> features[name] = value }
        nocturnalSpo2Feature(input)?.let { features["nocturnal_spo2_drop_burden_14d_pct"] = it }
        behaviorFeatures(input).forEach { (name, value) -> features[name] = value }

        val domainValues = domainWeights.keys.associateWith { mutableListOf<Double>() }
        indicators.forEach { indicator ->
            val feature = features[indicator.name]
            val absolute = feature?.absoluteScore ?: feature?.let { interpolate(it.value, indicator.points) } ?: 50.0
            val personal = feature?.personalScore(indicator.improvementDirection) ?: 50.0
            val rawIndicator =
                indicator.lambdaAbsolute * absolute + (1.0 - indicator.lambdaAbsolute) * personal
            val confidence = feature?.confidence?.coerceIn(0.0, 1.0) ?: 0.0
            domainValues.getValue(indicator.domain) +=
                (50.0 + confidence * (rawIndicator - 50.0)).coerceIn(0.0, 100.0)
        }
        val clinicalContextConfidences = clinicalContextConfidences(input)
        val confidence = (
            features.values.sumOf { it.confidence } + clinicalContextConfidences.sum()
            ) / (indicators.size + CLINICAL_CONTEXT_FIELD_COUNT)
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
            availableFeatureCount = features.size + clinicalContextConfidences.count { it > 0.0 },
            domains = domains,
        )
    }

    private fun activityFeatures(
        scoredOn: LocalDate,
        zoneId: ZoneId,
        days: Map<LocalDate, ActivityDay>,
        context: RhiContextInput,
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
        val current14 = (0L..13L).mapNotNull { days[scoredOn.minusDays(it)] }
        if (current14.isNotEmpty()) {
            result["active_day_regularity_14d_pct"] = FeatureValue(
                value = current14.count { it.steps >= 6_000 || it.exerciseMinutes >= 30 } /
                    14.0 * 100.0,
                confidence = (current14.size / 14.0 * sourceFactor).coerceIn(0.0, 1.0),
            )
        }
        context.manual?.takeIf { it.isAvailableOn(scoredOn, zoneId) }?.let { manual ->
            manual.sedentaryHoursPerDay?.let {
                result["sedentary_hours_7d_mean"] = FeatureValue(
                    value = it,
                    confidence = manual.manualConfidence(scoredOn, zoneId),
                )
            }
            manual.vo2MaxMlKgMin?.let {
                result["cardiorespiratory_fitness_score"] = FeatureValue(
                    value = vo2MaxToFitnessScore(it),
                    confidence = manual.manualConfidence(scoredOn, zoneId),
                )
            }
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

    private fun bloodPressureFeatures(input: RhiLiteCalculationInput): Map<String, FeatureValue> {
        val manual = input.context.manual?.takeIf {
                it.isAvailableOn(input.scoredOn, input.zoneId) &&
                it.cuffConfirmed &&
                it.cuffValidDays?.let { days -> days in 3..7 } == true &&
                it.cuffSbp7dMean?.let { value -> value in 70.0..250.0 } == true &&
                it.cuffDbp7dMean?.let { value -> value in 40.0..150.0 } == true &&
                it.cuffSbp7dMean!! > it.cuffDbp7dMean!!
        }
        if (manual != null) {
            val confidence = manual.manualConfidence(input.scoredOn, input.zoneId)
            return mapOf(
                "sbp_7d_mean" to FeatureValue(manual.cuffSbp7dMean!!, confidence),
                "dbp_7d_mean" to FeatureValue(manual.cuffDbp7dMean!!, confidence),
            )
        }
        // MRD ring blood-pressure rows are cuffless estimates. Without explicit
        // provenance that identifies a validated upper-arm cuff, they remain
        // display-only and must not silently become a clinical RHI input.
        return emptyMap()
    }

    private fun metabolicFeatures(input: RhiLiteCalculationInput): Map<String, FeatureValue> {
        val result = mutableMapOf<String, FeatureValue>()
        val manual = input.context.manual?.takeIf { it.isAvailableOn(input.scoredOn, input.zoneId) }
        manual?.waistCircumferenceCm?.let {
            result["waist_circumference_cm"] = FeatureValue(
                value = it,
                confidence = manual.manualConfidence(input.scoredOn, input.zoneId),
            )
        }
        val bmi = input.latestMetric(
            RingMetricType.BMI,
            valid = { it in 10.0..80.0 },
        )
        if (bmi != null) {
            result["bmi"] = bmi
        } else if (
            input.context.profileBmi?.let { it.isFinite() && it in 10.0..80.0 } == true &&
            input.context.profileObservedAt.isAvailableOn(input.scoredOn, input.zoneId)
        ) {
            result["bmi"] = FeatureValue(input.context.profileBmi!!, 0.60)
        }

        val confirmedLabConfidence = manual
            ?.takeIf { it.labConfirmed }
            ?.verifiedLabConfidence(input.scoredOn, input.zoneId)
            ?.takeIf { it > 0.0 }
        val hba1c = manual?.hba1cPercent
        if (hba1c != null) {
            result["glycemia_value"] = FeatureValue(
                value = hba1c,
                confidence = manual.manualConfidence(input.scoredOn, input.zoneId),
                absoluteScore = interpolate(
                    hba1c,
                    points(4 to 100, 5.6 to 95, 6.4 to 65, 8 to 25, 12 to 0),
                ),
            )
        } else if (confirmedLabConfidence != null && manual.fastingGlucoseMmolL != null) {
            val value = manual.fastingGlucoseMmolL
            result["glycemia_value"] = FeatureValue(
                value = value,
                confidence = confirmedLabConfidence,
                absoluteScore = interpolate(
                    value,
                    points(3.5 to 90, 5.5 to 100, 6.1 to 75, 7 to 45, 11 to 0),
                ),
            )
        } else {
            input.latestMetric(
                RingMetricType.BLOOD_GLUCOSE,
                normalize = ::normalizeGlucose,
                valid = { it in 1.0..40.0 },
                absolutePoints = points(3.5 to 90, 5.5 to 100, 6.1 to 75, 7 to 45, 11 to 0),
            )?.let { result["glycemia_value"] = it }
        }
        manual?.takeIf { confirmedLabConfidence != null }
            ?.ldlMmolL
            ?.takeIf { it in 0.1..15.0 }
            ?.let {
                result["ldl_c"] = FeatureValue(it, confirmedLabConfidence!!)
            } ?: input.latestMetric(
            RingMetricType.LDL_CHOLESTEROL,
            normalize = ::normalizeCholesterol,
            valid = { it in 0.1..15.0 },
        )?.let { result["ldl_c"] = it }
        manual?.takeIf { confirmedLabConfidence != null }
            ?.triglyceridesMmolL
            ?.takeIf { it in 0.1..30.0 }
            ?.let {
                result["triglycerides"] = FeatureValue(it, confirmedLabConfidence!!)
            } ?: input.latestMetric(
            RingMetricType.TRIGLYCERIDES,
            normalize = ::normalizeTriglycerides,
            valid = { it in 0.1..30.0 },
        )?.let { result["triglycerides"] = it }
        manual?.takeIf { confirmedLabConfidence != null }
            ?.hdlMmolL
            ?.takeIf { it in 0.1..10.0 }
            ?.let {
                result["hdl_c"] = FeatureValue(it, confirmedLabConfidence!!)
            } ?: input.latestMetric(
            RingMetricType.HDL_CHOLESTEROL,
            normalize = ::normalizeCholesterol,
            valid = { it in 0.1..10.0 },
        )?.let { result["hdl_c"] = it }
        weightChangeFeature(input)?.let { result["weight_change_28d_pct"] = it }
        return result
    }

    private fun nocturnalSpo2Feature(input: RhiLiteCalculationInput): FeatureValue? {
        val end = input.scoredOn.endExclusive(input.zoneId)
        val start = input.scoredOn.minusDays(13).atStartOfDay(input.zoneId).toInstant().toEpochMilli()
        val sleepIntervals = input.sleepSessions.filter { it.endedAt > it.startedAt }
        val rows = input.measurements.filter { measurement ->
            measurement.metricType.equals(RingMetricType.BLOOD_OXYGEN.name, true) &&
                measurement.primaryValue in 50.0..100.0 &&
                measurement.measuredAt in start until end &&
                sleepIntervals.any { measurement.measuredAt in it.startedAt..it.endedAt }
        }
        val nights = rows.map { it.measuredAt.toDate(input.zoneId) }.distinct().size
        if (rows.size < 5 || nights < 2) return null
        val burden = rows.count { it.primaryValue < 90.0 } / rows.size.toDouble() * 100.0
        val sourceFactor = if (rows.map { it.source }.distinct().size <= 1) 0.80 else 0.55
        return FeatureValue(
            value = burden,
            confidence = (nights / 14.0 * sourceFactor).coerceIn(0.0, 1.0),
        )
    }

    private fun behaviorFeatures(input: RhiLiteCalculationInput): Map<String, FeatureValue> =
        buildMap {
            if (
                input.context.nicotineExposure in 0..1 &&
                input.context.profileObservedAt.isAvailableOn(input.scoredOn, input.zoneId)
            ) {
                put(
                    "nicotine_exposure",
                    FeatureValue(input.context.nicotineExposure!!.toDouble(), 0.60),
                )
            }
            input.context.adherencePercent?.takeIf { it in 0.0..100.0 }?.let {
                put(
                    "adherence_composite_28d_pct",
                    FeatureValue(it, input.context.adherenceConfidence.coerceIn(0.0, 1.0)),
                )
            }
        }

    private fun clinicalContextConfidences(input: RhiLiteCalculationInput): List<Double> {
        val profileConfidence = if (
            input.context.profileObservedAt.isAvailableOn(input.scoredOn, input.zoneId)
        ) {
            0.60
        } else {
            0.0
        }
        val manualConfidence = input.context.manual
            ?.takeIf { it.isAvailableOn(input.scoredOn, input.zoneId) }
            ?.manualConfidence(input.scoredOn, input.zoneId)
            ?: 0.0
        val manual = input.context.manual?.takeIf { it.isAvailableOn(input.scoredOn, input.zoneId) }
        val totalCholesterolConfidence = if (
            manual?.labConfirmed == true && manual.totalCholesterolMmolL != null
        ) {
            manual.verifiedLabConfidence(input.scoredOn, input.zoneId)
        } else {
            input.latestMetric(
                RingMetricType.TOTAL_CHOLESTEROL,
                normalize = ::normalizeCholesterol,
                valid = { it in 0.1..20.0 },
            )?.confidence ?: 0.0
        }
        return listOf(
            profileConfidence.takeIf { input.context.age?.let { it in 18..120 } == true } ?: 0.0,
            profileConfidence.takeIf { input.context.biologicalSex != null } ?: 0.0,
            totalCholesterolConfidence,
            manualConfidence.takeIf { input.context.manual?.egfrMlMin173m2 != null } ?: 0.0,
            profileConfidence.takeIf { input.context.diabetesStatus?.let { it in 0..1 } == true } ?: 0.0,
            profileConfidence.takeIf {
                input.context.antihypertensiveMedication?.let { it in 0..1 } == true
            } ?: 0.0,
            profileConfidence.takeIf {
                input.context.lipidLoweringMedication?.let { it in 0..1 } == true
            } ?: 0.0,
            profileConfidence.takeIf {
                input.context.prematureCvdFamilyHistory?.let { it in 0..1 } == true
            } ?: 0.0,
        )
    }

    private fun weightChangeFeature(input: RhiLiteCalculationInput): FeatureValue? {
        val pairedWeights = input.measurements
            .filter {
                it.metricType.equals(RingMetricType.FAT_MASS.name, true) ||
                    it.metricType.equals(RingMetricType.FAT_FREE_MASS.name, true)
            }
            .groupBy { it.measuredAt }
            .mapNotNull { (measuredAt, rows) ->
                val fat = rows.firstOrNull { it.metricType.equals(RingMetricType.FAT_MASS.name, true) }
                    ?.primaryValue
                val lean = rows.firstOrNull { it.metricType.equals(RingMetricType.FAT_FREE_MASS.name, true) }
                    ?.primaryValue
                val weight = if (fat != null && lean != null) fat + lean else null
                weight?.takeIf { it in 20.0..350.0 }?.let { measuredAt to it }
            }
        val currentStart = input.scoredOn.minusDays(13).atStartOfDay(input.zoneId).toInstant().toEpochMilli()
        val baselineStart = input.scoredOn.minusDays(41).atStartOfDay(input.zoneId).toInstant().toEpochMilli()
        val baselineEnd = input.scoredOn.minusDays(14).endExclusive(input.zoneId)
        val current = pairedWeights.filter { it.first >= currentStart }.maxByOrNull { it.first } ?: return null
        val baseline = pairedWeights.filter { it.first in baselineStart until baselineEnd }
            .maxByOrNull { it.first } ?: return null
        val change = (current.second - baseline.second) / baseline.second * 100.0
        return FeatureValue(
            value = change.coerceIn(-50.0, 100.0),
            confidence = 0.80,
        )
    }

    private fun RhiLiteCalculationInput.latestMetric(
        type: RingMetricType,
        normalize: (Double, String) -> Double = { value, _ -> value },
        valid: (Double) -> Boolean,
        absolutePoints: List<Pair<Double, Double>>? = null,
    ): FeatureValue? {
        val end = scoredOn.endExclusive(zoneId)
        val row = measurements.asSequence()
            .filter { it.metricType.equals(type.name, true) && it.measuredAt < end }
            .sortedByDescending { it.measuredAt }
            .firstOrNull { valid(normalize(it.primaryValue, it.unit)) }
            ?: return null
        val value = normalize(row.primaryValue, row.unit)
        val ageDays = ((end - row.measuredAt).coerceAtLeast(0L) / MILLIS_PER_DAY).toInt()
        val freshness = when {
            ageDays <= 90 -> 0.85
            ageDays <= 180 -> 0.65
            ageDays <= 365 -> 0.40
            else -> 0.20
        }
        return FeatureValue(
            value = value,
            confidence = freshness,
            absoluteScore = absolutePoints?.let { interpolate(value, it) },
        )
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

    private fun points(vararg values: Pair<Number, Number>): List<Pair<Double, Double>> =
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
    val absoluteScore: Double? = null,
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

private fun LocalDate.endExclusive(zoneId: ZoneId): Long =
    plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

private fun Long?.isAvailableOn(scoredOn: LocalDate, zoneId: ZoneId): Boolean =
    this != null && this < scoredOn.endExclusive(zoneId)

private fun RhiManualHealthInputEntity.isAvailableOn(scoredOn: LocalDate, zoneId: ZoneId): Boolean =
    updatedAt < scoredOn.endExclusive(zoneId)

private fun RhiManualHealthInputEntity.manualConfidence(scoredOn: LocalDate, zoneId: ZoneId): Double {
    val ageDays = ((scoredOn.endExclusive(zoneId) - updatedAt).coerceAtLeast(0L) / MILLIS_PER_DAY).toInt()
    return when {
        ageDays <= 90 -> 0.80
        ageDays <= 180 -> 0.60
        ageDays <= 365 -> 0.40
        else -> 0.20
    }
}

private fun RhiManualHealthInputEntity.verifiedLabConfidence(
    scoredOn: LocalDate,
    zoneId: ZoneId,
): Double {
    if (!labConfirmed) return 0.0
    val observedAt = labRecordedAt ?: return 0.0
    val end = scoredOn.endExclusive(zoneId)
    if (observedAt >= end) return 0.0
    val ageDays = ((end - observedAt).coerceAtLeast(0L) / MILLIS_PER_DAY).toInt()
    return when {
        ageDays <= 90 -> 0.90
        ageDays <= 180 -> 0.65
        ageDays <= 365 -> 0.35
        else -> 0.15
    }
}

private fun normalizeGlucose(value: Double, unit: String): Double =
    if (unit.normalizedUnit().contains("mg/dl")) value / 18.0 else value

private fun normalizeCholesterol(value: Double, unit: String): Double =
    if (unit.normalizedUnit().contains("mg/dl")) value / 38.67 else value

private fun normalizeTriglycerides(value: Double, unit: String): Double =
    if (unit.normalizedUnit().contains("mg/dl")) value / 88.57 else value

private fun String.normalizedUnit(): String = lowercase().replace(" ", "")

private fun vo2MaxToFitnessScore(vo2Max: Double): Double =
    interpolatePreview(
        vo2Max,
        listOf(5.0 to 0.0, 15.0 to 10.0, 25.0 to 40.0, 35.0 to 70.0, 45.0 to 90.0, 60.0 to 100.0),
    )

private fun interpolatePreview(value: Double, curve: List<Pair<Double, Double>>): Double {
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

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
private const val CLINICAL_CONTEXT_FIELD_COUNT = 8
