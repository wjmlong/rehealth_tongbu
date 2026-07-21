package com.rehealth.genie.phm

import com.rehealth.genie.ring.RingMetricType

data class LifeState(
    val score: Int,
    val label: String,
    val insight: String,
)

data class Intervention(
    val title: String,
    val reason: String,
    val duration: String,
    val category: String,
)

enum class ModelInputStage {
    READY,
    FEATURE_EXTRACTED,
    LEARNING,
}

data class ModelInputStatus(
    val type: RingMetricType,
    val label: String,
    val feature: String,
    val stage: ModelInputStage,
)

interface PhmService {
    fun todayState(): LifeState
    fun interventions(): List<Intervention>
    fun modelInputs(): List<ModelInputStatus>
}

class MockPhmService : PhmService {
    override fun todayState() = LifeState(
        score = 82,
        label = "良好",
        insight = "昨晚睡眠恢复良好，今天适合保持轻量活动。",
    )

    override fun interventions() = listOf(
        Intervention("晚餐少油少盐", "优化血压与睡眠质量", "今晚", "饮食"),
        Intervention("睡前 30 分钟放松训练", "帮助入睡，提高睡眠质量", "30 分钟", "睡眠"),
        Intervention("今日步数达到 8000 步", "保持代谢活跃", "约 20 分钟", "运动"),
    )

    override fun modelInputs() = listOf(
        ModelInputStatus(RingMetricType.HEART_RATE, "心率", "静息心率、日内波动", ModelInputStage.LEARNING),
        ModelInputStatus(RingMetricType.BLOOD_OXYGEN, "血氧", "均值、低值时长", ModelInputStage.FEATURE_EXTRACTED),
        ModelInputStatus(RingMetricType.BLOOD_PRESSURE, "血压", "收缩压、舒张压趋势", ModelInputStage.LEARNING),
        ModelInputStatus(RingMetricType.SLEEP, "睡眠", "时长、阶段、连续性", ModelInputStage.LEARNING),
        ModelInputStatus(RingMetricType.TEMPERATURE, "体温", "个人基线偏差", ModelInputStage.FEATURE_EXTRACTED),
        ModelInputStatus(RingMetricType.STEPS, "步数", "总量、时段分布", ModelInputStage.LEARNING),
    )
}
