package com.rehealth.genie.ui

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.R
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ring.RingViewModel
import com.rehealth.genie.ring.PeriodAggregate
import com.rehealth.genie.ui.theme.Canvas
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted
import java.util.Locale

private data class RingMetricUi(
    val type: RingMetricType,
    val title: String,
    val value: String,
    val unit: String,
    val status: String,
    val icon: ImageVector,
    val color: Color,
    val manualMeasure: Boolean = false,
    val actionLabel: String = "测量",
    val measuringLabel: String = "测量中",
)

@Composable
internal fun DataScreen(
    state: RingUiState,
    ringViewModel: RingViewModel,
    canonicalRiskStatus: androidx.compose.runtime.State<RemoteFeatureEvaluateStatus?>,
    onMeasure: (RingMetricType) -> Unit,
) {
    var selectedPeriod by remember { mutableIntStateOf(1) }
    // 真实周期聚合：切换 今日/7天/30天/90天 时从本地 Room 历史重新计算
    var aggregate by remember { mutableStateOf<PeriodAggregate?>(null) }
    LaunchedEffect(selectedPeriod) {
        val windowDays = when (selectedPeriod) { 0 -> 0; 1 -> 7; 2 -> 30; 3 -> 90; else -> 7 }
        aggregate = ringViewModel.loadPeriodAggregate(windowDays)
    }

    fun measurement(type: RingMetricType): String {
        val record = state.measurements[type] ?: return "--"
        return if (type == RingMetricType.BLOOD_PRESSURE) {
            "${record.primaryValue.toInt()}/${record.secondaryValue?.toInt() ?: "--"}"
        } else if (type == RingMetricType.TEMPERATURE) {
            String.format(Locale.getDefault(), "%.1f", record.primaryValue)
        } else {
            record.primaryValue.toInt().toString()
        }
    }
    val periodDays = listOf(0, 7, 30, 90)[selectedPeriod]
    val periodLabel = if (periodDays == 0) "今日" else "近 $periodDays 天"
    val hrText = aggregate?.avgHeartRate?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: measurement(RingMetricType.HEART_RATE)
    val spo2Text = aggregate?.avgSpo2?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: measurement(RingMetricType.BLOOD_OXYGEN)
    val bpText = aggregate?.let { agg ->
        val s = agg.avgSbp?.toInt()
        val d = agg.avgDbp?.toInt()
        if (s != null && d != null) "$s/$d" else null
    } ?: measurement(RingMetricType.BLOOD_PRESSURE)
    val tempText = aggregate?.avgTemp?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: measurement(RingMetricType.TEMPERATURE)
    val sleepValue = aggregate?.avgSleepMinutes?.toInt()?.let { "${it / 60}h${it % 60}m" } ?: run {
        val m = state.sleep?.let { (it.endedAt - it.startedAt) / 60_000 }
        m?.let { "${it / 60}h${it % 60}m" } ?: "--"
    }
    val stepsText = aggregate?.totalSteps?.let { if (it > 0) it.toString() else null } ?: measurement(RingMetricType.STEPS)
    val vitalMetrics = listOf(
        RingMetricUi(RingMetricType.HEART_RATE, "心率", hrText, "bpm", periodLabel, Icons.Outlined.FavoriteBorder, Color(0xFFFF6078), manualMeasure = true),
        RingMetricUi(RingMetricType.BLOOD_OXYGEN, "血氧", spo2Text, "%", periodLabel, Icons.Outlined.DataUsage, Color(0xFF148BFF), manualMeasure = true),
        RingMetricUi(RingMetricType.BLOOD_PRESSURE, "血压", bpText, "mmHg", periodLabel, Icons.Outlined.FavoriteBorder, Color(0xFF8B63F6), manualMeasure = true),
        RingMetricUi(RingMetricType.TEMPERATURE, "体温", tempText, "°C", "定时采集", Icons.Outlined.Assessment, Color(0xFFFF8A32), manualMeasure = true, actionLabel = "开启", measuringLabel = "采集中"),
    )
    val dailyMetrics = listOf(
        RingMetricUi(RingMetricType.SLEEP, "睡眠", sleepValue, "", periodLabel, Icons.Outlined.AutoAwesome, Color(0xFF9668EF)),
        RingMetricUi(RingMetricType.STEPS, "步数", stepsText, "步", periodLabel, Icons.Outlined.ShowChart, Color(0xFF20B77A)),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFFE9FBF6), Canvas),
                radius = 900f,
            ),
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            top = 32.dp,
            end = 16.dp,
            bottom = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("健康数据", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (periodDays == 0) "今日身体状态" else "${periodLabel}身体状态总览",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .border(1.dp, Ink, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Assessment, "数据概览", tint = Ink, modifier = Modifier.size(20.dp))
                }
            }
        }
        item {
            PeriodSelector(
                labels = listOf("今日", "7 天", "30 天", "90 天"),
                selected = selectedPeriod,
                onSelected = { selectedPeriod = it },
            )
        }
        item {
            RiskScoreCard(canonicalRiskStatus)
        }
        if (state.message != null || state.isSyncing) {
            item {
                DataStatusCard(state, canonicalRiskStatus)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(178.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HealthScoreCard(Modifier.weight(1f))
                SmartRingOverviewCard(state, Modifier.weight(1f))
            }
        }
        item {
            DashboardSectionHeader(Icons.Outlined.FavoriteBorder, "生命体征")
        }
        item {
            MetricGrid(
                metrics = vitalMetrics,
                measuringMetric = state.measuringMetric,
                onMeasure = onMeasure,
                measureEnabled = !state.isSyncing,
            )
        }
        item {
            DashboardSectionHeader(Icons.Outlined.Timeline, "睡眠与活动")
        }
        item {
            MetricGrid(dailyMetrics)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFF4FFFB), Color(0xFFE3F9F2))))
                    .border(1.dp, Color(0xFFCDEBE2), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text("健康洞察 · AI 提醒", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "当前为智能戒指采集数据。血压、血氧等结果仅用于健康管理参考。",
                        color = Muted,
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = Mint, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PeriodSelector(labels: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
            .background(Color.White.copy(alpha = 0.88f))
            .border(1.dp, Color(0xFFD7E5E1), RoundedCornerShape(17.dp))
            .padding(3.dp),
    ) {
        labels.forEachIndexed { index, label ->
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(
                        if (index == selected) {
                            Brush.horizontalGradient(listOf(Color(0xFF0E9E8C), Color(0xFF11D7B0)))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        },
                    )
                    .clickable { onSelected(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (index == selected) Color.White else Muted,
                    fontSize = 13.sp,
                    fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun DataStatusCard(
    state: RingUiState,
    canonicalRiskStatus: androidx.compose.runtime.State<RemoteFeatureEvaluateStatus?>,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.dp, Color(0xFFD7E5E1), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            state.message ?: if (state.isSyncing) "正在处理戒指数据" else "",
            color = Ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (state.isSyncing) {
            LinearProgressIndicator(
                progress = { state.syncProgress / 100f },
                modifier = Modifier.fillMaxWidth().height(5.dp).padding(top = 7.dp),
                color = Mint,
                trackColor = MintSoft,
            )
        }
        val risk = canonicalRiskStatus.value
        if (risk != null) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(16.dp))
                Text(
                    "规范风险 ${risk.riskLevel.riskLevelLabel()} · ${risk.riskScore.riskScoreLabel()} · ${risk.modeLabel}",
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun RiskScoreCard(
    canonicalRiskStatus: androidx.compose.runtime.State<RemoteFeatureEvaluateStatus?>,
) {
    val current = canonicalRiskStatus.value
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color.White, Color(0xFFEAF8F4))))
            .border(1.dp, Color(0xFFD7E5E1), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("今日风险分", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                current?.summary ?: "正在从本机特征生成云端风险参考",
                color = Muted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(current?.riskScore.riskScoreLabel(), color = Mint, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(current?.riskLevel.riskLevelLabel(), color = Muted, fontSize = 10.sp)
            Text(current?.modeLabel ?: "评估中", color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun HealthScoreCard(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 11.dp.toPx()
                drawCircle(Color(0xFFD5F1E9), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFF0DA47C), Color(0xFF13D4A7), Color(0xFF0DA47C)),
                    ),
                    startAngle = -90f,
                    sweepAngle = 313f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round),
                )
                drawCircle(
                    color = Color(0xFFBEEBDD),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
                    radius = size.minDimension / 2 - 2.dp.toPx(),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("健康指数", color = Muted, fontSize = 10.sp)
                Text("87", color = Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                Text(
                    "良好",
                    color = Mint,
                    fontSize = 10.sp,
                    modifier = Modifier.clip(CircleShape).background(MintSoft)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        Text("身体状态良好，继续保持 ›", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SmartRingOverviewCard(state: RingUiState, modifier: Modifier) {
    val hasConnectedDevice = state.connectedDevice != null
    val hasLocalData = state.measurements.isNotEmpty() || state.sleep != null
    Column(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .border(1.dp, Color(0xFFD6E5E1), RoundedCornerShape(20.dp))
            .padding(11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Text("睿禾智能戒指", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
            Box(Modifier.size(16.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
        Text(
            when {
                hasConnectedDevice -> "设备已连接"
                hasLocalData -> "有历史数据，需重新连接"
                else -> "设备未连接"
            },
            color = Muted,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.smart_ring),
                contentDescription = "睿禾智能戒指",
                modifier = Modifier.fillMaxWidth().height(67.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                .background(Color.White).border(1.dp, Line, RoundedCornerShape(13.dp))
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Timeline, null, tint = Mint, modifier = Modifier.size(17.dp))
            Text(
                if (state.lastSyncAt == null) "待同步" else "已同步",
                color = Ink,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 7.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                state.lastSyncAt?.let { "上次同步 ${formatSyncTime(it)}" } ?: "上次同步 --:--",
                color = Muted,
                fontSize = 8.sp,
            )
        }
    }
}

@Composable
private fun DashboardSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Mint, modifier = Modifier.size(22.dp))
        }
        Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.dp))
        Spacer(Modifier.weight(1f))
        Text("查看全部  ›", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun MetricGrid(
    metrics: List<RingMetricUi>,
    measuringMetric: RingMetricType? = null,
    onMeasure: (RingMetricType) -> Unit = {},
    measureEnabled: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowMetrics.forEach { metric ->
                    DashboardMetricCard(
                        metric = metric,
                        measuring = measuringMetric == metric.type,
                        measureEnabled = measureEnabled,
                        onMeasure = onMeasure,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
    metric: RingMetricUi,
    measuring: Boolean,
    measureEnabled: Boolean,
    onMeasure: (RingMetricType) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val startMeasure = {
        val toast = if (metric.type == RingMetricType.TEMPERATURE) {
            "已开启体温定时采集，稍后会读取历史体温"
        } else {
            "开始测量${metric.title}，请保持戒指佩戴稳定"
        }
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        onMeasure(metric.type)
    }
    Column(
        modifier = modifier.height(if (metric.manualMeasure) 116.dp else 102.dp).clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .border(1.dp, Color(0xFFE1E9E7), RoundedCornerShape(18.dp))
            .clickable(
                enabled = metric.manualMeasure && measureEnabled && !measuring,
                onClick = startMeasure,
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(metric.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(metric.icon, null, tint = metric.color, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(metric.title, color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        metric.value,
                        color = Ink,
                        fontSize = if (metric.type == RingMetricType.BLOOD_PRESSURE) 12.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    if (metric.unit.isNotEmpty()) {
                        Text(
                            metric.unit,
                            color = Muted,
                            fontSize = if (metric.type == RingMetricType.BLOOD_PRESSURE) 5.sp else 7.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                        )
                    }
                }
            }
            if (!metric.manualMeasure) {
                MiniChart(
                    points = if (metric.type == RingMetricType.SLEEP || metric.type == RingMetricType.STEPS) {
                        listOf(.25f, .72f, .38f, .82f, .52f, .75f)
                    } else {
                        listOf(.35f, .65f, .48f, .72f, .28f, .55f)
                    },
                    color = metric.color,
                    modifier = Modifier.width(if (metric.type == RingMetricType.BLOOD_PRESSURE) 16.dp else 24.dp).height(32.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Mint))
            Text(metric.status, color = Muted, fontSize = 8.sp, modifier = Modifier.padding(start = 5.dp))
            if (metric.manualMeasure) {
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (measuring) Mint.copy(alpha = 0.16f) else MintSoft)
                        .border(1.dp, Mint.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                        .clickable(enabled = measureEnabled && !measuring, onClick = startMeasure)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (measuring) metric.measuringLabel else metric.actionLabel,
                        color = Mint,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
@Composable
private fun ChartCard(title: String, value: String, color: Color, points: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(110.dp)) {
                Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                Text("平均", color = Muted, fontSize = 10.sp)
            }
            MiniChart(points, color, Modifier.weight(1f).height(62.dp))
        }
    }
}

@Composable
private fun MiniChart(points: List<Float>, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val step = size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = index * step
            val y = size.height * (1f - point)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color, radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}
