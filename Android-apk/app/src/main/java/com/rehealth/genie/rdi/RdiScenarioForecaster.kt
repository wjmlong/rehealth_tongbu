package com.rehealth.genie.rdi

import com.rehealth.genie.features.ClinicalBloodPressureValues
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class RdiScenarioIntervention(
    val id: String? = null,
    val title: String? = null,
    val action: String? = null,
    val duration: String? = null,
    val status: String? = null,
)

data class RdiScenarioForecast(
    val noAction: List<Double>,
    val withPlan: List<Double>,
    val ciLower: List<Double>,
    val ciUpper: List<Double>,
    val d30NoAction: Double,
    val d30WithPlan: Double,
    val expectedReduction: Double,
    val horizonDays: Int = 30,
    val intervalMethod: String = INTERVAL_METHOD,
) {
    companion object {
        const val INTERVAL_METHOD = "recent-personal-variability-normal-sensitivity-95-v1"
    }
}

/**
 * RDI-native counterfactual simulator.
 *
 * Future rows are transient inputs to [RdiEngine]; they are never inserted into
 * Room as observations. The no-action arm repeats the user's recent weekday
 * pattern. The plan arm applies only explicit activity/sleep interventions.
 * The shaded interval is a deterministic sensitivity range based on recent
 * personal variability and +/-1.96 input perturbations. It is not a disease
 * probability confidence interval.
 */
object RdiScenarioForecaster {
    private const val HORIZON_DAYS = 30
    private const val MIN_HISTORY_DAYS = 7

    fun forecast(
        scoredOn: LocalDate,
        zoneId: ZoneId,
        activities: List<RingActivityEntity>,
        sleepSessions: List<RingSleepSessionEntity>,
        measurements: List<RingMeasurementEntity>,
        currentScore: Double,
        referenceDays: Int,
        anchoredBaselines: Map<String, Double>,
        bloodPressure: ClinicalBloodPressureValues?,
        confirmedLabs: List<RdiConfirmedLabEntity>,
        confirmedMeals: List<RdiConfirmedMealEntity>,
        interventions: List<RdiScenarioIntervention>,
        isMock: Boolean,
    ): RdiScenarioForecast? {
        val plan = ScenarioPlan.from(interventions) ?: return null
        val profile = RecentProfile.from(
            scoredOn = scoredOn,
            zoneId = zoneId,
            activities = activities,
            sleepSessions = sleepSessions,
            measurements = measurements,
            referenceDays = referenceDays,
        ) ?: return null
        val context = ForecastContext(
            scoredOn = scoredOn,
            zoneId = zoneId,
            historicalActivities = activities.filter { it.startedAt.toDate(zoneId) <= scoredOn },
            historicalSleep = sleepSessions.filter { it.endedAt.toDate(zoneId) <= scoredOn },
            historicalMeasurements = measurements.filter { it.measuredAt.toDate(zoneId) <= scoredOn },
            currentScore = currentScore,
            anchoredBaselines = anchoredBaselines,
            bloodPressure = bloodPressure,
            confirmedLabs = confirmedLabs,
            confirmedMeals = confirmedMeals,
            isMock = isMock,
            profile = profile,
            plan = plan,
        )
        val noActionEngine = runTrajectory(context, planStrength = 0.0, favorableZ = 0.0)
        val withPlanEngine = runTrajectory(context, planStrength = 1.0, favorableZ = 0.0)
        val optimisticEngine = runTrajectory(context, planStrength = 1.15, favorableZ = 1.96)
        val conservativeEngine = runTrajectory(context, planStrength = 0.65, favorableZ = -1.96)
        if (noActionEngine.size != HORIZON_DAYS + 1 || withPlanEngine.size != noActionEngine.size) return null
        // The selected-period RDI score is the displayed no-change reference.
        // Both counterfactual arms still run through RdiEngine; their native
        // pointwise difference is applied to this stable reference so that
        // "maintain current habits" is a horizontal comparison baseline.
        val noAction = List(noActionEngine.size) { currentScore.round2() }
        val withPlan = anchorEffects(currentScore, noActionEngine, withPlanEngine)
        val optimistic = anchorEffects(currentScore, noActionEngine, optimisticEngine)
        val conservative = anchorEffects(currentScore, noActionEngine, conservativeEngine)
        val lower = optimistic.zip(conservative).map { (a, b) -> minOf(a, b) }
        val upper = optimistic.zip(conservative).map { (a, b) -> maxOf(a, b) }
        val reduction = (noAction.last() - withPlan.last()).round1()
        return RdiScenarioForecast(
            noAction = noAction,
            withPlan = withPlan,
            ciLower = lower,
            ciUpper = upper,
            d30NoAction = noAction.last().round1(),
            d30WithPlan = withPlan.last().round1(),
            expectedReduction = reduction,
        )
    }

    private fun anchorEffects(
        referenceScore: Double,
        noActionEngine: List<Double>,
        planEngine: List<Double>,
    ): List<Double> = noActionEngine.zip(planEngine).map { (noAction, plan) ->
        (referenceScore - (noAction - plan)).coerceIn(0.0, 100.0).round2()
    }

    private fun runTrajectory(
        context: ForecastContext,
        planStrength: Double,
        favorableZ: Double,
    ): List<Double> {
        val activities = context.historicalActivities.toMutableList()
        val sleeps = context.historicalSleep.toMutableList()
        val measurements = context.historicalMeasurements.toMutableList()
        val scores = mutableListOf(context.currentScore.round2())
        var previous = context.currentScore
        (1..HORIZON_DAYS).forEach { dayIndex ->
            val date = context.scoredOn.plusDays(dayIndex.toLong())
            val projected = context.profile.project(
                date = date,
                dayIndex = dayIndex,
                plan = context.plan,
                planStrength = planStrength,
                favorableZ = favorableZ,
                zoneId = context.zoneId,
            )
            activities += projected.activity
            sleeps += projected.sleep
            measurements += projected.hrv
            val calculation = RdiEngine.calculate(
                RdiCalculationInput(
                    scoredOn = date,
                    zoneId = context.zoneId,
                    activities = activities,
                    sleepSessions = sleeps,
                    measurements = measurements,
                    previousDisplayScore = previous,
                    validDays = 28,
                    staleDays = 0,
                    anchoredBaselines = context.anchoredBaselines,
                    bloodPressure = context.bloodPressure,
                    confirmedLabs = context.confirmedLabs,
                    confirmedMeals = context.confirmedMeals,
                    dietRecords = emptyList(),
                    isMock = context.isMock,
                ),
            )
            previous = calculation.displayScore
            scores += previous.round2()
        }
        return scores
    }

    private data class ForecastContext(
        val scoredOn: LocalDate,
        val zoneId: ZoneId,
        val historicalActivities: List<RingActivityEntity>,
        val historicalSleep: List<RingSleepSessionEntity>,
        val historicalMeasurements: List<RingMeasurementEntity>,
        val currentScore: Double,
        val anchoredBaselines: Map<String, Double>,
        val bloodPressure: ClinicalBloodPressureValues?,
        val confirmedLabs: List<RdiConfirmedLabEntity>,
        val confirmedMeals: List<RdiConfirmedMealEntity>,
        val isMock: Boolean,
        val profile: RecentProfile,
        val plan: ScenarioPlan,
    )

    private data class ScenarioPlan(
        val activity: Boolean,
        val sleep: Boolean,
    ) {
        companion object {
            fun from(interventions: List<RdiScenarioIntervention>): ScenarioPlan? {
                val active = interventions.filterNot {
                    it.status?.lowercase() in setOf("completed", "cancelled", "expired")
                }
                if (active.isEmpty()) return null
                val text = active.joinToString(" ") {
                    listOfNotNull(it.id, it.title, it.action).joinToString(" ").lowercase()
                }
                val activity = listOf(
                    "walk", "walking", "exercise", "activity", "步行", "运动", "锻炼", "活动",
                ).any(text::contains)
                val sleep = listOf(
                    "sleep", "bedtime", "睡眠", "入睡", "作息", "休息", "节律",
                ).any(text::contains)
                return ScenarioPlan(activity = activity, sleep = sleep).takeIf { activity || sleep }
            }
        }
    }

    private data class ActivityPrototype(
        val date: LocalDate,
        val steps: Double,
        val exerciseMinutes: Double,
        val averageHeartRate: Double?,
    )

    private data class SleepPrototype(
        val date: LocalDate,
        val durationMinutes: Double,
        val bedtimeMinute: Double,
        val efficiency: Double,
    )

    private data class RecentProfile(
        val activityByWeekday: Map<Int, ActivityPrototype>,
        val sleepByWeekday: Map<Int, SleepPrototype>,
        val activityFallback: ActivityPrototype,
        val sleepFallback: SleepPrototype,
        val hrvMedian: Double,
        val activitySource: String,
        val sleepSource: String,
        val measurementSource: String,
        val stepsSd: Double,
        val minutesSd: Double,
        val sleepDurationSd: Double,
        val sleepEfficiencySd: Double,
        val hrvSd: Double,
        val heartRateSd: Double,
        val stableBedtimeMinute: Double,
    ) {
        fun project(
            date: LocalDate,
            dayIndex: Int,
            plan: ScenarioPlan,
            planStrength: Double,
            favorableZ: Double,
            zoneId: ZoneId,
        ): ProjectedDay {
            val activityTemplate = activityByWeekday[date.dayOfWeek.value] ?: activityFallback
            val sleepTemplate = sleepByWeekday[date.dayOfWeek.value] ?: sleepFallback
            val ramp = (dayIndex / 14.0).coerceIn(0.0, 1.0) * planStrength.coerceIn(0.0, 1.25)
            val activityRamp = if (plan.activity) ramp else 0.0
            val sleepRamp = if (plan.sleep) ramp else 0.0
            val stepsBoost = max(1_500.0, (7_000.0 - activityTemplate.steps).coerceAtLeast(0.0)) * activityRamp
            val steps = (
                activityTemplate.steps + stepsBoost + favorableZ * stepsSd * 0.35
                ).coerceIn(500.0, 20_000.0)
            val minutes = (
                activityTemplate.exerciseMinutes + 20.0 * activityRamp + favorableZ * minutesSd * 0.25
                ).coerceIn(0.0, 180.0)
            val baseHeartRate = activityTemplate.averageHeartRate ?: 92.0
            val heartRate = (
                baseHeartRate - 2.5 * max(activityRamp, sleepRamp) - favorableZ * heartRateSd * 0.20
                ).coerceIn(45.0, 190.0)
            val durationDirection = if (sleepTemplate.durationMinutes <= 480.0) 1.0 else -1.0
            val duration = (
                sleepTemplate.durationMinutes +
                    (480.0 - sleepTemplate.durationMinutes) * 0.65 * sleepRamp +
                    favorableZ * sleepDurationSd * 0.20 * durationDirection
                ).coerceIn(240.0, 660.0)
            val efficiency = (
                sleepTemplate.efficiency + (92.0 - sleepTemplate.efficiency).coerceAtLeast(0.0) * 0.65 * sleepRamp +
                    favorableZ * sleepEfficiencySd * 0.20
                ).coerceIn(55.0, 98.0)
            val bedtimeDelta = circularDelta(sleepTemplate.bedtimeMinute, stableBedtimeMinute)
            val bedtime = normalizeMinute(
                stableBedtimeMinute + bedtimeDelta * (1.0 - 0.75 * sleepRamp),
            )
            val hrv = (
                hrvMedian * (1.0 + 0.08 * max(activityRamp, sleepRamp)) + favorableZ * hrvSd * 0.25
                ).coerceIn(5.0, 250.0)
            return ProjectedDay(
                activity = activity(date, steps, minutes, heartRate, activitySource, zoneId),
                sleep = sleep(date, duration, bedtime, efficiency, sleepSource, zoneId),
                hrv = hrv(date, hrv, measurementSource, zoneId),
            )
        }

        companion object {
            fun from(
                scoredOn: LocalDate,
                zoneId: ZoneId,
                activities: List<RingActivityEntity>,
                sleepSessions: List<RingSleepSessionEntity>,
                measurements: List<RingMeasurementEntity>,
                referenceDays: Int,
            ): RecentProfile? {
                val lookbackDays = referenceDays.coerceIn(MIN_HISTORY_DAYS, 90)
                val cutoff = scoredOn.minusDays((lookbackDays - 1).toLong())
                val activityDays = activities.filter {
                    it.startedAt.toDate(zoneId) in cutoff..scoredOn
                }.groupBy { it.startedAt.toDate(zoneId) }.map { (date, records) ->
                    val daily = records.filter { it.activityType.isDailyAggregate() }
                    val sessions = records.filterNot { it.activityType.isDailyAggregate() }
                    ActivityPrototype(
                        date = date,
                        steps = maxOf(
                            daily.maxOfOrNull { it.steps } ?: 0,
                            sessions.sumOf { it.steps.coerceAtLeast(0) },
                        ).toDouble(),
                        exerciseMinutes = sessions.filter { it.activityType.isExerciseSession() }
                            .sumOf { it.durationMinutes.coerceAtLeast(0) }.toDouble(),
                        averageHeartRate = records.mapNotNull { it.averageHeartRate }.median(),
                    )
                }.sortedBy { it.date }
                val sleepDays = sleepSessions.filter {
                    it.endedAt.toDate(zoneId) in cutoff..scoredOn
                }.mapNotNull { session ->
                    val duration = ((session.endedAt - session.startedAt) / 60_000.0)
                    if (duration !in 120.0..900.0) return@mapNotNull null
                    val asleep = session.deepMinutes + session.lightMinutes + session.remMinutes
                    SleepPrototype(
                        date = session.endedAt.toDate(zoneId),
                        durationMinutes = duration,
                        bedtimeMinute = session.startedAt.bedtimeMinute(zoneId).toDouble(),
                        efficiency = if (asleep > 0) (asleep / duration * 100.0).coerceIn(0.0, 100.0) else 80.0,
                    )
                }.groupBy { it.date }.map { (_, values) -> values.maxBy { it.durationMinutes } }
                    .sortedBy { it.date }
                if (activityDays.map { it.date }.distinct().size < MIN_HISTORY_DAYS ||
                    sleepDays.map { it.date }.distinct().size < MIN_HISTORY_DAYS
                ) return null
                val hrvDays = measurements.filter {
                    it.metricType.equals(RingMetricType.HRV.name, true) &&
                        it.measuredAt.toDate(zoneId) in cutoff..scoredOn
                }.groupBy { it.measuredAt.toDate(zoneId) }
                    .values.mapNotNull { records -> records.map { it.primaryValue }.median() }
                if (hrvDays.size < MIN_HISTORY_DAYS) return null
                val heartRates = activityDays.mapNotNull { it.averageHeartRate }
                return RecentProfile(
                    activityByWeekday = activityDays.groupBy { it.date.dayOfWeek.value }.mapValues { (_, values) ->
                        ActivityPrototype(
                            date = values.maxOf { it.date },
                            steps = values.map { it.steps }.median() ?: return null,
                            exerciseMinutes = values.map { it.exerciseMinutes }.median() ?: return null,
                            averageHeartRate = values.mapNotNull { it.averageHeartRate }.median(),
                        )
                    },
                    sleepByWeekday = sleepDays.groupBy { it.date.dayOfWeek.value }.mapValues { (_, values) ->
                        SleepPrototype(
                            date = values.maxOf { it.date },
                            durationMinutes = values.map { it.durationMinutes }.median() ?: return null,
                            bedtimeMinute = circularMean(values.map { it.bedtimeMinute }),
                            efficiency = values.map { it.efficiency }.median() ?: return null,
                        )
                    },
                    activityFallback = ActivityPrototype(
                        date = scoredOn,
                        steps = activityDays.map { it.steps }.median() ?: return null,
                        exerciseMinutes = activityDays.map { it.exerciseMinutes }.median() ?: return null,
                        averageHeartRate = activityDays.mapNotNull { it.averageHeartRate }.median(),
                    ),
                    sleepFallback = SleepPrototype(
                        date = scoredOn,
                        durationMinutes = sleepDays.map { it.durationMinutes }.median() ?: return null,
                        bedtimeMinute = circularMean(sleepDays.map { it.bedtimeMinute }),
                        efficiency = sleepDays.map { it.efficiency }.median() ?: return null,
                    ),
                    hrvMedian = hrvDays.median() ?: return null,
                    activitySource = activities.maxByOrNull { it.startedAt }?.source ?: return null,
                    sleepSource = sleepSessions.maxByOrNull { it.endedAt }?.source ?: return null,
                    measurementSource = measurements.filter { it.metricType.equals(RingMetricType.HRV.name, true) }
                        .maxByOrNull { it.measuredAt }?.source ?: return null,
                    stepsSd = activityDays.map { it.steps }.standardDeviation().coerceAtLeast(350.0),
                    minutesSd = activityDays.map { it.exerciseMinutes }.standardDeviation().coerceAtLeast(4.0),
                    sleepDurationSd = sleepDays.map { it.durationMinutes }.standardDeviation().coerceAtLeast(15.0),
                    sleepEfficiencySd = sleepDays.map { it.efficiency }.standardDeviation().coerceAtLeast(1.0),
                    hrvSd = hrvDays.standardDeviation().coerceAtLeast(2.0),
                    heartRateSd = heartRates.standardDeviation().coerceAtLeast(1.0),
                    stableBedtimeMinute = circularMean(sleepDays.map { it.bedtimeMinute }),
                )
            }
        }
    }

    private data class ProjectedDay(
        val activity: RingActivityEntity,
        val sleep: RingSleepSessionEntity,
        val hrv: RingMeasurementEntity,
    )

    private fun activity(
        date: LocalDate,
        steps: Double,
        minutes: Double,
        heartRate: Double,
        source: String,
        zoneId: ZoneId,
    ): RingActivityEntity {
        val start = date.atTime(18, 0).atZone(zoneId).toInstant().toEpochMilli()
        val duration = minutes.roundToInt()
        return RingActivityEntity(
            id = "rdi-scenario-activity-$date-$duration-${steps.roundToInt()}",
            startedAt = start,
            endedAt = start + duration * 60_000L,
            activityType = "walking",
            steps = steps.roundToInt(),
            distanceMeters = steps * 0.68,
            caloriesKcal = steps * 0.036,
            durationMinutes = duration,
            averageHeartRate = heartRate,
            source = source,
            rawPayload = null,
        )
    }

    private fun sleep(
        date: LocalDate,
        duration: Double,
        bedtimeMinute: Double,
        efficiency: Double,
        source: String,
        zoneId: ZoneId,
    ): RingSleepSessionEntity {
        val clockMinute = (normalizeMinute(bedtimeMinute) + 720.0).roundToInt() % 1_440
        val time = LocalTime.of(clockMinute / 60, clockMinute % 60)
        val startDate = if (clockMinute >= 12 * 60) date.minusDays(1) else date
        val start = startDate.atTime(time).atZone(zoneId).toInstant().toEpochMilli()
        val total = duration.roundToInt()
        val awake = (total * (1.0 - efficiency / 100.0)).roundToInt().coerceIn(0, total)
        val asleep = total - awake
        val deep = (asleep * 0.22).roundToInt()
        val rem = (asleep * 0.20).roundToInt()
        val light = asleep - deep - rem
        return RingSleepSessionEntity(
            id = "rdi-scenario-sleep-$date-$total-$clockMinute",
            startedAt = start,
            endedAt = start + total * 60_000L,
            deepMinutes = deep,
            lightMinutes = light,
            awakeMinutes = awake,
            remMinutes = rem,
            interruptionMinutes = awake / 3,
            source = source,
            rawPayload = null,
            totalSleepMinutes = total,
        )
    }

    private fun hrv(date: LocalDate, value: Double, source: String, zoneId: ZoneId): RingMeasurementEntity {
        val at = date.atTime(3, 0).atZone(zoneId).toInstant().toEpochMilli()
        return RingMeasurementEntity(
            id = "rdi-scenario-hrv-$date-${(value * 10).roundToInt()}",
            metricType = RingMetricType.HRV.name,
            measuredAt = at,
            primaryValue = value,
            unit = "ms",
            quality = 90,
            source = source,
            rawPayload = null,
        )
    }
}

private fun Long.toDate(zoneId: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

private fun List<Double>.median(): Double? {
    if (isEmpty()) return null
    val values = sorted()
    val middle = values.size / 2
    return if (values.size % 2 == 0) (values[middle - 1] + values[middle]) / 2.0 else values[middle]
}

private fun List<Double>.standardDeviation(): Double {
    if (size < 2) return 0.0
    val mean = average()
    return sqrt(sumOf { (it - mean).pow(2) } / size)
}

private fun circularMean(values: List<Double>): Double {
    if (values.isEmpty()) return 0.0
    val angles = values.map { normalizeMinute(it) / 1_440.0 * 2.0 * PI }
    val angle = kotlin.math.atan2(angles.sumOf { sin(it) }, angles.sumOf { kotlin.math.cos(it) })
    return normalizeMinute(angle / (2.0 * PI) * 1_440.0)
}

private fun circularDelta(value: Double, center: Double): Double {
    var delta = normalizeMinute(value) - normalizeMinute(center)
    if (delta > 720.0) delta -= 1_440.0
    if (delta < -720.0) delta += 1_440.0
    return delta
}

private fun normalizeMinute(value: Double): Double {
    var normalized = value % 1_440.0
    if (normalized < 0.0) normalized += 1_440.0
    return normalized
}

private fun Double.round1(): Double = kotlin.math.round(this * 10.0) / 10.0
private fun Double.round2(): Double = kotlin.math.round(this * 100.0) / 100.0
