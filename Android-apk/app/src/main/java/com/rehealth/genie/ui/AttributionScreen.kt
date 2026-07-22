package com.rehealth.genie.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rehealth.genie.BuildConfig
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.phm.AttributionHistoryPoint
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ui.theme.Canvas as AppCanvas
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted
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
            factorValues = attributionFactorValues(ringState),
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
        modifier = Modifier.fillMaxSize().background(AppCanvas).statusBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("健康归因", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "个人改善路径 · 近 ${state.period.selectorLabel}",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                IconButton(onClick = onRetry) {
                    Icon(Icons.Outlined.Refresh, "刷新归因数据", tint = Mint)
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
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MintSoft).padding(14.dp),
            ) {
                Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(18.dp))
                Text(
                    "归因结果仅用于健康管理参考，不代表医学诊断，也不能替代医生建议。",
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(start = 8.dp),
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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color.White).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AttributionPeriod.entries.forEach { period ->
            val active = selected == period
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if (active) Mint else Color.Transparent)
                    .clickable { onSelected(period) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    period.selectorLabel,
                    color = if (active) Color.White else Muted,
                    fontSize = 12.sp,
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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
            .background(if (canRetry) Color(0xFFFFF4E4) else MintSoft)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Mint,
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(message, color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
        if (canRetry) {
            TextButton(onClick = onRetry) { Text("重试", color = Mint, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun AttributionSummaryCard(state: AttributionUiState) {
    AttributionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("健康改善", color = Ink, fontWeight = FontWeight.SemiBold)
                val improvementColor = when {
                    state.improvementPoints == null -> Muted
                    state.improvementPoints >= 0.0 -> Mint
                    else -> Color(0xFFE36B61)
                }
                Text(
                    if (state.improvementPoints == null) "--" else "${state.improvementText} 个百分点",
                    color = improvementColor,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    state.improvementMessage,
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("当前风险", color = Muted, fontSize = 11.sp)
                Text(
                    state.currentRiskText,
                    color = Ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    riskLevelLabel(state.riskLevel),
                    color = Mint,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        if (state.selectedHistory.size >= 2) {
            AttributionHistoryChart(
                history = state.selectedHistory,
                modifier = Modifier.fillMaxWidth().height(76.dp).padding(top = 10.dp),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(top = 10.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xFFF7FBFA)),
                contentAlignment = Alignment.Center,
            ) {
                Text("已确认风险趋势正在积累", color = Muted, fontSize = 11.sp)
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
                Text("个人风险趋势", color = Ink, fontWeight = FontWeight.SemiBold)
                Text("PIAS 固定 30 天预测 · 执行计划与维持现状", color = Muted, fontSize = 10.sp)
            }
            Text(
                when (pias) {
                    is AttributionPiasUiState.Ready -> trendLabel(pias.trend)
                    is AttributionPiasUiState.Accumulating -> "积累中"
                    is AttributionPiasUiState.Failed -> "暂不可用"
                    AttributionPiasUiState.Empty -> "待生成"
                    AttributionPiasUiState.Loading -> "计算中"
                },
                color = Mint,
                fontSize = 10.sp,
                modifier = Modifier.clip(CircleShape).background(MintSoft)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
        when (pias) {
            AttributionPiasUiState.Empty -> AttributionCompactMessage("完成已确认风险评估后生成 30 天预测。")
            AttributionPiasUiState.Loading -> AttributionLoadingMessage("正在请求 PIAS 个人归因…")
            is AttributionPiasUiState.Failed -> {
                AttributionCompactMessage(pias.message)
                TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                    Text("重新计算", color = Mint, fontSize = 11.sp)
                }
            }
            is AttributionPiasUiState.Accumulating -> {
                AttributionCompactMessage(
                    "已有 ${pias.historyDays} 天记录，至少需要 ${pias.minHistoryDays} 天才能生成完整预测。",
                )
                LinearProgressIndicator(
                    progress = { (pias.historyDays.toFloat() / pias.minHistoryDays.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = Mint,
                    trackColor = MintSoft,
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
            modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 12.dp),
        )
    } else {
        AttributionCompactMessage("PIAS 已完成分析，本次未返回可绘制的预测序列。")
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AttributionLegend(Color(0xFFF28B82), "维持现状")
        AttributionLegend(Mint, "执行计划")
        AttributionLegend(Color(0xFF9BAFAA), "95% 参考区间")
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AttributionForecastMetric(
            label = "维持现状",
            value = pias.forecast.d30NoAction.asPercent(),
            color = Color(0xFFF28B82),
            modifier = Modifier.weight(1f),
        )
        AttributionForecastMetric(
            label = "执行计划",
            value = pias.forecast.d30WithPlan.asPercent(),
            color = Mint,
            modifier = Modifier.weight(1f),
        )
        AttributionForecastMetric(
            label = "预计降低",
            value = pias.forecast.riskReduction.asPercent(signed = true),
            color = Color(0xFF4E7BFF),
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
    Text(attText, color = Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 11.dp))
}

@Composable
private fun AttributionActivityCard(activity: AttributionActivityUi?) {
    AttributionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("今日行为记录", color = Ink, fontWeight = FontWeight.SemiBold)
                Text("本机 Room 中最近一次戒指活动", color = Muted, fontSize = 10.sp)
            }
            Text(
                if (activity == null) "待记录" else "已记录",
                color = Mint,
                fontSize = 10.sp,
                modifier = Modifier.clip(CircleShape).background(MintSoft)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
        if (activity == null) {
            AttributionCompactMessage("暂无可展示的真实活动记录；同步 MR11 戒指后自动更新。")
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF8EE)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFFE7C6)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("行", color = Color(0xFFE88625), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(activityTypeLabel(activity.activityType), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(formatActivityTime(activity.startedAt), color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                }
                Text(activity.provenanceLabel, color = Mint, fontSize = 9.sp, textAlign = TextAlign.End)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    val maxContribution = groups.flatMap { it.factors }
        .mapNotNull { it.contribution }
        .maxOfOrNull { kotlin.math.abs(it) }
        ?.takeIf { it > 0.0 }
        ?: 1.0
    AttributionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("贡献因素", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("16 项 · 点击查看依据", color = Muted, fontSize = 10.sp)
        }
        groups.forEach { group ->
            Text(
                group.title,
                color = Mint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 14.dp),
            )
            group.factors.forEach { factor ->
                AttributionFactorRow(
                    factor = factor,
                    maxContribution = maxContribution,
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
    factor: AttributionFactorUi,
    maxContribution: Double,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val contribution = factor.contribution
    val fraction by animateFloatAsState(
        targetValue = contribution?.let { (kotlin.math.abs(it) / maxContribution).toFloat().coerceIn(0f, 1f) } ?: 0f,
        animationSpec = tween(650),
        label = "attribution-${factor.key}",
    )
    val contributionColor = when {
        contribution == null -> Muted
        contribution >= 0.0 -> Color(0xFFE39A22)
        else -> Mint
    }
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(factor.label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(factor.value ?: "当前值未提供", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    contribution?.let { String.format(Locale.US, "%+.3f", it) } ?: "未提供",
                    color = contributionColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(if (expanded) "收起" else "详情", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(5.dp).clip(CircleShape),
            color = contributionColor,
            trackColor = Color(0xFFF0F5F3),
        )
        if (expanded) {
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF7FBFA)).padding(10.dp),
            ) {
                Text(
                    if (contribution == null) {
                        "本次已确认模型结果没有提供该项贡献值，保留该行以维持完整 Core16 输入视图。"
                    } else {
                        "模型返回贡献值 ${String.format(Locale.US, "%+.3f", contribution)}；正值表示风险方向，负值表示保护方向。"
                    },
                    color = Ink,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                )
                Text("数据来源：已确认云端风险评估与本机健康档案。", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
        HorizontalDivider(color = Line, modifier = Modifier.padding(top = 10.dp))
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
                Text("个性化干预计划", color = Ink, fontWeight = FontWeight.SemiBold)
                Text("仅展示带真实服务端 ID 的计划", color = Muted, fontSize = 10.sp)
            }
            Text("${interventions.size} 项", color = Mint, fontSize = 11.sp)
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
                    Text(message, color = Mint, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            Button(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (expanded) MintSoft else Mint,
                    contentColor = if (expanded) Mint else Color.White,
                ),
            ) {
                Text(if (expanded) "收起干预计划" else "查看详细干预计划")
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
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(27.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
                Text(number.toString().padStart(2, '0'), color = Mint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(start = 9.dp)) {
                Text(intervention.title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                intervention.action?.let { Text(it, color = Ink, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 3.dp)) }
                val detail = listOfNotNull(intervention.duration, intervention.reason).joinToString(" · ")
                if (detail.isNotBlank()) {
                    Text(detail, color = Muted, fontSize = 9.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 36.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
        HorizontalDivider(color = Line, modifier = Modifier.padding(top = 10.dp))
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
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) Mint else Color.White,
            contentColor = if (primary) Color.White else Mint,
        ),
    ) {
        if (primary) {
            Icon(Icons.Outlined.Check, null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
        } else if (label == "不适用") {
            Icon(Icons.Outlined.Close, null, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(label, fontSize = 9.sp)
    }
}

@Composable
private fun AttributionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun AttributionCompactMessage(message: String) {
    Text(
        message,
        color = Muted,
        fontSize = 11.sp,
        lineHeight = 17.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp)).background(Color(0xFFF7FBFA)).padding(11.dp),
    )
}

@Composable
private fun AttributionLoadingMessage(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp)).background(Color(0xFFF7FBFA)).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(17.dp), color = Mint, strokeWidth = 2.dp)
        Text(message, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 9.dp))
    }
}

@Composable
private fun AttributionActivityMetric(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFF8FBFA)).padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(label, color = Muted, fontSize = 8.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun AttributionLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, color = Muted, fontSize = 9.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun AttributionForecastMetric(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.08f))
            .padding(horizontal = 9.dp, vertical = 8.dp),
    ) {
        Text(label, color = Muted, fontSize = 9.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
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
        val top = 5.dp.toPx()
        val bottom = size.height - 5.dp.toPx()
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / values.lastIndex
            val y = bottom - ((value - minimum) / range) * (bottom - top)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(Mint, radius = 3.dp.toPx(), center = Offset(x, y))
        }
        drawPath(path, Mint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
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
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        fun point(index: Int, value: Double): Offset {
            val x = left + (right - left) * index / (count - 1)
            val y = bottom - ((value - minimum) / (maximum - minimum)).toFloat() * (bottom - top)
            return Offset(x, y.coerceIn(top, bottom))
        }
        repeat(4) { index ->
            val y = top + (bottom - top) * index / 3f
            drawLine(Color(0xFFE7F0ED), Offset(left, y), Offset(right, y), 1.dp.toPx())
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
            drawPath(confidencePath, Color(0xFF9BAFAA).copy(alpha = 0.16f))
        }
        fun drawSeries(values: List<Double>, color: Color, width: Float) {
            val path = Path()
            values.take(count).forEachIndexed { index, value ->
                val position = point(index, value)
                if (index == 0) path.moveTo(position.x, position.y) else path.lineTo(position.x, position.y)
                drawCircle(color, radius = 2.6.dp.toPx(), center = position)
            }
            drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round))
        }
        drawSeries(forecast.noAction, Color(0xFFF28B82), 2.dp.toPx())
        drawSeries(forecast.withPlan, Mint, 2.5.dp.toPx())
    }
}

private fun attributionFactorValues(state: RingUiState): Map<String, String> = buildMap {
    val profile = state.patientMvp?.profile
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
