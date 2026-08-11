package com.rehealth.genie.ui

import com.rehealth.genie.BuildConfig
import com.rehealth.genie.phm.IndividualAttributionResult
import com.rehealth.genie.phm.PiasAttributionCacheRepository

internal suspend fun runtimeAttributionPiasResult(
    repository: PiasAttributionCacheRepository,
    historyDays: Int,
): IndividualAttributionResult? {
    if (!BuildConfig.SEED_FAKE_HEALTH_DATA) return repository.load(allowMock = false)

    val noAction = List(31) { day -> 0.24 + day * 0.0015 }
    val withPlan = List(31) { day -> 0.24 - day * 0.0013 }
    val preview = IndividualAttributionResult(
        status = "ready",
        historyDays = historyDays.coerceAtLeast(90),
        minHistoryDays = 30,
        currentRiskScore = 0.24,
        riskLevel = "low",
        trend = "improving",
        d30NoAction = noAction.last(),
        d30WithPlan = withPlan.last(),
        riskReduction = noAction.last() - withPlan.last(),
        individualAtt = 0.052,
        attCiLower = 0.031,
        attCiUpper = 0.073,
        attPValue = 0.02,
        attSignificant = true,
        attAvailable = true,
        interventionDays = 18,
        interventionDataSufficient = true,
        headline = "Debug 模拟：坚持当前计划预计改善风险趋势",
        body = "该 PIAS 结果仅用于 Debug 页面与图表验收，不代表真实医学评估。",
        advice = "保持规律活动、睡眠和饮食记录；真实建议请以服务端评估为准。",
        forecastNoAction = noAction,
        forecastWithPlan = withPlan,
        forecastCiUpper = noAction.map { (it + 0.025).coerceAtMost(1.0) },
        forecastCiLower = withPlan.map { (it - 0.025).coerceAtLeast(0.0) },
    )
    repository.save(
        result = preview,
        isMock = true,
        modelVersion = DEBUG_PIAS_PREVIEW_VERSION,
    )
    return repository.load(allowMock = true) ?: preview
}

private const val DEBUG_PIAS_PREVIEW_VERSION = "debug-pias-preview-1.0.0"
