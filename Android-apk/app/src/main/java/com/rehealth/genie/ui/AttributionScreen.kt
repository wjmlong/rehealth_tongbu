package com.rehealth.genie.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rehealth.genie.BuildConfig
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.PatientProfilePayload
import com.rehealth.genie.phm.AttributionHistoryPoint
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ui.theme.AttributionDimensions as Dimensions
import com.rehealth.genie.ui.theme.AttributionMotion as Motion
import com.rehealth.genie.ui.theme.AttributionOpacity as Opacity
import com.rehealth.genie.ui.theme.AttributionPalette as Palette
import com.rehealth.genie.ui.theme.AttributionTypography as Type
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException

@Composable
fun AttributionScreen(
    ringState: RingUiState,
    evaluation: AttributionRiskEvaluation?,
) {
    val application = LocalContext.current.applicationContext as ReHealthApplication
    val feedbackViewModel: InterventionFeedbackViewModel = viewModel(
        factory = InterventionFeedbackViewModel.Factory(LocalContext.current),
    )
    var selectedPeriod by remember { mutableStateOf(AttributionPeriod.DAYS_7) }
    var retryKey by remember { mutableIntStateOf(0) }
    var requestSequence by remember { mutableLongStateOf(0L) }
    var refreshState by remember { mutableStateOf(AttributionRefreshState()) }

    LaunchedEffect(
        retryKey,
        ringState.lastSyncAt,
        ringState.patientMvp?.updatedAt,
        evaluation?.riskScore,
        evaluation?.confirmed,
    ) {
        requestSequence += 1
        val requestId = requestSequence
        val previousData = refreshState.data
        refreshState = refreshState.reduce(AttributionRefreshEvent.Started(requestId))
        try {
            val history = application.riskHistoryRepository.attributionHistory(limit = 90)
            val pias = if (history.isEmpty()) {
                null
            } else {
                application.remotePhmService.attributeIndividual(history, forecastDays = 30)
            }
            refreshState = refreshState.reduce(
                AttributionRefreshEvent.Succeeded(
                    requestId = requestId,
                    data = AttributionRemoteData(history = history, pias = pias),
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val availableData = previousData ?: runCatching {
                AttributionRemoteData(
                    history = application.riskHistoryRepository.attributionHistory(limit = 90),
                    pias = null,
                )
            }.getOrNull()
            refreshState = refreshState.reduce(
                AttributionRefreshEvent.Failed(
                    requestId = requestId,
                    message = error.message ?: "归因服务暂时不可用，请稍后重试。",
                    data = availableData,
                ),
            )
        }
    }

    val remote = refreshState.data ?: AttributionRemoteData(emptyList(), null)
    val attributionProfile = AttributionDataProvenance.trustedProfile(ringState.patientMvp)
    val uiState = AttributionUiMapper.map(
        AttributionUiInput(
            period = selectedPeriod,
            today = LocalDate.now(),
            evaluation = evaluation,
            remote = remote,
            refreshPhase = refreshState.phase,
            refreshError = refreshState.errorMessage,
            activity = ringState.activity?.let { activity ->
                AttributionActivityInput(
                    startedAt = activity.startedAt,
                    activityType = activity.activityType,
                    steps = activity.steps,
                    durationMinutes = activity.durationMinutes,
                    caloriesKcal = activity.caloriesKcal,
                    distanceMeters = activity.distanceMeters,
                    source = activity.source,
                    replay = activity.source.equals("ring_sim", ignoreCase = true),
                )
            },
            allowDebugReplay = BuildConfig.DEBUG,
            factorValues = attributionFactorValues(ringState, attributionProfile),
            interventions = ringState.patientMvp?.interventionPlan.orEmpty().map { intervention ->
                AttributionInterventionInput(
                    id = intervention.id,
                    title = intervention.title,
                    action = intervention.action,
                    duration = intervention.duration,
                    reason = intervention.reason,
                    status = intervention.status,
                )
            },
            interventionSourceMode = ringState.patientMvp?.risk?.mode,
        ),
    )

    AttributionContent(
        state = uiState,
        feedbackViewModel = feedbackViewModel,
        onPeriodSelected = { selectedPeriod = it },
        onRetry = { retryKey += 1 },
    )
}

@Composable
private fun AttributionContent(
    state: AttributionUiState,
    feedbackViewModel: InterventionFeedbackViewModel,
    onPeriodSelected: (AttributionPeriod) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Palette.Page).statusBarsPadding(),
        contentPadding = PaddingValues(Dimensions.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SectionGap),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("健康归因", color = Palette.TextPrimary, style = Type.PageTitle)
                    Text(
                        "个人改善路径 · 近 ${state.period.selectorLabel}",
                        color = Palette.TextSecondary,
                        style = Type.PageSubtitle,
                        modifier = Modifier.padding(top = Dimensions.PageSubtitleTop),
                    )
                }
                IconButton(onClick = onRetry) {
                    Icon(Icons.Outlined.Refresh, "刷新归因数据", tint = Palette.Accent)
                }
            }
        }
        item {
            AttributionPeriodSelector(state.period, onPeriodSelected)
        }
        state.refreshMessage?.let { message ->
            item {
                AttributionRefreshBanner(
                    message = message,
                    loading = state.refreshPhase == AttributionRefreshPhase.LOADING ||
                        state.refreshPhase == AttributionRefreshPhase.REFRESHING,
                    canRetry = state.refreshPhase == AttributionRefreshPhase.ERROR,
                    onRetry = onRetry,
                )
            }
        }
        item { AttributionSummaryCard(state) }
        item { AttributionPiasCard(state.pias, onRetry) }
        item { AttributionActivityCard(state.activity) }
        item { AttributionFactorsCard(state.factorGroups) }
        item { AttributionPlanCard(state.interventions, feedbackViewModel) }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimensions.DisclaimerRadius))
                    .background(Palette.AccentSoft).padding(Dimensions.DisclaimerPadding),
            ) {
                Icon(
                    Icons.Outlined.Shield,
                    null,
                    tint = Palette.Accent,
                    modifier = Modifier.size(Dimensions.DisclaimerIcon),
                )
                Text(
                    "归因结果仅用于健康管理参考。\n不代表医学诊断，也不能替代医生建议。",
                    color = Palette.TextSecondary,
                    style = Type.Body,
                    modifier = Modifier.weight(1f).padding(start = Dimensions.DisclaimerIconGap),
                )
            }
        }
    }
}

@Composable
private fun AttributionPeriodSelector(
    selected: AttributionPeriod,
    onSelected: (AttributionPeriod) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimensions.SelectorRadius))
            .background(Palette.Surface).padding(Dimensions.SelectorPadding).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SelectorGap),
    ) {
        AttributionPeriod.entries.forEach { period ->
            val active = selected == period
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(Dimensions.SelectorItemRadius))
                    .background(if (active) Palette.Accent else Palette.Transparent)
                    .selectable(
                        selected = active,
                        role = Role.Tab,
                        onClick = { onSelected(period) },
                    )
                    .padding(vertical = Dimensions.SelectorItemVerticalPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    period.selectorLabel,
                    color = if (active) Palette.OnAccent else Palette.TextSecondary,
                    style = Type.Selector,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun AttributionRefreshBanner(
    message: String,
    loading: Boolean,
    canRetry: Boolean,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimensions.BannerRadius))
            .background(if (canRetry) Palette.SurfaceWarning else Palette.AccentSoft)
            .padding(
                horizontal = Dimensions.BannerHorizontalPadding,
                vertical = Dimensions.BannerVerticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimensions.BannerSpinner),
                color = Palette.Accent,
                strokeWidth = Dimensions.BannerSpinnerStroke,
            )
            Spacer(Modifier.width(Dimensions.BannerSpinnerGap))
        }
        Text(message, color = Palette.TextSecondary, style = Type.Body, modifier = Modifier.weight(1f))
        if (canRetry) {
            TextButton(onClick = onRetry) { Text("重试", color = Palette.Accent, style = Type.Body) }
        }
    }
}

@Composable
private fun AttributionSummaryCard(state: AttributionUiState) {
    AttributionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("健康改善得分", color = Palette.TextPrimary, style = Type.CardTitle)
                val improvementColor = when {
                    state.improvementPoints == null -> Palette.TextSecondary
                    state.improvementPoints >= 0.0 -> Palette.Accent
                    else -> Palette.ImprovementWorsening
                }
                Text(
                    if (state.improvementPoints == null) "--" else "${state.improvementText} 个百分点",
                    color = improvementColor,
                    style = Type.SummaryScore,
                    modifier = Modifier.padding(top = Dimensions.SummaryScoreTop),
                )
                Text(
                    state.improvementMessage,
                    color = Palette.TextSecondary,
                    style = Type.Detail,
                    modifier = Modifier.padding(top = Dimensions.SummarySupportingTop),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("当前风险", color = Palette.TextSecondary, style = Type.Body)
                Text(
                    state.currentRiskText,
                    color = Palette.TextPrimary,
                    style = Type.RiskScore,
                    modifier = Modifier.padding(top = Dimensions.PageSubtitleTop),
                )
                Text(
                    riskLevelLabel(state.riskLevel),
                    color = Palette.Accent,
                    style = Type.Detail,
                    modifier = Modifier.padding(top = Dimensions.SummarySupportingTop),
                )
            }
        }
        if (state.selectedHistory.size >= 2) {
            AttributionHistoryChart(
                history = state.selectedHistory,
                modifier = Modifier.fillMaxWidth().height(Dimensions.HistoryChartHeight)
                    .padding(top = Dimensions.HistoryChartTop),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(Dimensions.HistoryEmptyHeight)
                    .padding(top = Dimensions.HistoryChartTop)
                    .clip(RoundedCornerShape(Dimensions.ContentRadius)).background(Palette.SurfaceSubtle),
                contentAlignment = Alignment.Center,
            ) {
                Text("已确认风险趋势正在积累", color = Palette.TextSecondary, style = Type.Body)
            }
        }
    }
}

@Composable
private fun AttributionPiasCard(
    pias: AttributionPiasUiState,
    onRetry: () -> Unit,
) {
    AttributionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("个人风险趋势", color = Palette.TextPrimary, style = Type.CardTitle)
                Text(
                    "PIAS 固定 30 天预测 · 执行计划与维持现状",
                    color = Palette.TextSecondary,
                    style = Type.Detail,
                )
            }
            Text(
                when (pias) {
                    is AttributionPiasUiState.Ready -> trendLabel(pias.trend)
                    is AttributionPiasUiState.Accumulating -> "积累中"
                    is AttributionPiasUiState.Failed -> "暂不可用"
                    AttributionPiasUiState.Empty -> "待生成"
                    AttributionPiasUiState.Loading -> "计算中"
                },
                color = Palette.Accent,
                style = Type.Detail,
                modifier = Modifier.clip(CircleShape).background(Palette.AccentSoft)
                    .padding(
                        horizontal = Dimensions.StatusHorizontalPadding,
                        vertical = Dimensions.StatusVerticalPadding,
                    ),
            )
        }
        when (pias) {
            AttributionPiasUiState.Empty -> AttributionCompactMessage("完成已确认风险评估后生成 30 天预测。")
            AttributionPiasUiState.Loading -> AttributionLoadingMessage("正在请求 PIAS 个人归因…")
            is AttributionPiasUiState.Failed -> {
                AttributionCompactMessage(pias.message)
                TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                    Text("重新计算", color = Palette.Accent, style = Type.Body)
                }
            }
            is AttributionPiasUiState.Accumulating -> {
                AttributionCompactMessage(
                    "已有 ${pias.historyDays} 天记录，还需 ${pias.remainingDays} 天才能生成完整预测。",
                )
                LinearProgressIndicator(
                    progress = { (pias.historyDays.toFloat() / pias.minHistoryDays.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(Dimensions.AccumulationProgressHeight).clip(CircleShape),
                    color = Palette.Accent,
                    trackColor = Palette.AccentSoft,
                )
            }
            is AttributionPiasUiState.Ready -> AttributionPiasReadyContent(pias)
        }
    }
}

@Composable
private fun AttributionPiasReadyContent(pias: AttributionPiasUiState.Ready) {
    if (pias.forecast.chartAvailable) {
        AttributionForecastChart(
            forecast = pias.forecast,
            modifier = Modifier.fillMaxWidth().height(Dimensions.ForecastChartHeight)
                .padding(top = Dimensions.ForecastChartTop),
        )
    } else {
        AttributionCompactMessage("PIAS 已完成分析，本次未返回可绘制的预测序列。")
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Dimensions.LegendTop),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.LegendGap),
    ) {
        AttributionLegend(Palette.ForecastNoAction, "维持现状")
        AttributionLegend(Palette.Accent, "执行计划")
        AttributionLegend(Palette.ForecastInterval, "95% 参考区间")
    }
    Row(
        Modifier.fillMaxWidth().padding(top = Dimensions.ForecastMetricsTop),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.ForecastMetricGap),
    ) {
        AttributionForecastMetric(
            label = "维持现状",
            value = pias.forecast.d30NoAction.asPercent(),
            color = Palette.ForecastNoAction,
            modifier = Modifier.weight(1f),
        )
        AttributionForecastMetric(
            label = "执行计划",
            value = pias.forecast.d30WithPlan.asPercent(),
            color = Palette.Accent,
            modifier = Modifier.weight(1f),
        )
        AttributionForecastMetric(
            label = "预计降低",
            value = pias.forecast.riskReduction.asPercent(signed = true),
            color = Palette.ForecastReduction,
            modifier = Modifier.weight(1f),
        )
    }
    val attText = when (val att = pias.att) {
        is AttributionAttUiState.Available -> buildString {
            append("个体 ATT ${att.value.asPercent(signed = true)}")
            if (att.ciLower != null && att.ciUpper != null) {
                append(" · 95% CI [${att.ciLower.asPercent(signed = true)}, ${att.ciUpper.asPercent(signed = true)}]")
            }
            att.pValue?.let { append(" · p=${String.format(Locale.US, "%.3f", it)}") }
        }
        is AttributionAttUiState.Unavailable -> "个体 ATT 暂不可用：${att.reason}"
    }
    Text(
        attText,
        color = Palette.TextSecondary,
        style = Type.Detail,
        modifier = Modifier.padding(top = Dimensions.AttTop),
    )
}

@Composable
private fun AttributionActivityCard(activity: AttributionActivityUi?) {
    AttributionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("今日行为记录", color = Palette.TextPrimary, style = Type.CardTitle)
                Text("本机 Room 中最近一次戒指活动", color = Palette.TextSecondary, style = Type.Detail)
            }
            Text(
                if (activity == null) "待记录" else "已记录",
                color = Palette.Accent,
                style = Type.Detail,
                modifier = Modifier.clip(CircleShape).background(Palette.AccentSoft)
                    .padding(
                        horizontal = Dimensions.StatusHorizontalPadding,
                        vertical = Dimensions.StatusVerticalPadding,
                    ),
            )
        }
        if (activity == null) {
            AttributionCompactMessage("暂无可展示的真实活动记录；同步 MR11 戒指后自动更新。")
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Dimensions.ActivityContentTop)
                    .clip(RoundedCornerShape(Dimensions.ActivityContentRadius))
                    .background(Palette.ActivitySurface).padding(Dimensions.ActivityContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(Dimensions.ActivityBadgeSize).clip(CircleShape)
                        .background(Palette.ActivityBadge),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("行", color = Palette.ActivityAccent, style = Type.ActivityGlyph)
                }
                Column(Modifier.weight(1f).padding(start = Dimensions.ActivityTextGap)) {
                    Text(
                        activityTypeLabel(activity.activityType),
                        color = Palette.TextPrimary,
                        style = Type.Selector,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        formatActivityTime(activity.startedAt),
                        color = Palette.TextSecondary,
                        style = Type.Micro,
                        modifier = Modifier.padding(top = Dimensions.ActivitySupportingTop),
                    )
                }
                Text(
                    activity.provenanceLabel,
                    color = Palette.Accent,
                    style = Type.Micro,
                    textAlign = TextAlign.End,
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = Dimensions.ActivityMetricsTop),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.ActivityMetricGap),
            ) {
                AttributionActivityMetric("步数", "${activity.steps}", Modifier.weight(1f))
                AttributionActivityMetric("时长", "${activity.durationMinutes} 分", Modifier.weight(1f))
                AttributionActivityMetric("热量", "${activity.caloriesKcal.toInt()} kcal", Modifier.weight(1f))
                AttributionActivityMetric(
                    "距离",
                    String.format(Locale.US, "%.1f km", activity.distanceMeters / 1_000.0),
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AttributionFactorsCard(groups: List<AttributionFactorGroupUi>) {
    var expandedFactor by remember { mutableStateOf<String?>(null) }
    val factors = groups.flatMap { it.factors }
    val absoluteContributions = factors.mapNotNull { factor ->
        factor.contribution?.let { contribution -> kotlin.math.abs(contribution) }
    }
    val maxContribution = absoluteContributions.maxOrNull()
        ?.takeIf { it > 0.0 }
        ?: 1.0
    val totalContribution = absoluteContributions.sum().takeIf { it > 0.0 }
    val ranks = factors.mapIndexed { index, factor -> factor.key to index + 1 }.toMap()
    AttributionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("贡献因素", color = Palette.TextPrimary, style = Type.CardTitle, modifier = Modifier.weight(1f))
            Text("16 项 · 点击查看依据", color = Palette.TextSecondary, style = Type.Detail)
        }
        groups.forEach { group ->
            Text(
                group.title,
                color = Palette.Accent,
                style = Type.Body,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = Dimensions.FactorGroupTop),
            )
            group.factors.forEach { factor ->
                AttributionFactorRow(
                    rank = ranks.getValue(factor.key),
                    factor = factor,
                    maxContribution = maxContribution,
                    totalContribution = totalContribution,
                    expanded = expandedFactor == factor.key,
                    onClick = {
                        expandedFactor = if (expandedFactor == factor.key) null else factor.key
                    },
                )
            }
        }
    }
}

@Composable
private fun AttributionFactorRow(
    rank: Int,
    factor: AttributionFactorUi,
    maxContribution: Double,
    totalContribution: Double?,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val contribution = factor.contribution
    val fraction by animateFloatAsState(
        targetValue = contribution?.let { (kotlin.math.abs(it) / maxContribution).toFloat().coerceIn(0f, 1f) } ?: 0f,
        animationSpec = tween(Motion.ProgressMillis),
        label = "attribution-${factor.key}",
    )
    val contributionColor = when {
        contribution == null -> Palette.TextSecondary
        contribution >= 0.0 -> Palette.ContributionRisk
        else -> Palette.Accent
    }
    val contributionScore = contribution?.let { String.format(Locale.US, "%+.3f", it) } ?: "--"
    val contributionShare = if (contribution == null || totalContribution == null) {
        "贡献占比 --"
    } else {
        val percentage = kotlin.math.abs(contribution) / totalContribution * 100.0
        "绝对贡献占比 ${String.format(Locale.US, "%.1f%%", percentage)}"
    }
    Column(
        modifier = Modifier.fillMaxWidth()
            .semantics {
                stateDescription = if (expanded) "已展开" else "已收起"
            }
            .clickable(role = Role.Button, onClickLabel = if (expanded) "收起依据" else "查看依据", onClick = onClick)
            .padding(top = Dimensions.FactorRowTop),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(Dimensions.FactorRankSize).clip(CircleShape).background(Palette.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(rank.toString(), color = Palette.Accent, style = Type.Selector, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(start = Dimensions.FactorContentGap)) {
                Text(factor.label, color = Palette.TextPrimary, style = Type.FactorTitle)
                Text(
                    factor.value ?: "当前值未提供",
                    color = Palette.TextSecondary,
                    style = Type.Detail,
                    modifier = Modifier.padding(top = Dimensions.FactorSupportingTop),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    contributionScore,
                    color = contributionColor,
                    style = Type.FactorScore,
                )
                Text(
                    contributionShare,
                    color = Palette.TextSecondary,
                    style = Type.Micro,
                    modifier = Modifier.padding(top = Dimensions.FactorSupportingTop),
                )
                Text(
                    if (expanded) "收起" else "详情",
                    color = Palette.TextSecondary,
                    style = Type.Micro,
                    modifier = Modifier.padding(top = Dimensions.FactorSupportingTop),
                )
            }
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth()
                .padding(start = Dimensions.FactorIndent, top = Dimensions.FactorBarTop)
                .height(Dimensions.FactorBarHeight).clip(CircleShape),
            color = contributionColor,
            trackColor = Palette.FactorTrack,
        )
        if (expanded) {
            Column(
                Modifier.fillMaxWidth().padding(start = Dimensions.FactorIndent, top = Dimensions.FactorDetailTop)
                    .clip(RoundedCornerShape(Dimensions.ContentRadius))
                    .background(Palette.SurfaceSubtle).padding(Dimensions.FactorDetailPadding),
            ) {
                Text(
                    if (contribution == null) {
                        "本次已确认模型结果没有提供该项贡献值，保留该行以维持完整 Core16 输入视图。"
                    } else {
                        "模型返回贡献值 ${String.format(Locale.US, "%+.3f", contribution)}；正值表示风险方向，负值表示保护方向。"
                    },
                    color = Palette.TextPrimary,
                    style = Type.Detail,
                )
                Text(
                    "数据来源：已确认云端风险评估与本机健康档案。",
                    color = Palette.TextSecondary,
                    style = Type.Micro,
                    modifier = Modifier.padding(top = Dimensions.FactorEvidenceTop),
                )
            }
        }
        if (rank != AttributionUiMapper.CANONICAL_FACTOR_KEYS.size) {
            HorizontalDivider(
                color = Palette.Border,
                modifier = Modifier.padding(start = Dimensions.FactorIndent, top = Dimensions.FactorDividerTop),
            )
        }
    }
}

@Composable
private fun AttributionPlanCard(
    interventions: List<AttributionInterventionUi>,
    feedbackViewModel: InterventionFeedbackViewModel,
) {
    var expanded by remember { mutableStateOf(false) }
    val feedbackState by feedbackViewModel.uiState.collectAsState()
    AttributionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("个性化干预计划", color = Palette.TextPrimary, style = Type.CardTitle)
                Text("仅展示带真实服务端 ID 的计划", color = Palette.TextSecondary, style = Type.Detail)
            }
            Text("${interventions.size} 项", color = Palette.Accent, style = Type.Body)
        }
        if (interventions.isEmpty()) {
            AttributionCompactMessage("暂无可展示的服务端干预计划；本地启发式建议不会显示为真实计划。")
        } else {
            if (expanded) {
                interventions.forEachIndexed { index, intervention ->
                    AttributionInterventionRow(
                        number = index + 1,
                        intervention = intervention,
                        enabled = !feedbackState.isSubmitting,
                        onFeedback = { status ->
                            feedbackViewModel.submitFeedback(intervention.id, status, null)
                        },
                    )
                }
                feedbackState.message?.let { message ->
                    Text(
                        message,
                        color = Palette.Accent,
                        style = Type.Detail,
                        modifier = Modifier.padding(top = Dimensions.PlanFeedbackTop),
                    )
                }
            }
            Button(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().padding(top = Dimensions.PlanButtonTop)
                    .height(Dimensions.PlanButtonHeight),
                shape = RoundedCornerShape(Dimensions.PlanButtonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (expanded) Palette.AccentSoft else Palette.Accent,
                    contentColor = if (expanded) Palette.Accent else Palette.OnAccent,
                ),
            ) {
                Text(if (expanded) "收起干预计划" else "查看详细干预计划", style = Type.ButtonLabel)
            }
        }
    }
}

@Composable
private fun AttributionInterventionRow(
    number: Int,
    intervention: AttributionInterventionUi,
    enabled: Boolean,
    onFeedback: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = Dimensions.InterventionTop)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(Dimensions.InterventionRankSize).clip(CircleShape).background(Palette.AccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    number.toString().padStart(2, '0'),
                    color = Palette.Accent,
                    style = Type.Micro,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f).padding(start = Dimensions.InterventionContentGap)) {
                Text(
                    intervention.title,
                    color = Palette.TextPrimary,
                    style = Type.Selector,
                    fontWeight = FontWeight.SemiBold,
                )
                intervention.action?.let {
                    Text(
                        it,
                        color = Palette.TextPrimary,
                        style = Type.Detail,
                        modifier = Modifier.padding(top = Dimensions.InterventionSupportingTop),
                    )
                }
                val detail = listOfNotNull(intervention.duration, intervention.reason).joinToString(" · ")
                if (detail.isNotBlank()) {
                    Text(
                        detail,
                        color = Palette.TextSecondary,
                        style = Type.Micro,
                        modifier = Modifier.padding(top = Dimensions.InterventionSupportingTop),
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(
                start = Dimensions.InterventionActionIndent,
                top = Dimensions.InterventionActionsTop,
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.InterventionActionGap),
        ) {
            AttributionFeedbackButton(
                label = "完成",
                enabled = enabled && intervention.feedbackEnabled,
                primary = true,
                modifier = Modifier.weight(1f),
                onClick = { onFeedback("completed") },
            )
            AttributionFeedbackButton(
                label = "不适用",
                enabled = enabled && intervention.feedbackEnabled,
                modifier = Modifier.weight(1f),
                onClick = { onFeedback("not_applicable") },
            )
            AttributionFeedbackButton(
                label = "稍后",
                enabled = enabled && intervention.feedbackEnabled,
                modifier = Modifier.weight(1f),
                onClick = { onFeedback("skipped") },
            )
        }
        HorizontalDivider(color = Palette.Border, modifier = Modifier.padding(top = Dimensions.InterventionDividerTop))
    }
}

@Composable
private fun AttributionFeedbackButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(Dimensions.FeedbackButtonHeight),
        contentPadding = PaddingValues(horizontal = Dimensions.FeedbackButtonHorizontalPadding),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) Palette.Accent else Palette.Surface,
            contentColor = if (primary) Palette.OnAccent else Palette.Accent,
        ),
    ) {
        if (primary) {
            Icon(Icons.Outlined.Check, null, modifier = Modifier.size(Dimensions.FeedbackIconSize))
            Spacer(Modifier.width(Dimensions.FeedbackIconGap))
        } else if (label == "不适用") {
            Icon(Icons.Outlined.Close, null, modifier = Modifier.size(Dimensions.FeedbackIconSize))
            Spacer(Modifier.width(Dimensions.FeedbackIconGap))
        }
        Text(label, style = Type.Micro)
    }
}

@Composable
private fun AttributionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimensions.CardRadius),
        colors = CardDefaults.cardColors(containerColor = Palette.Surface),
        border = BorderStroke(Dimensions.CardBorder, Palette.Border),
    ) {
        Column(Modifier.padding(Dimensions.CardPadding), content = content)
    }
}

@Composable
private fun AttributionCompactMessage(message: String) {
    Text(
        message,
        color = Palette.TextSecondary,
        style = Type.Body,
        modifier = Modifier.fillMaxWidth().padding(top = Dimensions.MessageTop)
            .clip(RoundedCornerShape(Dimensions.ContentRadius))
            .background(Palette.SurfaceSubtle).padding(Dimensions.MessagePadding),
    )
}

@Composable
private fun AttributionLoadingMessage(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Dimensions.MessageTop)
            .clip(RoundedCornerShape(Dimensions.ContentRadius))
            .background(Palette.SurfaceSubtle).padding(Dimensions.MessagePadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimensions.MessageSpinner),
            color = Palette.Accent,
            strokeWidth = Dimensions.MessageSpinnerStroke,
        )
        Text(
            message,
            color = Palette.TextSecondary,
            style = Type.Body,
            modifier = Modifier.padding(start = Dimensions.MessageSpinnerGap),
        )
    }
}

@Composable
private fun AttributionActivityMetric(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(Dimensions.ActivityMetricRadius)).background(Palette.SurfaceMetric)
            .padding(vertical = Dimensions.ActivityMetricVerticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = Palette.TextPrimary, style = Type.Detail, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(
            label,
            color = Palette.TextSecondary,
            style = Type.MetricLabel,
            modifier = Modifier.padding(top = Dimensions.ActivityMetricLabelTop),
        )
    }
}

@Composable
private fun AttributionLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(Dimensions.LegendDot).clip(CircleShape).background(color))
        Text(
            label,
            color = Palette.TextSecondary,
            style = Type.Micro,
            modifier = Modifier.padding(start = Dimensions.LegendLabelGap),
        )
    }
}

@Composable
private fun AttributionForecastMetric(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(Dimensions.ContentRadius)).background(color.copy(alpha = Opacity.MetricTint))
            .padding(
                horizontal = Dimensions.ForecastMetricHorizontalPadding,
                vertical = Dimensions.ForecastMetricVerticalPadding,
            ),
    ) {
        Text(label, color = Palette.TextSecondary, style = Type.Micro)
        Text(
            value,
            color = color,
            style = Type.ForecastMetric,
            modifier = Modifier.padding(top = Dimensions.ForecastMetricValueTop),
        )
    }
}

@Composable
private fun AttributionHistoryChart(history: List<AttributionHistoryPoint>, modifier: Modifier) {
    Canvas(modifier) {
        if (history.size < 2) return@Canvas
        val values = history.map { it.riskScore.toFloat() }
        val minimum = values.minOrNull() ?: return@Canvas
        val maximum = values.maxOrNull() ?: return@Canvas
        val range = (maximum - minimum).coerceAtLeast(0.01f)
        val top = Dimensions.HistoryChartInset.toPx()
        val bottom = size.height - Dimensions.HistoryChartInset.toPx()
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / values.lastIndex
            val y = bottom - ((value - minimum) / range) * (bottom - top)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(Palette.Accent, radius = Dimensions.HistoryChartDotRadius.toPx(), center = Offset(x, y))
        }
        drawPath(
            path,
            Palette.Accent,
            style = Stroke(width = Dimensions.HistoryChartStroke.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun AttributionForecastChart(forecast: AttributionForecastUi, modifier: Modifier) {
    Canvas(modifier) {
        val count = minOf(forecast.noAction.size, forecast.withPlan.size)
        if (count < 2) return@Canvas
        val allValues = buildList {
            addAll(forecast.noAction.take(count))
            addAll(forecast.withPlan.take(count))
            addAll(forecast.ciLower.take(count))
            addAll(forecast.ciUpper.take(count))
        }.filter(Double::isFinite)
        val rawMinimum = allValues.minOrNull() ?: return@Canvas
        val rawMaximum = allValues.maxOrNull() ?: return@Canvas
        val padding = ((rawMaximum - rawMinimum) * 0.15).coerceAtLeast(0.01)
        val minimum = (rawMinimum - padding).coerceAtLeast(0.0)
        val maximum = (rawMaximum + padding).coerceAtMost(1.0).coerceAtLeast(minimum + 0.01)
        val left = Dimensions.ForecastChartInset.toPx()
        val right = size.width - Dimensions.ForecastChartInset.toPx()
        val top = Dimensions.ForecastChartInset.toPx()
        val bottom = size.height - Dimensions.ForecastChartInset.toPx()
        fun point(index: Int, value: Double): Offset {
            val x = left + (right - left) * index / (count - 1)
            val y = bottom - ((value - minimum) / (maximum - minimum)).toFloat() * (bottom - top)
            return Offset(x, y.coerceIn(top, bottom))
        }
        repeat(4) { index ->
            val y = top + (bottom - top) * index / 3f
            drawLine(
                Palette.ChartGrid,
                Offset(left, y),
                Offset(right, y),
                Dimensions.ForecastGridStroke.toPx(),
            )
        }
        if (forecast.ciLower.size >= count && forecast.ciUpper.size >= count) {
            val confidencePath = Path()
            forecast.ciUpper.take(count).forEachIndexed { index, value ->
                val position = point(index, value)
                if (index == 0) confidencePath.moveTo(position.x, position.y) else confidencePath.lineTo(position.x, position.y)
            }
            forecast.ciLower.take(count).asReversed().forEachIndexed { reverseIndex, value ->
                val index = count - reverseIndex - 1
                val position = point(index, value)
                confidencePath.lineTo(position.x, position.y)
            }
            confidencePath.close()
            drawPath(confidencePath, Palette.ForecastInterval.copy(alpha = Opacity.ForecastInterval))
        }
        fun drawSeries(values: List<Double>, color: Color, width: Float) {
            val path = Path()
            values.take(count).forEachIndexed { index, value ->
                val position = point(index, value)
                if (index == 0) path.moveTo(position.x, position.y) else path.lineTo(position.x, position.y)
                drawCircle(color, radius = Dimensions.ForecastDotRadius.toPx(), center = position)
            }
            drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round))
        }
        drawSeries(forecast.noAction, Palette.ForecastNoAction, Dimensions.ForecastNoActionStroke.toPx())
        drawSeries(forecast.withPlan, Palette.Accent, Dimensions.ForecastPlanStroke.toPx())
    }
}

private fun attributionFactorValues(
    state: RingUiState,
    profile: PatientProfilePayload?,
): Map<String, String> = buildMap {
    profile?.age?.let { put("age", "$it 岁") }
    profile?.gender?.let { gender ->
        put("gender", when (gender.lowercase()) { "male" -> "男"; "female" -> "女"; else -> gender })
    }
    profile?.bmi?.let { put("bmi", String.format(Locale.US, "%.1f", it)) }
    profile?.smoking?.let { put("smoking", it.asYesNo()) }
    profile?.drinking?.let { put("drinking", it.asYesNo()) }
    profile?.diabetesHistory?.let { put("diabetes_history", it.asHistory()) }
    profile?.hypertensionHistory?.let { put("hypertension_history", it.asHistory()) }
    profile?.familyHistory?.let { put("family_history", it.asHistory()) }
    state.measurements[RingMetricType.BLOOD_PRESSURE]?.let { pressure ->
        put("sbp", "${pressure.primaryValue.toInt()} mmHg")
        pressure.secondaryValue?.let { put("dbp", "${it.toInt()} mmHg") }
    }
}

private fun Boolean.asYesNo(): String = if (this) "是" else "否"

private fun Boolean.asHistory(): String = if (this) "有" else "无"

private fun Double?.asPercent(signed: Boolean = false): String = this?.let {
    String.format(Locale.US, if (signed) "%+.2f%%" else "%.2f%%", it * 100.0)
} ?: "--"

private fun riskLevelLabel(level: String?): String = when (level?.lowercase()) {
    "low" -> "低风险"
    "moderate", "medium" -> "中等风险"
    "high" -> "高风险"
    "very_high", "very high" -> "极高风险"
    null -> "等待已确认评估"
    else -> level
}

private fun trendLabel(trend: String?): String = when (trend?.lowercase()) {
    "improving" -> "趋势改善"
    "worsening" -> "趋势上升"
    "stable" -> "趋势平稳"
    null -> "已完成"
    else -> trend
}

private fun activityTypeLabel(type: String): String = when (type.lowercase()) {
    "walking", "walk" -> "步行活动"
    "running", "run" -> "跑步活动"
    "cycling", "cycle" -> "骑行活动"
    "daily" -> "日常活动"
    else -> type.ifBlank { "戒指活动" }
}

private fun formatActivityTime(timestamp: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
