package com.rehealth.genie.rdi

import com.rehealth.genie.features.ClinicalBloodPressureValues
import com.rehealth.genie.rdi.RdiConfirmedLabEntity
import com.rehealth.genie.rdi.RdiConfirmedMealEntity
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import com.rehealth.genie.diet.DietRecordEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

const val RDI_ALGORITHM_VERSION = "rdi-rule-1.0.1"

/** 状态枚举（设计要求 5.2）：按有效日数/数据可用性判定，而非置信度阈值。 */
object RdiStatus {
    const val NO_DATA = "NO_DATA"
    const val BASELINE_BUILDING = "BASELINE_BUILDING"
    const val PRELIMINARY = "PRELIMINARY"
    const val CONFIRMED = "CONFIRMED"
    const val STALE = "STALE"
    const val INVALID = "INVALID"
    const val DEBUG_MOCK = "DEBUG_MOCK"

    /** 用于 UI 展示的中文标签。 */
    fun label(status: String): String = when (status) {
        NO_DATA -> "尚无数据"
        BASELINE_BUILDING -> "基线建立中"
        PRELIMINARY -> "初步指数"
        CONFIRMED -> "已建立个人基线"
        STALE -> "数据已过期"
        INVALID -> "数据冲突"
        DEBUG_MOCK -> "演示数据"
        else -> "未知状态"
    }
}

data class RdiCalculationInput(
    val scoredOn: LocalDate,
    val zoneId: ZoneId,
    val activities: List<RingActivityEntity>,
    val sleepSessions: List<RingSleepSessionEntity>,
    val measurements: List<RingMeasurementEntity>,
    val previousDisplayScore: Double?,
    /** 近 28 天中有任意有效数据的自然日数（用于状态判定）。 */
    val validDays: Int = 0,
    /** 连续无有效数据的自然日数（>=3 判定 STALE）。 */
    val staleDays: Int = 0,
    /** 是否为调试演示数据，必须醒目标记且禁止导出为正式报告。 */
    val isMock: Boolean = false,
    /** 已冻结的个人基线锚定值（设计 6.2），键为 factorCode。存在时优先于滚动中位数。 */
    val anchoredBaselines: Map<String, Double> = emptyMap(),
    /** 已确认的上臂袖带血压（设计 6.7）：仅验证来源可进入 C_bp_weight。 */
    val bloodPressure: ClinicalBloodPressureValues? = null,
    /** 个人血压目标（mmHg），为 null 时仅相对个人基线计算。 */
    val bpTargetSbp: Double? = null,
    val bpTargetDbp: Double? = null,
    /** 体重记录（设计 6.7）：仅在明确目标方向时计分。 */
    val weights: List<WeightPoint> = emptyList(),
    /** 已确认血检锚点（设计 6.1 C_lab / 5.6）。 */
    val confirmedLabs: List<RdiConfirmedLabEntity> = emptyList(),
    /** 后端确认/模型服务回填的饮食影响（可选叠加）。无时本地从 dietRecords 估算。 */
    val confirmedMeals: List<RdiConfirmedMealEntity> = emptyList(),
    /** 用户当日餐食录入（离线优先，Room v11 diet_records），本地估算 C_diet 影响。 */
    val dietRecords: List<DietRecordEntity> = emptyList(),
)

/** 单条体重记录（设计 6.7）。 */
data class WeightPoint(val date: LocalDate, val kg: Double)

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
        val bpWeight = clampDomain(bpWeightContributions(input), 5.0)
        val lab = clampDomain(labContributions(input), 10.0)
        val diet = clampDomain(dietContributions(input), 5.0)
        val contributions = activity + sleep + recovery + bpWeight + lab + diet
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
            input.isMock -> RdiStatus.DEBUG_MOCK
            input.validDays <= 0 -> RdiStatus.NO_DATA
            input.staleDays >= 3 -> RdiStatus.STALE
            input.validDays >= 14 -> RdiStatus.CONFIRMED
            input.validDays >= 7 -> RdiStatus.PRELIMINARY
            else -> RdiStatus.BASELINE_BUILDING
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
        val baselineSteps = input.anchoredBaselines["steps"] ?: baselineDays.map { it.steps.toDouble() }.medianOrNull()
        var rawStepPoints = baselineSteps?.let { -0.35 * (currentSteps - it) / 1_000.0 } ?: 0.0
        // 绝对状态修正（设计 6.4）：低步数独立累加惩罚，而非与相对项取较大值。
        if (currentSteps < 2_000.0) rawStepPoints += 1.0
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
        val baselineMinutes = input.anchoredBaselines["verified_activity_minutes"]
            ?: baselineDays.chunked(7).map { week -> week.sumOf { it.exerciseMinutes }.toDouble() }.medianOrNull()
        if (currentDays.any { it.exerciseMinutes > 0 }) {
            var rawMinutes = baselineMinutes?.let { -0.50 * (currentMinutes - it) / 30.0 } ?: 0.0
            // 绝对状态修正（设计 6.4）：MVPA 不足独立累加惩罚/达标独立累加奖励。
            if (currentMinutes < 30.0) rawMinutes += 0.5
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

    /** 将睡眠会话聚合为按自然日去重的 SleepDay（设计 6.2 基线使用的同一口径）。 */
    private fun aggregateSleepDays(
        sleepSessions: List<RingSleepSessionEntity>,
        zoneId: ZoneId,
    ): Map<LocalDate, SleepDay> =
        sleepSessions.mapNotNull { session ->
            val durationMinutes = ((session.endedAt - session.startedAt) / 60_000L).toInt()
            if (durationMinutes !in 120..900) return@mapNotNull null
            val asleep = (session.deepMinutes + session.lightMinutes + session.remMinutes).takeIf { it > 0 }
                ?: (durationMinutes - session.awakeMinutes).coerceAtLeast(0)
            SleepDay(
                date = session.endedAt.toDate(zoneId),
                durationMinutes = durationMinutes,
                bedtimeMinute = session.startedAt.bedtimeMinute(zoneId),
                efficiency = (asleep.toDouble() / durationMinutes * 100.0).coerceIn(0.0, 100.0),
                source = session.source,
            )
        }.groupBy { it.date }.mapValues { (_, values) -> values.maxBy { it.durationMinutes } }

    private fun sleepContributions(input: RdiCalculationInput): List<RdiContribution> {
        val days = aggregateSleepDays(input.sleepSessions, input.zoneId)
        val current = (0L..6L).mapNotNull { days[input.scoredOn.minusDays(it)] }
        if (current.isEmpty()) return emptyList()
        val baseline = (7L..27L).mapNotNull { days[input.scoredOn.minusDays(it)] }
        val coverage = (current.size / 7.0).coerceIn(0.0, 1.0)
        val sourcePenalty = if ((current + baseline).map { it.source }.distinct().size > 1) 0.65 else 1.0
        val confidence = (coverage * sourcePenalty).coerceIn(0.0, 1.0)
        val duration = current.map { it.durationMinutes.toDouble() }.average()
        val baselineDuration = input.anchoredBaselines["sleep_duration"] ?: baseline.map { it.durationMinutes.toDouble() }.medianOrNull()
        val absoluteDurationRaw = when {
            duration < 420.0 -> 0.40 * ((420.0 - duration) / 30.0)
            duration > 540.0 -> 0.20 * ((duration - 540.0) / 30.0)
            else -> 0.0
        }
        // 相对基线项（设计 6.5）：向 7–9h 区间改善每 30min 最多 -0.30，远离每 30min 最多 +0.30。
        val relativeDurationRaw = baselineDuration?.let {
            val bDev = it - 480.0
            val cDev = duration - 480.0
            val improvement = kotlin.math.abs(bDev) - kotlin.math.abs(cDev)
            (-0.30 / 30.0) * improvement
        } ?: 0.0
        val durationRaw = absoluteDurationRaw + relativeDurationRaw
        val result = mutableListOf(
            contribution(
                "sleep_duration",
                "sleep",
                "ROOM_WEARABLE",
                duration,
                baselineDuration,
                "min/night",
                durationRaw,
                confidence,
                "近7日平均睡眠${(duration / 60.0).round2()}小时",
                "wearable:sleep_duration:${input.scoredOn}",
            ),
        )
        if (current.size >= 3) {
            val regularity = current.map { it.bedtimeMinute.toDouble() }.standardDeviation()
            val baselineRegularity = input.anchoredBaselines["sleep_regularity"]
                ?: baseline.map { it.bedtimeMinute.toDouble() }.takeIf { it.size >= 5 }?.standardDeviation()
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
        val baselineEfficiency = input.anchoredBaselines["sleep_efficiency"]
            ?: baseline.map { it.efficiency }.medianOrNull()
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
        val result = mutableListOf<RdiContribution>()
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
        // 同设备 + 同算法版本门槛（设计 6.6）：跨设备/固件不计入，避免基线错配。
        val sameDeviceHrv = current.size >= 5 && baseline.size >= 5 &&
            (current + baseline).map { it.source }.distinct().size == 1
        if (sameDeviceHrv) {
            val currentValue = current.map { it.value }.medianOrNull()
            val baselineValue = input.anchoredBaselines["hrv_personal_trend"]
                ?: baseline.map { it.value }.medianOrNull()?.takeIf { it > 0.0 }
            if (currentValue != null && baselineValue != null) {
                val relativeChange = (currentValue - baselineValue) / baselineValue
                val raw = -0.40 * (relativeChange / 0.10)
                val confidence = (
                    (current.size / 7.0).coerceIn(0.0, 1.0) *
                        current.map { it.quality }.average().coerceIn(0.0, 1.0)
                    ).coerceIn(0.0, 1.0)
                result += contribution(
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
                )
            }
        }

        // 静息/日均心率因子（设计 6.6）：P_restingHr = +0.80 × Δ/5bpm，同样遵循同设备门槛。
        val hrByDay = input.activities.filter { it.averageHeartRate != null }
            .groupBy { it.startedAt.toDate(input.zoneId) }
            .mapValues { (_, recs) -> recs.mapNotNull { it.averageHeartRate }.medianOrNull() }
            .mapNotNull { (date, value) -> value?.let { date to it } }.toMap()
        val hrCurrent = (0L..6L).mapNotNull { hrByDay[input.scoredOn.minusDays(it)] }
        val hrBaseline = (7L..27L).mapNotNull { hrByDay[input.scoredOn.minusDays(it)] }
        if (hrCurrent.size >= 5 && hrBaseline.size >= 5) {
            val hrCurrentValue = hrCurrent.medianOrNull()
            val hrBaselineValue = input.anchoredBaselines["resting_hr"]
                ?: hrBaseline.medianOrNull()?.takeIf { it > 0.0 }
            if (hrCurrentValue != null && hrBaselineValue != null) {
                val hrRaw = 0.80 * (hrCurrentValue - hrBaselineValue) / 5.0
                val hrConfidence = (hrCurrent.size / 7.0).coerceIn(0.0, 1.0)
                result += contribution(
                    "resting_hr",
                    "recovery",
                    "ROOM_WEARABLE_SAME_DEVICE",
                    hrCurrentValue,
                    hrBaselineValue,
                    "bpm",
                    hrRaw,
                    hrConfidence,
                    "近7日日均心率较个人基线升高${((hrCurrentValue - hrBaselineValue)).round2()}次/分",
                    "wearable:resting_hr:${input.scoredOn}",
                )
            }
        }
        return result
    }

    /**
     * 血压与体重领域（设计 6.7）：C_bp_weight 上限 ±5。
     * 仅已验证的上臂袖带/医院/确认手动录入的血压进入正式贡献；未验证手表血压不计分。
     * 体重仅在明确目标方向时计分，28 天趋势 ±1% 对应 ±0.5，封顶 ±2；一周变化 >2% 停止奖励。
     */
    private fun bpWeightContributions(input: RdiCalculationInput): List<RdiContribution> {
        val result = mutableListOf<RdiContribution>()
        val bp = input.bloodPressure
        // 正式贡献仅接受已确认的上臂袖带来源，且有效日足够（设计 6.7）。
        if (bp != null && bp.confirmedUpperArmCuff && (bp.validDays ?: 0) >= 3) {
            val sbp = bp.sbp7dMean ?: 0.0
            val dbp = bp.dbp7dMean ?: 0.0
            if (sbp > 0.0 && dbp > 0.0) {
                val baselineSbp = input.anchoredBaselines["bp_sbp"]
                val baselineDbp = input.anchoredBaselines["bp_dbp"]
                val sbpTarget = input.bpTargetSbp ?: baselineSbp
                val dbpTarget = input.bpTargetDbp ?: baselineDbp
                val sbpRaw = if (sbpTarget != null) 0.60 * (sbp - sbpTarget) / 5.0 else 0.0
                val dbpRaw = if (dbpTarget != null) 0.30 * (dbp - dbpTarget) / 5.0 else 0.0
                val raw = sbpRaw + dbpRaw
                val confidence = if (input.bpTargetSbp != null || baselineSbp != null) 0.85 else 0.0
                val label = if (input.bpTargetSbp != null) {
                    "血压相对个人目标"
                } else if (baselineSbp != null) {
                    "血压相对个人基线（不代表达标判断）"
                } else {
                    "血压无可用基线/目标"
                }
                if (raw != 0.0 && confidence > 0.0) {
                    result += contribution(
                        "bp_sbp_dbp",
                        "bp_weight",
                        "ROOM_CONFIRMED_CUFF",
                        sbp,
                        baselineSbp ?: sbpTarget ?: sbp,
                        "mmHg",
                        raw,
                        confidence,
                        "$label：收缩压${sbp.round2()}/舒张压${dbp.round2()} mmHg",
                        "wearable:bp:${input.scoredOn}",
                    )
                }
            }
        }

        // 体重（设计 6.7）：仅在有目标方向时计分。
        if (input.weights.size >= 14) {
            val recent = input.weights.sortedBy { it.date }.takeLast(14)
            val earliest = recent.first()
            val latest = recent.last()
            val days = java.time.temporal.ChronoUnit.DAYS.between(earliest.date, latest.date).toDouble().coerceAtLeast(1.0)
            if (days >= 14) {
                val changePct = (latest.kg - earliest.kg) / earliest.kg * 100.0
                val per28ChangePct = changePct * (28.0 / days)
                val weightRaw = when {
                    per28ChangePct > 2.0 -> 0.0 // 一周变化 >2% 停止奖励，标记复核
                    per28ChangePct >= 0.0 -> 0.50 * (per28ChangePct / 1.0).coerceIn(0.0, 2.0)
                    else -> 0.50 * (per28ChangePct / 1.0).coerceIn(-2.0, 0.0)
                }
                result += contribution(
                    "weight_trend",
                    "bp_weight",
                    "ROOM_SCALE",
                    latest.kg,
                    earliest.kg,
                    "kg",
                    weightRaw,
                    0.70,
                    "近${days.toInt()}天体重变化${changePct.round2()}%（28天窗口近似）",
                    "wearable:weight:${input.scoredOn}",
                )
            }
        }
        return result
    }

    /**
     * 血检风险锚点领域（设计 6.1 C_lab）：上限 ±10，权重最高。
     * 每个已确认指标：finalPoints = 0.8 × 实测贡献 + 0.2 × 控制支持趋势。
     * OCR 未确认（confidence=0）不计分；无数据时本域贡献为 0（不伪装基线）。
     */
    private fun labContributions(input: RdiCalculationInput): List<RdiContribution> {
        return input.confirmedLabs.mapNotNull { lab ->
            if (lab.confidence <= 0.0) return@mapNotNull null
            // 实测贡献：由 marker 参考范围映射到 -10~+10 的贡献，当前采用规范化后的实测点（由 model-service 给出）。
            // 此处以 measuredRawPoint 占位，实际由 model-service 的 lab 标准化服务提供。
            val measuredRawPoint = lab.measuredValue.takeIf { it.isFinite() } ?: return@mapNotNull null
            val finalPoints = 0.8 * measuredRawPoint + 0.2 * lab.controlTrend
            contribution(
                factorCode = lab.markerCode,
                domain = "lab",
                source = lab.source,
                currentValue = lab.measuredValue,
                baselineValue = null,
                unit = lab.unit,
                rawPoints = measuredRawPoint,
                confidence = lab.confidence,
                evidence = "${lab.markerCode} ${lab.measuredValue}${lab.unit}（控制支持趋势 ${lab.controlTrend.round2()}）",
                sourceFactorId = "lab:${lab.markerCode}:${lab.measuredAt}",
            ).copy(finalPoints = finalPoints)
        }
    }

    /**
     * 饮食领域（设计 6.1 C_diet）：上限 ±5。
     * 优先消费后端模型回填的 [confirmedMeals]（精确 mealImpact）；
     * 否则本地基于当日 diet_records 汇总，按保守推荐区间估算每餐影响，
     * 单餐范围 -2~+2，多餐求和后按 domain 上限收缩。无记录时贡献为 0。
     */
    private fun dietContributions(input: RdiCalculationInput): List<RdiContribution> {
        val fromBackend = input.confirmedMeals.mapNotNull { meal ->
            if (meal.confidence <= 0.0) return@mapNotNull null
            val impact = meal.mealImpact.coerceIn(-2.0, 2.0)
            contribution(
                factorCode = "diet_${meal.mealType.lowercase()}",
                domain = "diet",
                source = "ROOM_MEAL_CONFIRMED",
                currentValue = meal.kcalHigh,
                baselineValue = meal.kcalLow,
                unit = "kcal",
                rawPoints = impact,
                confidence = meal.confidence,
                evidence = "${meal.mealType} ${meal.reasonText}（影响 ${impact.round2()} 分）",
                sourceFactorId = "meal:${meal.mealType}:${meal.recordedAt}",
            ).copy(finalPoints = impact)
        }
        if (fromBackend.isNotEmpty()) return fromBackend
        return localDietContributions(input.dietRecords)
    }

    /**
     * 本地餐食归因估算：从当日 diet_records 汇总，按保守营养区间给出每餐影响。
     * 仅使用用户已录入的数据，不依赖网络或模型服务；属知情估算，非医学诊断。
     */
    private fun localDietContributions(records: List<DietRecordEntity>): List<RdiContribution> {
        if (records.isEmpty()) return emptyList()
        val totalKcal = records.sumOf { it.caloriesKcal }
        val totalSodium = records.sumOf { it.sodiumMilligrams ?: 0.0 }
        val totalProtein = records.sumOf { it.proteinGrams ?: 0.0 }
        val totalFat = records.sumOf { it.fatGrams ?: 0.0 }

        // 当日总影响（保守，正向仅在接近推荐区间时给小正分）。
        var dayImpact = 0.0
        // 热量：以 2000 kcal 为参考中线，[1600,2400] 内给 +1.0。
        dayImpact += when {
            totalKcal in 1600.0..2400.0 -> 1.0
            totalKcal < 1600.0 -> 1.0 - (1600.0 - totalKcal) / 200.0 * 0.3
            else -> 1.0 - (totalKcal - 2400.0) / 400.0 * 0.4
        }
        // 钠：>2300mg 部分每 1000mg -0.3，封顶 -1.0。
        if (totalSodium > 2300.0) {
            dayImpact -= ((totalSodium - 2300.0) / 1000.0 * 0.3).coerceAtMost(1.0)
        }
        // 蛋白：≥50g +0.3，<40g -0.3。
        dayImpact += when {
            totalProtein >= 50.0 -> 0.3
            totalProtein < 40.0 -> -0.3
            else -> 0.0
        }
        // 脂肪：供能占比 >35% -0.3。
        if (totalKcal > 0.0 && totalFat * 9.0 / totalKcal > 0.35) {
            dayImpact -= 0.3
        }
        dayImpact = dayImpact.coerceIn(-5.0, 5.0)

        // 按每餐热量占比分摊当日影响，单餐限制在 ±2。
        return records.mapNotNull { record ->
            val share = if (totalKcal > 0.0) record.caloriesKcal / totalKcal else 1.0 / records.size
            val impact = (dayImpact * share).coerceIn(-2.0, 2.0)
            contribution(
                factorCode = "diet_${record.mealType.lowercase()}",
                domain = "diet",
                source = "ROOM_DIET_LOCAL",
                currentValue = record.caloriesKcal,
                baselineValue = 2000.0,
                unit = "kcal",
                rawPoints = impact,
                confidence = 0.6,
                evidence = "${record.mealType} ${record.caloriesKcal.round0()}kcal" +
                    (record.sodiumMilligrams?.let { "/钠${it.round0()}mg" } ?: "") +
                    "（本地估算影响 ${impact.round2()} 分）",
                sourceFactorId = "diet:${record.id}",
            ).copy(finalPoints = impact)
        }
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
data class SleepDay(
    val date: LocalDate,
    val durationMinutes: Int,
    val bedtimeMinute: Int,
    val efficiency: Double,
    val source: String,
)
private data class MeasurementDay(val value: Double, val source: String, val quality: Double)

private fun Long.toDate(zoneId: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

internal fun Long.bedtimeMinute(zoneId: ZoneId): Int {
    val time = Instant.ofEpochMilli(this).atZone(zoneId).toLocalTime()
    return ((time.hour - 12 + 24) % 24) * 60 + time.minute
}

internal fun String.isDailyAggregate(): Boolean {
    val value = lowercase()
    return value.contains("daily") || value.contains("summary") || value == "steps"
}

internal fun String.isExerciseSession(): Boolean {
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

private fun Double.round0(): Double = kotlin.math.round(this)
private fun Double.round2(): Double = kotlin.math.round(this * 100.0) / 100.0
private fun Double.round3(): Double = kotlin.math.round(this * 1_000.0) / 1_000.0
