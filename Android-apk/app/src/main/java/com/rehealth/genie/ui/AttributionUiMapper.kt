package com.rehealth.genie.ui

import com.rehealth.genie.phm.AttributionHistoryPoint
import com.rehealth.genie.phm.IndividualAttributionResult
import java.time.LocalDate
import java.util.Locale

object AttributionUiMapper {
    val CANONICAL_FACTOR_KEYS = listOf(
        "age",
        "gender",
        "bmi",
        "sbp",
        "dbp",
        "fasting_glucose",
        "total_cholesterol",
        "ldl",
        "hdl",
        "triglycerides",
        "exercise_days",
        "smoking",
        "drinking",
        "diabetes_history",
        "hypertension_history",
        "family_history",
    )

    private val factorDefinitions = listOf(
        FactorDefinition("age", "年龄", "基础体征"),
        FactorDefinition("gender", "性别", "基础体征"),
        FactorDefinition("bmi", "BMI", "基础体征"),
        FactorDefinition("sbp", "收缩压", "血压与代谢"),
        FactorDefinition("dbp", "舒张压", "血压与代谢"),
        FactorDefinition("fasting_glucose", "空腹血糖", "血压与代谢"),
        FactorDefinition("total_cholesterol", "总胆固醇", "血压与代谢"),
        FactorDefinition("ldl", "LDL 胆固醇", "血压与代谢"),
        FactorDefinition("hdl", "HDL 胆固醇", "血压与代谢"),
        FactorDefinition("triglycerides", "甘油三酯", "血压与代谢"),
        FactorDefinition("exercise_days", "每周运动天数", "生活方式"),
        FactorDefinition("smoking", "吸烟", "生活方式"),
        FactorDefinition("drinking", "饮酒", "生活方式"),
        FactorDefinition("diabetes_history", "糖尿病史", "病史与家族史"),
        FactorDefinition("hypertension_history", "高血压史", "病史与家族史"),
        FactorDefinition("family_history", "心血管家族史", "病史与家族史"),
    )
    private val sections = factorDefinitions.map { it.section }.distinct()
    private val aliases = CANONICAL_FACTOR_KEYS.associateBy(::aliasKey)
    private val fallbackInterventionIds = setOf("bp_monitor", "walking_zone2", "sleep_baseline")

    fun map(input: AttributionUiInput): AttributionUiState {
        val validHistory = input.remote.history.mapNotNull(::validHistoryPoint).sortedBy { it.first }
        val cutoff = input.today.minusDays(input.period.days - 1)
        val selectedHistory = validHistory
            .filter { (date) -> !date.isBefore(cutoff) && !date.isAfter(input.today) }
            .map { it.second }
        val currentRisk = input.evaluation
            ?.takeIf { it.confirmed }
            ?.riskScore
            ?.takeIf(::validRisk)
            ?: validHistory.lastOrNull()?.second?.riskScore
        val improvement = selectedHistory.takeIf { it.size >= 2 }?.let {
            (it.first().riskScore - it.last().riskScore) * 100.0
        }
        val factors = mapFactors(input.evaluation, input.factorValues)

        return AttributionUiState(
            period = input.period,
            refreshPhase = input.refreshPhase,
            refreshMessage = refreshMessage(input.refreshPhase, input.refreshError),
            currentRisk = currentRisk,
            currentRiskText = currentRisk?.let { String.format(Locale.US, "%.2f%%", it * 100) } ?: "--",
            riskLevel = input.evaluation?.takeIf { it.confirmed }?.riskLevel,
            improvementPoints = improvement,
            improvementText = improvement?.let { String.format(Locale.US, "%+.1f", it) } ?: "--",
            improvementMessage = if (improvement == null) {
                "所选周期至少需要 2 次已确认风险评估"
            } else {
                "最早与最新已确认风险的百分点变化"
            },
            selectedHistory = selectedHistory,
            pias = mapPias(input.remote.pias, input.refreshPhase, input.refreshError),
            activity = mapActivity(input.activity, input.allowDebugReplay),
            factors = factors,
            factorGroups = sections.map { section ->
                AttributionFactorGroupUi(section, factors.filter { it.section == section })
            },
            interventions = mapInterventions(input.interventions, input.interventionSourceMode),
        )
    }

    private fun mapPias(
        result: IndividualAttributionResult?,
        refreshPhase: AttributionRefreshPhase,
        refreshError: String?,
    ): AttributionPiasUiState {
        if (result == null) {
            return when (refreshPhase) {
                AttributionRefreshPhase.LOADING, AttributionRefreshPhase.REFRESHING -> AttributionPiasUiState.Loading
                AttributionRefreshPhase.ERROR -> AttributionPiasUiState.Failed(refreshError ?: "PIAS 暂时不可用")
                AttributionRefreshPhase.IDLE, AttributionRefreshPhase.READY -> AttributionPiasUiState.Empty
            }
        }
        return when (result.status?.lowercase()) {
            "accumulating" -> AttributionPiasUiState.Accumulating(
                historyDays = result.historyDays ?: 0,
                minHistoryDays = result.minHistoryDays ?: 14,
            )
            "error", "failed" -> AttributionPiasUiState.Failed("PIAS 分析未完成，请重试")
            else -> mapReadyPias(result)
        }
    }

    private fun mapReadyPias(result: IndividualAttributionResult): AttributionPiasUiState.Ready {
        val pairedForecast = result.forecastNoAction.zip(result.forecastWithPlan)
            .filter { (noAction, withPlan) -> validRisk(noAction) && validRisk(withPlan) }
        val noAction = pairedForecast.map { it.first }
        val withPlan = pairedForecast.map { it.second }
        val confidence = if (
            noAction.isNotEmpty() &&
            result.forecastCiLower.size >= noAction.size &&
            result.forecastCiUpper.size >= noAction.size
        ) {
            result.forecastCiLower.take(noAction.size).zip(result.forecastCiUpper.take(noAction.size))
                .takeIf { pairs -> pairs.all { (lower, upper) -> validRisk(lower) && validRisk(upper) && lower <= upper } }
        } else {
            null
        }
        val att = result.individualAtt
            ?.takeIf { result.attAvailable != false }
            ?.takeIf(Double::isFinite)
            ?.let {
            AttributionAttUiState.Available(
                value = it,
                ciLower = result.attCiLower?.takeIf(Double::isFinite),
                ciUpper = result.attCiUpper?.takeIf(Double::isFinite),
                pValue = result.attPValue?.takeIf(Double::isFinite),
                significant = result.attSignificant,
            )
        } ?: AttributionAttUiState.Unavailable(
            result.attUnavailableReason ?: if (result.interventionDataSufficient == false) {
                "干预与对照记录不足，暂不能计算 ATT"
            } else {
                "本次 PIAS 未提供 ATT"
            },
        )
        return AttributionPiasUiState.Ready(
            forecast = AttributionForecastUi(
                noAction = noAction,
                withPlan = withPlan,
                ciLower = confidence?.map { it.first }.orEmpty(),
                ciUpper = confidence?.map { it.second }.orEmpty(),
                d30NoAction = result.d30NoAction?.takeIf(::validRisk),
                d30WithPlan = result.d30WithPlan?.takeIf(::validRisk),
                riskReduction = result.riskReduction?.takeIf(Double::isFinite),
            ),
            att = att,
            trend = result.trend,
            historyDays = result.historyDays,
        )
    }

    private fun mapFactors(
        evaluation: AttributionRiskEvaluation?,
        values: Map<String, String>,
    ): List<AttributionFactorUi> {
        val contributions = evaluation?.takeIf { it.confirmed }?.contributions.orEmpty()
            .mapNotNull { (key, value) -> normalizeKey(key)?.let { it to value.takeIf(Double::isFinite) } }
            .filter { it.second != null }
            .associate { it.first to requireNotNull(it.second) }
        val normalizedValues = values.mapNotNull { (key, value) ->
            normalizeKey(key)?.let { it to value.takeIf(String::isNotBlank) }
        }.filter { it.second != null }.associate { it.first to requireNotNull(it.second) }
        return factorDefinitions.map { factor ->
            AttributionFactorUi(
                key = factor.key,
                label = factor.label,
                section = factor.section,
                value = normalizedValues[factor.key],
                contribution = contributions[factor.key],
            )
        }
    }

    private fun mapActivity(
        activity: AttributionActivityInput?,
        allowDebugReplay: Boolean,
    ): AttributionActivityUi? {
        activity ?: return null
        if (activity.replay && !allowDebugReplay) return null
        return AttributionActivityUi(
            startedAt = activity.startedAt,
            activityType = activity.activityType,
            steps = activity.steps.coerceAtLeast(0),
            durationMinutes = activity.durationMinutes.coerceAtLeast(0),
            caloriesKcal = activity.caloriesKcal.coerceAtLeast(0.0),
            distanceMeters = activity.distanceMeters.coerceAtLeast(0.0),
            provenanceLabel = if (activity.replay) {
                "调试回放 · Room（非真实设备）"
            } else if (activity.source.contains("mrd", ignoreCase = true)) {
                "MR11 戒指 · Room"
            } else {
                "本机活动 · Room"
            },
        )
    }

    private fun mapInterventions(
        inputs: List<AttributionInterventionInput>,
        sourceMode: String?,
    ): List<AttributionInterventionUi> {
        if (sourceMode.equals("local_heuristic", ignoreCase = true)) return emptyList()
        return inputs.mapNotNull { input ->
            val id = input.id?.trim()?.takeIf { it.isNotEmpty() && it !in fallbackInterventionIds }
                ?: return@mapNotNull null
            AttributionInterventionUi(
                id = id,
                title = input.title?.takeIf(String::isNotBlank) ?: "健康干预",
                action = input.action?.takeIf(String::isNotBlank),
                duration = input.duration?.takeIf(String::isNotBlank),
                reason = input.reason?.takeIf(String::isNotBlank),
                status = input.status,
                feedbackEnabled = true,
            )
        }
    }

    private fun validHistoryPoint(point: AttributionHistoryPoint): Pair<LocalDate, AttributionHistoryPoint>? {
        if (!validRisk(point.riskScore)) return null
        return runCatching { LocalDate.parse(point.date) }.getOrNull()?.let { it to point }
    }

    private fun validRisk(value: Double): Boolean = value.isFinite() && value in 0.0..1.0

    private fun normalizeKey(key: String): String? = aliases[aliasKey(key)]

    private fun aliasKey(key: String): String = key.filter(Char::isLetterOrDigit).lowercase(Locale.US)

    private fun refreshMessage(phase: AttributionRefreshPhase, error: String?): String? = when (phase) {
        AttributionRefreshPhase.LOADING -> "正在读取已确认风险历史与 PIAS"
        AttributionRefreshPhase.REFRESHING -> "正在刷新，当前显示上次成功结果"
        AttributionRefreshPhase.ERROR -> error ?: "刷新失败，当前显示上次成功结果"
        AttributionRefreshPhase.IDLE, AttributionRefreshPhase.READY -> null
    }

    private data class FactorDefinition(val key: String, val label: String, val section: String)
}
