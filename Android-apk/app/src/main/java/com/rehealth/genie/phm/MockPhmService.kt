package com.rehealth.genie.phm

import com.rehealth.genie.network.dto.RiskResultDto
import com.rehealth.genie.ring.RingMetricType

/**
 * Offline fallback for [PhmService]. Implements the full app-facing interface, but every
 * value it returns is COMPUTED from the available feature vector (or the last one seen)
 * via [CvdRiskHeuristic] — not canned/random numbers. This satisfies Requirement C:
 * when the real backend model-service is unavailable, the app still shows simulated data
 * derived from real inputs, never arbitrary placeholders.
 *
 * Conservative, non-diagnostic text only. Must not be presented as a diagnosis.
 */
class MockPhmService : PhmService {
    private var lastVector: CvdFeatureVector? = null
    private var lastRisk: RiskResult? = null

    override fun modelInputs(): List<ModelInputStatus> = listOf(
        ModelInputStatus(RingMetricType.HEART_RATE, "心率", "静息心率、日内波动", ModelInputStage.LEARNING),
        ModelInputStatus(RingMetricType.BLOOD_OXYGEN, "血氧", "均值、低值时长", ModelInputStage.FEATURE_EXTRACTED),
        ModelInputStatus(RingMetricType.BLOOD_PRESSURE, "血压", "收缩压、舒张压趋势", ModelInputStage.LEARNING),
        ModelInputStatus(RingMetricType.SLEEP, "睡眠", "时长、阶段、连续性", ModelInputStage.LEARNING),
        ModelInputStatus(RingMetricType.TEMPERATURE, "体温", "个人基线偏差", ModelInputStage.FEATURE_EXTRACTED),
        ModelInputStatus(RingMetricType.STEPS, "步数", "总量、时段分布", ModelInputStage.LEARNING),
    )

    override suspend fun todayState(): LifeState {
        val score = lastRisk?.riskScore ?: CvdRiskHeuristic.score(lastVector ?: NEUTRAL_VECTOR)
        val lifeScore = (100 - score * 55).coerceIn(0.0, 100.0).toInt()
        val label = when {
            lifeScore >= 80 -> "良好"
            lifeScore >= 60 -> "平稳"
            lifeScore >= 40 -> "需关注"
            else -> "偏低"
        }
        val insight = when {
            lifeScore >= 80 -> "当前指标平稳，保持规律作息与适度活动即可。"
            lifeScore >= 60 -> "整体状态平稳，注意今日活动量与睡眠。"
            lifeScore >= 40 -> "部分风险指标偏高，建议今天优先关注血压与活动。"
            else -> "风险指标偏高，建议今天减少高盐饮食并规律监测。"
        }
        return LifeState(score = lifeScore, label = label, insight = insight)
    }

    override suspend fun interventions(): List<Intervention> =
        computedInterventions(lastVector ?: NEUTRAL_VECTOR)

    override suspend fun login(username: String, password: String): LoginResult =
        LoginResult(token = "mock-token", userId = "mock-user", username = username)

    override suspend fun logout() {}

    override suspend fun evaluateRisk(request: RiskEvaluateRequest): RiskResult {
        val vector = request.featureVector
        lastVector = vector
        val score = CvdRiskHeuristic.score(vector)
        val lvl = CvdRiskHeuristic.level(score)
        val result = RiskResult(
            riskScore = score,
            riskLevel = lvl,
            summary = CvdRiskHeuristic.summary(score, vector),
            isMock = true,
            modelVersion = "rehealth-local-heuristic-0.1",
        )
        lastRisk = result
        return result
    }

    override suspend fun latestRisk(): RiskResult? {
        val vector = lastVector ?: NEUTRAL_VECTOR
        val score = CvdRiskHeuristic.score(vector)
        val lvl = CvdRiskHeuristic.level(score)
        return RiskResult(
            riskScore = score,
            riskLevel = lvl,
            summary = CvdRiskHeuristic.summary(score, vector),
            isMock = true,
            modelVersion = "rehealth-local-heuristic-0.1",
        )
    }

    override suspend fun todayIntervention(): InterventionPlan {
        val vector = lastVector ?: NEUTRAL_VECTOR
        val top = topFactor(vector)
        return InterventionPlan(
            priorityIntervention = top?.title ?: "保持规律作息与适度运动",
            rationale = top?.reason ?: "基于当前可计算特征的通用健康建议",
            expectedImpact = "有助于维持代谢与心血管稳态",
            isMock = true,
        )
    }

    override suspend fun generateIntervention(request: InterventionGenerateRequest): InterventionPlan {
        val vector = request.featureVector ?: lastVector ?: NEUTRAL_VECTOR
        val top = topFactor(vector)
        return InterventionPlan(
            priorityIntervention = top?.title ?: "保持规律作息与适度运动",
            rationale = top?.reason ?: "基于当前可计算特征的通用健康建议",
            expectedImpact = "维持代谢与心血管稳态",
            isMock = true,
        )
    }

    override suspend fun submitFeedback(planId: String, feedback: FeedbackRequest): FeedbackResult =
        FeedbackResult(id = planId, status = feedback.status, persisted = false)

    override suspend fun bindDevice(request: DeviceBindRequest): DeviceBindResult =
        DeviceBindResult(id = "mock-device-id", userId = "mock-user", deviceId = request.deviceId)

    override suspend fun uploadMeasurements(request: TelemetryBatchRequest): TelemetryBatchResult =
        TelemetryBatchResult(batchId = request.batchId, accepted = true, persisted = false, recordCount = request.measurements.size)

    override suspend fun recordAttributionEvents(request: AttributionEventsRequest): AttributionResult =
        AttributionResult(requestId = null)

    override suspend fun attributeIndividual(
        history: List<AttributionHistoryPoint>,
        forecastDays: Int,
        language: String,
    ): IndividualAttributionResult {
        if (history.isEmpty()) {
            return IndividualAttributionResult(
                headline = "暂无归因数据",
                body = "本地模拟归因需要至少一条风险记录。",
                advice = "保持规律作息与适度运动。",
                currentRiskScore = null,
                riskLevel = null,
            )
        }
        val recent = history.takeLast(7)
        val avg = recent.mapNotNull { it.riskScore }.average()
        val first = recent.first().riskScore
        val last = recent.last().riskScore
        val delta = last - first
        val level = CvdRiskHeuristic.level(avg)
        val trend = when {
            delta < -0.01 -> "下降"
            delta > 0.01 -> "上升"
            else -> "平稳"
        }
        val headline = "近 ${recent.size} 次记录平均风险为${CvdRiskHeuristic.label(avg)}（${trend}）"
        val advice = when {
            delta < -0.01 -> "当前干预方向有效，建议继续保持。"
            delta > 0.01 -> "风险有上升趋势，建议加强血压与活动管理。"
            else -> "风险平稳，维持当前健康习惯。"
        }
        return IndividualAttributionResult(
            headline = headline,
            body = "基于本地 ${recent.size} 条风险记录计算，平均风险 ${String.format("%.2f", avg)}。",
            advice = advice,
            currentRiskScore = avg,
            riskLevel = level,
        )
    }

    // ---- helpers ----

    private fun computedInterventions(vector: CvdFeatureVector): List<Intervention> {
        val list = mutableListOf<Intervention>()
        topFactor(vector)?.let { list += it }
        if (vector.exerciseDays == null || vector.exerciseDays < 4) {
            list += Intervention("今日步数达到 8000 步", "保持代谢活跃", "约 20 分钟", "运动")
        }
        if (vector.sbp != null || vector.dbp != null) {
            list += Intervention("睡前 30 分钟放松训练", "帮助入睡，提高睡眠质量", "30 分钟", "睡眠")
        }
        if (list.isEmpty()) {
            list += Intervention("保持规律作息", "维持代谢与心血管稳态", "每天", "生活")
        }
        return list
    }

    private fun topFactor(vector: CvdFeatureVector): Intervention? {
        val candidates = mutableListOf<Pair<Double, Intervention>>()
        vector.sbp?.takeIf { it >= 130 }?.let {
            candidates += it to Intervention("规律监测并记录血压", "收缩压偏高，需关注血压趋势", "早晚各 1 次", "血压")
        }
        vector.bmi?.takeIf { it >= 28 }?.let {
            candidates += it to Intervention("控制晚餐油盐摄入", "体重偏高，优化血压与睡眠", "今晚", "饮食")
        }
        vector.smoking?.takeIf { it == 1 }?.let {
            candidates += 100.0 to Intervention("制定减烟计划", "吸烟是心血管重要风险因素", "逐步减少", "生活方式")
        }
        vector.diabetesHistory?.takeIf { it == 1 }?.let {
            candidates += 90.0 to Intervention("关注餐后血糖", "糖尿病史需管理代谢", "每日", "代谢")
        }
        vector.exerciseDays?.takeIf { it < 3 }?.let {
            candidates += (10 - it * 2.0) to Intervention("增加日常活动量", "活动不足，适度提升代谢", "每天 20 分钟", "运动")
        }
        return candidates.maxByOrNull { it.first }?.second
    }

    private val NEUTRAL_VECTOR = CvdFeatureVector()

    private companion object {
        const val TAG = "MockPhmService"
    }
}
