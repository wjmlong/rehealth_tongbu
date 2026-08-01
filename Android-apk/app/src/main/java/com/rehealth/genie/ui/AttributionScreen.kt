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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rehealth.genie.BuildConfig
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.data.profileAvatarStorageKey
import com.rehealth.genie.network.PatientProfilePayload
import com.rehealth.genie.network.dto.BehaviorRecordDto
import com.rehealth.genie.phm.AttributionHistoryPoint
import com.rehealth.genie.rdi.RdiContributionEntity
import com.rehealth.genie.rdi.RdiDisplayData
import com.rehealth.genie.rhi.RhiDailyScore
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
    val dietEntryViewModel: DietEntryViewModel = viewModel(
        factory = DietEntryViewModel.Factory(LocalContext.current),
    )
    val dietEntryState by dietEntryViewModel.state.collectAsState()
    val activeWearableBinding by application.activeWearableStore.activeBinding.collectAsState()
    val rhiViewModel: RhiViewModel = viewModel(factory = RhiViewModel.Factory(LocalContext.current))
    val rhiPeriodSummary by rhiViewModel.periodSummary.collectAsState()
    val rhiRefreshError by rhiViewModel.refreshError.collectAsState()
    val rdiViewModel: RdiViewModel = viewModel(factory = RdiViewModel.Factory(LocalContext.current))
    val rdiDisplayData by rdiViewModel.display.collectAsState()
    val rdiPeriodSummary by rdiViewModel.periodSummary.collectAsState()
    val behaviorOwnerKey = remember(application.sessionStore.userId, application.sessionStore.username) {
        profileAvatarStorageKey(
            application.sessionStore.userId ?: application.sessionStore.username ?: "signed-out",
        )
    }
    val behaviorViewModel: BehaviorRecordViewModel = viewModel(
        key = "behavior-records-$behaviorOwnerKey",
        factory = remember(application) { BehaviorRecordViewModel.Factory(application) },
    )
    val behaviorState by behaviorViewModel.state.collectAsState()
    var selectedPeriod by remember { mutableStateOf(AttributionPeriod.DAYS_7) }
    var retryKey by remember { mutableIntStateOf(0) }
    var requestSequence by remember { mutableLongStateOf(0L) }
    var refreshState by remember { mutableStateOf(AttributionRefreshState()) }

    LaunchedEffect(activeWearableBinding.address, activeWearableBinding.vendor) {
        dietEntryViewModel.preparePendingUploads()
    }

    LaunchedEffect(
        selectedPeriod,
        ringState.lastSyncAt,
        ringState.patientMvp?.profile?.updatedAt,
    ) {
        rhiViewModel.refresh(
            selectedPeriod.days.toInt(),
            AttributionDataProvenance.trustedProfile(ringState.patientMvp),
        )
    }

    LaunchedEffect(
        selectedPeriod,
        ringState.lastSyncAt,
        activeWearableBinding.address,
        activeWearableBinding.vendor,
    ) {
        rdiViewModel.refresh(selectedPeriod.days.toInt())
    }

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
            refreshState = refreshState.reduce(
                AttributionRefreshEvent.Succeeded(
                    requestId = requestId,
                    data = AttributionRemoteData(history = history, pias = null),
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
            rdiSummary = rdiPeriodSummary,
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
            factorValues = evaluation?.factorValues?.takeIf { it.isNotEmpty() }
                ?: attributionFactorValues(attributionProfile),
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
    val rhiImprovement = AttributionUiMapper.mapRhiImprovement(
        summary = rhiPeriodSummary,
        period = selectedPeriod,
        today = LocalDate.now(),
    )

    AttributionContent(
        state = uiState,
        rhiImprovement = rhiImprovement,
        rhiError = rhiRefreshError,
        feedbackViewModel = feedbackViewModel,
        dietEntryState = dietEntryState,
        onSaveDietRecord = dietEntryViewModel::save,
        onClearDietMessage = dietEntryViewModel::clearMessage,
        onPeriodSelected = { selectedPeriod = it },
        onRetry = {
            retryKey += 1
            rhiViewModel.refresh(
                selectedPeriod.days.toInt(),
                AttributionDataProvenance.trustedProfile(ringState.patientMvp),
            )
            rdiViewModel.refresh(selectedPeriod.days.toInt())
            behaviorViewModel.refreshToday()
        },
        rdiDisplayData = rdiDisplayData,
        behaviorRecords = behaviorState.records,
    )
}

@Composable
private fun AttributionContent(
    state: AttributionUiState,
    rhiImprovement: AttributionRhiImprovementUi,
    rhiError: String?,
    feedbackViewModel: InterventionFeedbackViewModel,
    dietEntryState: DietEntryUiState,
    onSaveDietRecord: (com.rehealth.genie.diet.DietRecordDraft) -> Unit,
    onClearDietMessage: () -> Unit,
    onPeriodSelected: (AttributionPeriod) -> Unit,
    onRetry: () -> Unit,
    rdiDisplayData: RdiDisplayData?,
    behaviorRecords: List<BehaviorRecordDto>,
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
        item { AttributionSummaryCard(state, rhiImprovement, rhiError) }
        item {
            AttributionRiskTrendCard(
                period = state.period,
                history = state.selectedHistory,
                scenario = state.rdiScenario,
                impact = rdiDisplayData,
            )
        }
        item {
            AttributionDietCard(
                state = dietEntryState,
                onSave = onSaveDietRecord,
                onClearMessage = onClearDietMessage,
            )
        }
        item { AttributionActivityCard(state.activity, behaviorRecords) }
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
private fun AttributionSummaryCard(
    state: AttributionUiState,
    rhiImprovement: AttributionRhiImprovementUi,
    rhiError: String?,
) {
    AttributionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("健康改善得分", color = Palette.TextPrimary, style = Type.CardTitle)
                Text(
                    rhiImprovement.improvementText,
                    color = when {
                        rhiImprovement.improvementPoints == null -> Palette.TextSecondary
                        rhiImprovement.improvementPoints >= 0.0 -> Palette.Accent
                        else -> Palette.ImprovementWorsening
                    },
                    style = Type.SummaryScore,
                    modifier = Modifier.padding(top = Dimensions.SummaryScoreTop),
                )
                Text(
                    rhiError ?: rhiImprovement.comparisonText,
                    color = Palette.TextSecondary,
                    style = Type.Detail,
                    modifier = Modifier.padding(top = Dimensions.SummarySupportingTop),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("RDI-16 风险指数", color = Palette.TextSecondary, style = Type.Body)
                Text(
                    state.currentRiskText,
                    color = Palette.TextPrimary,
                    style = Type.RiskScore,
                    modifier = Modifier.padding(top = Dimensions.PageSubtitleTop),
                )
                Text(
                    if (state.selectedHistory.isEmpty()) {
                        "本周期暂无有效记录"
                    } else {
                        "${state.period.selectorLabel} · ${state.selectedHistory.size} 个有效日"
                    },
                    color = Palette.Accent,
                    style = Type.Detail,
                    modifier = Modifier.padding(top = Dimensions.SummarySupportingTop),
                )
            }
        }
        if (rhiImprovement.selectedHistory.size >= 2) {
            RhiHistoryChart(
                history = rhiImprovement.selectedHistory,
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
                Text("动态心健康指数趋势正在积累", color = Palette.TextSecondary, style = Type.Body)
            }
        }
    }
}

@Composable
private fun AttributionRiskTrendCard(
    period: AttributionPeriod,
    history: List<AttributionHistoryPoint>,
    scenario: AttributionRdiScenarioUi,
    impact: RdiDisplayData?,
) {
    AttributionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("个人风险趋势", color = Palette.TextPrimary, style = Type.CardTitle)
                Text(
                    "RDI-16 · ${period.selectorLabel}每日风险变化",
                    color = Palette.TextSecondary,
                    style = Type.Detail,
                )
            }
            Text(
                if (history.isEmpty()) "积累中" else "${history.size} 个有效日",
                color = Palette.Accent,
                style = Type.Detail,
                modifier = Modifier.clip(CircleShape).background(Palette.AccentSoft)
                    .padding(
                        horizontal = Dimensions.StatusHorizontalPadding,
                        vertical = Dimensions.StatusVerticalPadding,
                ),
            )
        }
        AttributionRdiTrendContent(history, scenario)
        RdiImpactSection(impact)
        Text(
            "情景模拟基于近期健康状态，不代表未来疾病发生概率。",
            color = Palette.TextSecondary,
            style = Type.Micro,
            modifier = Modifier.padding(top = Dimensions.AttTop),
        )
    }
}

@Composable
private fun AttributionRdiTrendContent(
    history: List<AttributionHistoryPoint>,
    scenario: AttributionRdiScenarioUi,
) {
    if (history.size >= 2) {
        AttributionRdiHistoryChart(
            history = history,
            modifier = Modifier.fillMaxWidth().height(Dimensions.ForecastChartHeight)
                .padding(top = Dimensions.ForecastChartTop),
        )
    } else {
        AttributionCompactMessage("RDI-16 历史不足 2 个有效日，暂不能绘制变化曲线。")
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = Dimensions.LegendTop)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.LegendGap),
        ) {
            AttributionTrendLegend(
                color = Palette.ForecastActual,
                label = "已发生的 RDI-16 风险指数",
                modifier = Modifier.weight(1f),
            )
            AttributionTrendLegend(
                color = Palette.ForecastNoAction,
                label = "维持现状（暂不可用）",
                dashed = true,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.LegendGap),
        ) {
            AttributionTrendLegend(
                color = Palette.Accent,
                label = "执行计划（暂不可用）",
                dashed = true,
                modifier = Modifier.weight(1f),
            )
            AttributionTrendLegend(
                color = Palette.ForecastInterval,
                label = "95% 区间（暂不可用）",
                interval = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(top = Dimensions.ForecastMetricsTop),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.ForecastMetricGap),
    ) {
        AttributionForecastMetric(
            label = "维持现状",
            value = scenario.noActionScore.asRdiScenarioValue(),
            color = Palette.ForecastNoAction,
            modifier = Modifier.weight(1f),
        )
        AttributionForecastMetric(
            label = "执行计划",
            value = scenario.withPlanScore.asRdiScenarioValue(),
            color = Palette.Accent,
            modifier = Modifier.weight(1f),
        )
        AttributionForecastMetric(
            label = "预计降低",
            value = scenario.expectedReduction.asRdiScenarioValue(),
            color = Palette.ForecastReduction,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun RdiImpactSection(data: RdiDisplayData?) {
    HorizontalDivider(
        color = Palette.Border,
        modifier = Modifier.padding(top = Dimensions.FactorGroupTop),
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Dimensions.FactorGroupTop),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "本周期影响最大的 3 项",
            color = Palette.TextPrimary,
            style = Type.CardTitle,
            modifier = Modifier.weight(1f),
        )
        Text(
            data?.let {
                "可信度 ${String.format(Locale.US, "%.0f%%", it.confidence * 100.0)}"
            } ?: "正在读取",
            color = Palette.TextSecondary,
            style = Type.Micro,
        )
    }
        val contributions = data?.topContributions.orEmpty()
        if (contributions.isNotEmpty()) {
            contributions.forEachIndexed { index, contribution ->
                RdiImpactContributionRow(index + 1, contribution)
            }
        } else {
            Text(
                "正在积累更多有效数据",
                color = Palette.TextSecondary,
                style = Type.Detail,
                modifier = Modifier.padding(top = Dimensions.FactorRowTop),
            )
        }
}

@Composable
private fun RdiImpactContributionRow(
    rank: Int,
    contribution: RdiContributionEntity,
) {
    val points = contribution.finalPoints
    val improvesRisk = points < 0.0
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Dimensions.FactorRowTop),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(Dimensions.FactorRankSize).clip(CircleShape).background(Palette.AccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                rank.toString(),
                color = Palette.Accent,
                style = Type.Selector,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            contribution.evidenceText.ifBlank { contribution.factorCode },
            color = Palette.TextPrimary,
            style = Type.FactorTitle,
            modifier = Modifier.weight(1f).padding(start = Dimensions.FactorContentGap),
        )
        Text(
            String.format(Locale.US, "%s %+.1f 分", if (improvesRisk) "↓" else "↑", points),
            color = if (improvesRisk) Palette.Accent else Palette.ContributionRisk,
            style = Type.FactorScore,
            modifier = Modifier.padding(start = Dimensions.LegendLabelGap),
        )
    }
}

internal fun rdiImpactStatusLabel(status: String?): String = when (status?.lowercase(Locale.US)) {
    "confirmed" -> "已建立个人基线"
    "provisional" -> "初步结果"
    "accumulating" -> "基线建立中"
    null -> "计算中"
    else -> "状态待确认"
}

@Composable
private fun AttributionActivityCard(
    activity: AttributionActivityUi?,
    behaviorRecords: List<BehaviorRecordDto>,
) {
    AttributionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("今日行为记录", color = Palette.TextPrimary, style = Type.CardTitle)
                Text("戒指活动与拍照生成的饮食/OCR记录", color = Palette.TextSecondary, style = Type.Detail)
            }
            Text(
                if (activity == null && behaviorRecords.isEmpty()) "待记录" else "已记录",
                color = Palette.Accent,
                style = Type.Detail,
                modifier = Modifier.clip(CircleShape).background(Palette.AccentSoft)
                    .padding(
                        horizontal = Dimensions.StatusHorizontalPadding,
                        vertical = Dimensions.StatusVerticalPadding,
                    ),
            )
        }
        if (activity == null && behaviorRecords.isEmpty()) {
            AttributionCompactMessage("暂无可展示的真实活动记录；同步 MR11 戒指后自动更新。")
        } else if (activity != null) {
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
        behaviorRecords.take(5).forEach { record ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Dimensions.ActivityContentTop)
                    .clip(RoundedCornerShape(Dimensions.ActivityContentRadius))
                    .background(Palette.ActivitySurface).padding(Dimensions.ActivityContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(Dimensions.ActivityBadgeSize).clip(CircleShape).background(Palette.ActivityBadge),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (record.category == "FOOD") "餐" else "文",
                        color = Palette.ActivityAccent,
                        style = Type.ActivityGlyph,
                    )
                }
                Column(Modifier.weight(1f).padding(start = Dimensions.ActivityTextGap)) {
                    Text(
                        record.title ?: "照片记录",
                        color = Palette.TextPrimary,
                        style = Type.Selector,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        record.summary ?: "已完成图像识别",
                        color = Palette.TextSecondary,
                        style = Type.Micro,
                        maxLines = 2,
                    )
                }
                record.caloriesKcal?.let {
                    Text(
                        "约 ${it.toInt()} kcal",
                        color = Palette.Accent,
                        style = Type.Micro,
                        textAlign = TextAlign.End,
                    )
                }
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
    val evidence = attributionFactorEvidence(factor)
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
                    evidence.explanation,
                    color = Palette.TextPrimary,
                    style = Type.Detail,
                )
                Text(
                    "建议：${evidence.recommendation}",
                    color = Palette.Accent,
                    style = Type.Detail,
                    fontWeight = FontWeight.Medium,
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
private fun AttributionTrendLegend(
    color: Color,
    label: String,
    modifier: Modifier,
    dashed: Boolean = false,
    interval: Boolean = false,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(20.dp).height(8.dp)) {
            if (interval) {
                drawRect(color.copy(alpha = 0.35f))
            } else {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = if (dashed) {
                        PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                    } else {
                        null
                    },
                )
            }
        }
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
private fun RhiHistoryChart(history: List<RhiDailyScore>, modifier: Modifier) {
    Canvas(modifier) {
        if (history.size < 2) return@Canvas
        val values = history.map { it.score.toFloat() }
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
private fun AttributionRdiHistoryChart(
    history: List<AttributionHistoryPoint>,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val values = history.map(AttributionHistoryPoint::riskScore)
            .filter { it.isFinite() && it in 0.0..1.0 }
        if (values.size < 2) return@Canvas
        val rawMinimum = values.minOrNull() ?: return@Canvas
        val rawMaximum = values.maxOrNull() ?: return@Canvas
        val padding = ((rawMaximum - rawMinimum) * 0.15).coerceAtLeast(0.01)
        val minimum = (rawMinimum - padding).coerceAtLeast(0.0)
        val maximum = (rawMaximum + padding).coerceAtMost(1.0).coerceAtLeast(minimum + 0.01)
        val left = Dimensions.ForecastChartInset.toPx()
        val right = size.width - Dimensions.ForecastChartInset.toPx()
        val top = Dimensions.ForecastChartInset.toPx()
        val bottom = size.height - Dimensions.ForecastChartInset.toPx()
        repeat(4) { index ->
            val y = top + (bottom - top) * index / 3f
            drawLine(
                Palette.ChartGrid,
                Offset(left, y),
                Offset(right, y),
                Dimensions.ForecastGridStroke.toPx(),
            )
        }
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = left + (right - left) * index / values.lastIndex
            val y = bottom - ((value - minimum) / (maximum - minimum)).toFloat() * (bottom - top)
            val point = Offset(x, y.coerceIn(top, bottom))
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            drawCircle(
                Palette.ForecastActual,
                radius = Dimensions.ForecastDotRadius.toPx(),
                center = point,
            )
        }
        drawPath(
            path,
            Palette.ForecastActual,
            style = Stroke(width = Dimensions.ForecastPlanStroke.toPx(), cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun AttributionForecastChart(
    history: List<AttributionHistoryPoint>,
    forecast: AttributionForecastUi,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val count = minOf(forecast.noAction.size, forecast.withPlan.size)
        if (count < 2) return@Canvas
        val actualValues = history.map(AttributionHistoryPoint::riskScore)
            .filter { it.isFinite() && it in 0.0..1.0 }
        val allValues = buildList {
            addAll(actualValues)
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
        val predictionLeft = if (actualValues.isEmpty()) left else left + (right - left) * 0.34f
        fun point(index: Int, value: Double, startX: Float, endX: Float, pointCount: Int): Offset {
            val x = if (pointCount <= 1) endX else startX + (endX - startX) * index / (pointCount - 1)
            val y = bottom - ((value - minimum) / (maximum - minimum)).toFloat() * (bottom - top)
            return Offset(x, y.coerceIn(top, bottom))
        }
        fun forecastPoint(index: Int, value: Double): Offset =
            point(index, value, predictionLeft, right, count)
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
                val position = forecastPoint(index, value)
                if (index == 0) confidencePath.moveTo(position.x, position.y) else confidencePath.lineTo(position.x, position.y)
            }
            forecast.ciLower.take(count).asReversed().forEachIndexed { reverseIndex, value ->
                val index = count - reverseIndex - 1
                val position = forecastPoint(index, value)
                confidencePath.lineTo(position.x, position.y)
            }
            confidencePath.close()
            drawPath(confidencePath, Palette.ForecastInterval.copy(alpha = Opacity.ForecastInterval))
        }
        fun drawForecastSeries(values: List<Double>, color: Color, width: Float) {
            val path = Path()
            values.take(count).forEachIndexed { index, value ->
                val position = forecastPoint(index, value)
                if (index == 0) path.moveTo(position.x, position.y) else path.lineTo(position.x, position.y)
            }
            drawPath(
                path,
                color,
                style = Stroke(
                    width = width,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(8.dp.toPx(), 5.dp.toPx()),
                    ),
                ),
            )
        }
        if (actualValues.isNotEmpty()) {
            val actualPath = Path()
            actualValues.forEachIndexed { index, value ->
                val position = point(index, value, left, predictionLeft, actualValues.size)
                if (index == 0) {
                    actualPath.moveTo(position.x, position.y)
                } else {
                    actualPath.lineTo(position.x, position.y)
                }
                drawCircle(
                    Palette.ForecastActual,
                    radius = Dimensions.ForecastDotRadius.toPx(),
                    center = position,
                )
            }
            drawPath(
                actualPath,
                Palette.ForecastActual,
                style = Stroke(width = Dimensions.ForecastPlanStroke.toPx(), cap = StrokeCap.Round),
            )
        }
        drawForecastSeries(
            forecast.noAction,
            Palette.ForecastNoAction,
            Dimensions.ForecastNoActionStroke.toPx(),
        )
        drawForecastSeries(
            forecast.withPlan,
            Palette.Accent,
            Dimensions.ForecastPlanStroke.toPx(),
        )
    }
}

private fun attributionFactorValues(profile: PatientProfilePayload?): Map<String, String> = buildMap {
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
}

private fun Boolean.asYesNo(): String = if (this) "是" else "否"

private fun Boolean.asHistory(): String = if (this) "有" else "无"

private fun Double?.asRiskIndex(): String = this?.let {
    String.format(Locale.US, "%.1f", it * 100.0)
} ?: "--"

private fun Double?.asRiskIndexDelta(): String = this?.let {
    String.format(Locale.US, "%.1f", it * 100.0)
} ?: "--"

private fun Double?.asRdiScenarioValue(): String = this?.let {
    String.format(Locale.US, "%.1f", it)
} ?: "暂不可用"

private fun riskLevelLabel(level: String?): String = when (level?.lowercase()) {
    "low" -> "低风险"
    "moderate", "medium" -> "中等风险"
    "high" -> "高风险"
    "very_high", "very high" -> "极高风险"
    null -> "等待已确认评估"
    else -> level
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
