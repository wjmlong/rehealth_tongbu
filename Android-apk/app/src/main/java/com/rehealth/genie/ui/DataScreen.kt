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
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rehealth.genie.R
import com.rehealth.genie.rhi.RhiPeriodAggregation
import com.rehealth.genie.rhi.RhiPeriodSummary
import com.rehealth.genie.ring.RingAcquisitionMode
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingFeatureType
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
import java.time.LocalDate
import java.time.ZoneId

private data class RingMetricUi(
    val type: RingMetricType,
    val title: String,
    val value: String,
    val unit: String,
    val status: String,
    val icon: ImageVector,
    val color: Color,
    val manualMeasure: Boolean = false,
    val showAction: Boolean = false,
    val showChart: Boolean = true,
    val actionLabel: String = "测量",
    val measuringLabel: String = "测量中",
)

internal const val DEFAULT_DATA_PERIOD_INDEX = 0
internal val DATA_PERIOD_OPTIONS = listOf(
    "今日" to 0,
    "7 天" to 7,
    "30 天" to 30,
    "90 天" to 90,
)

@Composable
internal fun DataScreen(
    state: RingUiState,
    ringViewModel: RingViewModel,
    canonicalRiskStatus: androidx.compose.runtime.State<RemoteFeatureEvaluateStatus?>,
    onMeasure: (RingMetricType) -> Unit,
    onSync: () -> Unit,
) {
    var selectedPeriod by remember { mutableIntStateOf(DEFAULT_DATA_PERIOD_INDEX) }
    val rhiViewModel: RhiViewModel = viewModel(factory = RhiViewModel.Factory(LocalContext.current))
    val rhiPeriodSummary by rhiViewModel.periodSummary.collectAsState()
    val rhiRefreshError by rhiViewModel.refreshError.collectAsState()
    var showBloodGlucoseCalibration by remember { mutableStateOf(false) }
    var showWomensHealthSetting by remember { mutableStateOf(false) }
    var showEcgDetail by remember { mutableStateOf(false) }
    var showEcgInstructionsOnOpen by remember { mutableStateOf(false) }
    var pendingMeasurementInstruction by remember { mutableStateOf<RingMetricType?>(null) }
    if (showEcgDetail) {
        EcgDetailScreen(
            state = state,
            showMeasurementInstructionsOnOpen = showEcgInstructionsOnOpen,
            onBack = {
                showEcgDetail = false
                showEcgInstructionsOnOpen = false
            },
            onMeasure = onMeasure,
        )
        return
    }
    // 真实周期聚合：切换 今日/7天/30天/90天 时从本地 Room 历史重新计算
    var aggregate by remember { mutableStateOf<PeriodAggregate?>(null) }
    LaunchedEffect(selectedPeriod, state.lastSyncAt, state.activity?.id, state.sleep?.id) {
        val windowDays = DATA_PERIOD_OPTIONS[selectedPeriod].second
        aggregate = ringViewModel.loadPeriodAggregate(windowDays)
    }
    val rhiPeriodDays = when (selectedPeriod) {
        2 -> 30
        3 -> 90
        else -> 7
    }
    LaunchedEffect(
        rhiPeriodDays,
        state.lastSyncAt,
        state.activity?.id,
        state.sleep?.id,
        state.patientMvp?.profile?.updatedAt,
    ) {
        rhiViewModel.refresh(rhiPeriodDays, AttributionDataProvenance.trustedProfile(state.patientMvp))
    }
    val currentRhi = rhiPeriodSummary?.takeIf { it.periodDays == rhiPeriodDays }
    val cloudMode = state.acquisitionMode == RingAcquisitionMode.CLOUD

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
    val periodDays = DATA_PERIOD_OPTIONS[selectedPeriod].second
    val periodLabel = if (periodDays == 0) "今日" else "近 $periodDays 天"
    val hrText = aggregate?.avgHeartRate?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: measurement(RingMetricType.HEART_RATE)
    val spo2Text = aggregate?.avgSpo2?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: measurement(RingMetricType.BLOOD_OXYGEN)
    val spo2PeriodStatus = aggregate?.minSpo2?.takeIf { cloudMode }?.let {
        "最低 ${String.format(Locale.getDefault(), "%.0f", it)}% · $periodLabel"
    } ?: periodLabel
    val bpText = aggregate?.let { agg ->
        val s = agg.avgSbp?.toInt()
        val d = agg.avgDbp?.toInt()
        if (s != null && d != null) "$s/$d" else null
    } ?: measurement(RingMetricType.BLOOD_PRESSURE)
    val hrvText = measurement(RingMetricType.HRV)
    val sleepValue = aggregate?.avgSleepMinutes?.toInt()?.let { "${it / 60}h${it % 60}m" } ?: run {
        val m = sleepDurationMinutes(state.sleep)
        m?.let { "${it / 60}h${it % 60}m" } ?: "--"
    }
    val stepsText = aggregate?.totalSteps?.let { if (it > 0) it.toString() else null }
        ?: state.activity?.steps?.takeIf { it > 0 }?.toString()
        ?: measurement(RingMetricType.STEPS)
    val ecgText = measurement(RingMetricType.ECG)
    val ecgStatus = state.signals[RingMetricType.ECG]?.let { "已保存 ${it.sampleCount} 点波形" } ?: periodLabel
    val activity = state.activity
    val (activityText, activityUnit) = when {
        activity == null -> "--" to "kcal"
        activity.caloriesKcal > 0.0 -> String.format(Locale.getDefault(), "%.0f", activity.caloriesKcal) to "kcal"
        activity.distanceMeters > 0.0 -> String.format(Locale.getDefault(), "%.0f", activity.distanceMeters) to "米"
        activity.steps > 0 -> activity.steps.toString() to "步"
        else -> "--" to "kcal"
    }
    fun decimalMeasurement(type: RingMetricType): String {
        val record = state.measurements[type] ?: return "--"
        return String.format(Locale.getDefault(), "%.1f", record.primaryValue)
    }
    fun measurementUnit(type: RingMetricType, fallback: String): String =
        state.measurements[type]?.unit?.takeIf(String::isNotBlank) ?: fallback
    fun capabilityStatus(type: RingMetricType, fallback: String): String = when {
        state.connectedDevice == null -> if (cloudMode) "绑定云米后同步" else "连接设备后检测能力"
        type !in state.supportedMetrics -> "当前设备不支持"
        cloudMode -> "云米云端 · $fallback"
        else -> fallback
    }
    val hrvRecord = state.measurements[RingMetricType.HRV]
        ?.takeIf { isDisplayableAdvancedMeasurement(RingMetricType.HRV, it) }
    val stressRecord = state.measurements[RingMetricType.STRESS]
        ?.takeIf { isDisplayableAdvancedMeasurement(RingMetricType.STRESS, it) }
    val metRecord = state.measurements[RingMetricType.MET]
        ?.takeIf { isDisplayableAdvancedMeasurement(RingMetricType.MET, it) }
    val vitalMetrics = buildList {
        add(RingMetricUi(RingMetricType.HEART_RATE, "心率", hrText, "bpm", capabilityStatus(RingMetricType.HEART_RATE, periodLabel), Icons.Outlined.FavoriteBorder, Color(0xFFFF6078), manualMeasure = RingMetricType.HEART_RATE in state.manuallyMeasurableMetrics, showAction = !cloudMode, showChart = !cloudMode))
        add(RingMetricUi(RingMetricType.BLOOD_OXYGEN, "血氧", spo2Text, "%", capabilityStatus(RingMetricType.BLOOD_OXYGEN, spo2PeriodStatus), Icons.Outlined.DataUsage, Color(0xFF148BFF), manualMeasure = RingMetricType.BLOOD_OXYGEN in state.manuallyMeasurableMetrics, showAction = !cloudMode, showChart = !cloudMode))
        add(RingMetricUi(RingMetricType.BLOOD_PRESSURE, "血压", bpText, "mmHg", capabilityStatus(RingMetricType.BLOOD_PRESSURE, periodLabel), Icons.Outlined.FavoriteBorder, Color(0xFF8B63F6), manualMeasure = RingMetricType.BLOOD_PRESSURE in state.manuallyMeasurableMetrics, showAction = !cloudMode, showChart = !cloudMode))
        if (!cloudMode && hrvRecord != null) {
            val canMeasure = RingMetricType.HRV in state.manuallyMeasurableMetrics
            add(RingMetricUi(RingMetricType.HRV, "HRV", hrvText, "ms", periodLabel, Icons.Outlined.Timeline, Color(0xFF00A6A6), manualMeasure = canMeasure, showAction = canMeasure))
        }
        if (!cloudMode) add(RingMetricUi(RingMetricType.BLOOD_GLUCOSE, "血糖", decimalMeasurement(RingMetricType.BLOOD_GLUCOSE), measurementUnit(RingMetricType.BLOOD_GLUCOSE, "设备单位"), capabilityStatus(RingMetricType.BLOOD_GLUCOSE, "设备估算，仅供健康参考"), Icons.Outlined.DataUsage, Color(0xFFE06B57), manualMeasure = RingMetricType.BLOOD_GLUCOSE in state.manuallyMeasurableMetrics, showAction = true))
        if (!cloudMode && stressRecord != null) {
            val canMeasure = RingMetricType.STRESS in state.manuallyMeasurableMetrics
            add(RingMetricUi(RingMetricType.STRESS, "压力", measurement(RingMetricType.STRESS), "分", periodLabel, Icons.Outlined.Timeline, Color(0xFF7B61B8), manualMeasure = canMeasure, showAction = canMeasure))
        }
        if (!cloudMode && metRecord != null) {
            add(RingMetricUi(RingMetricType.MET, "MET", decimalMeasurement(RingMetricType.MET), "MET", "设备历史数据", Icons.Outlined.ShowChart, Color(0xFF2E8B72)))
        }
        if (!cloudMode) add(RingMetricUi(RingMetricType.ECG, "ECG", ecgText, "bpm", capabilityStatus(RingMetricType.ECG, ecgStatus), Icons.Outlined.Assessment, Color(0xFF009688), manualMeasure = RingMetricType.ECG in state.manuallyMeasurableMetrics, showAction = true))
    }
    val bloodComponentTypes = listOf(
        RingMetricType.URIC_ACID,
        RingMetricType.TOTAL_CHOLESTEROL,
        RingMetricType.TRIGLYCERIDES,
        RingMetricType.HDL_CHOLESTEROL,
        RingMetricType.LDL_CHOLESTEROL,
    )
    val bloodComponentMetrics = listOf(
        RingMetricUi(RingMetricType.BLOOD_COMPONENT, "血液成分", "${bloodComponentTypes.count(state.measurements::containsKey)}/5", "项", capabilityStatus(RingMetricType.BLOOD_COMPONENT, "设备估算，仅供健康参考"), Icons.Outlined.DataUsage, Color(0xFFC35B90), manualMeasure = RingMetricType.BLOOD_COMPONENT in state.manuallyMeasurableMetrics, showAction = true),
        RingMetricUi(RingMetricType.URIC_ACID, "尿酸", decimalMeasurement(RingMetricType.URIC_ACID), measurementUnit(RingMetricType.URIC_ACID, "设备单位"), periodLabel, Icons.Outlined.DataUsage, Color(0xFFC35B90)),
        RingMetricUi(RingMetricType.TOTAL_CHOLESTEROL, "总胆固醇", decimalMeasurement(RingMetricType.TOTAL_CHOLESTEROL), measurementUnit(RingMetricType.TOTAL_CHOLESTEROL, "设备单位"), periodLabel, Icons.Outlined.DataUsage, Color(0xFFC35B90)),
        RingMetricUi(RingMetricType.TRIGLYCERIDES, "甘油三酯", decimalMeasurement(RingMetricType.TRIGLYCERIDES), measurementUnit(RingMetricType.TRIGLYCERIDES, "设备单位"), periodLabel, Icons.Outlined.DataUsage, Color(0xFFC35B90)),
        RingMetricUi(RingMetricType.HDL_CHOLESTEROL, "HDL", decimalMeasurement(RingMetricType.HDL_CHOLESTEROL), measurementUnit(RingMetricType.HDL_CHOLESTEROL, "设备单位"), periodLabel, Icons.Outlined.DataUsage, Color(0xFFC35B90)),
        RingMetricUi(RingMetricType.LDL_CHOLESTEROL, "LDL", decimalMeasurement(RingMetricType.LDL_CHOLESTEROL), measurementUnit(RingMetricType.LDL_CHOLESTEROL, "设备单位"), periodLabel, Icons.Outlined.DataUsage, Color(0xFFC35B90)),
    )
    val bodyComponentTypes = listOf(
        RingMetricType.BMI,
        RingMetricType.BODY_FAT_PERCENT,
        RingMetricType.FAT_MASS,
        RingMetricType.FAT_FREE_MASS,
        RingMetricType.MUSCLE_PERCENT,
        RingMetricType.MUSCLE_MASS,
        RingMetricType.SUBCUTANEOUS_FAT_PERCENT,
        RingMetricType.BODY_WATER_PERCENT,
        RingMetricType.WATER_MASS,
        RingMetricType.SKELETAL_MUSCLE_PERCENT,
        RingMetricType.BONE_MASS,
        RingMetricType.PROTEIN_PERCENT,
        RingMetricType.PROTEIN_MASS,
        RingMetricType.BASAL_METABOLIC_RATE,
    )
    val bodyComponentMetrics = listOf(
        RingMetricUi(RingMetricType.BODY_COMPOSITION, "身体成分", "${bodyComponentTypes.count(state.measurements::containsKey)}/14", "项", capabilityStatus(RingMetricType.BODY_COMPOSITION, "设备估算，仅供健康参考"), Icons.Outlined.Assessment, Color(0xFF6A72D8), manualMeasure = RingMetricType.BODY_COMPOSITION in state.manuallyMeasurableMetrics, showAction = true),
        RingMetricUi(RingMetricType.BMI, "BMI", decimalMeasurement(RingMetricType.BMI), "kg/m²", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.BODY_FAT_PERCENT, "体脂率", decimalMeasurement(RingMetricType.BODY_FAT_PERCENT), "%", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.FAT_MASS, "脂肪量", decimalMeasurement(RingMetricType.FAT_MASS), "kg", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.FAT_FREE_MASS, "去脂体重", decimalMeasurement(RingMetricType.FAT_FREE_MASS), "kg", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.MUSCLE_PERCENT, "肌肉率", decimalMeasurement(RingMetricType.MUSCLE_PERCENT), "%", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.MUSCLE_MASS, "肌肉量", decimalMeasurement(RingMetricType.MUSCLE_MASS), "kg", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.SUBCUTANEOUS_FAT_PERCENT, "皮下脂肪率", decimalMeasurement(RingMetricType.SUBCUTANEOUS_FAT_PERCENT), "%", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.BODY_WATER_PERCENT, "体水分率", decimalMeasurement(RingMetricType.BODY_WATER_PERCENT), "%", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.WATER_MASS, "水分量", decimalMeasurement(RingMetricType.WATER_MASS), "kg", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.SKELETAL_MUSCLE_PERCENT, "骨骼肌率", decimalMeasurement(RingMetricType.SKELETAL_MUSCLE_PERCENT), "%", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.BONE_MASS, "骨量", decimalMeasurement(RingMetricType.BONE_MASS), "kg", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.PROTEIN_PERCENT, "蛋白质率", decimalMeasurement(RingMetricType.PROTEIN_PERCENT), "%", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.PROTEIN_MASS, "蛋白质量", decimalMeasurement(RingMetricType.PROTEIN_MASS), "kg", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
        RingMetricUi(RingMetricType.BASAL_METABOLIC_RATE, "基础代谢", decimalMeasurement(RingMetricType.BASAL_METABOLIC_RATE), "kcal/day", periodLabel, Icons.Outlined.Assessment, Color(0xFF6A72D8)),
    )
    val dailyMetrics = listOf(
        RingMetricUi(RingMetricType.SLEEP, "睡眠", sleepValue, "", periodLabel, Icons.Outlined.AutoAwesome, Color(0xFF9668EF)),
        RingMetricUi(RingMetricType.STEPS, "步数", stepsText, "步", periodLabel, Icons.Outlined.ShowChart, Color(0xFF20B77A)),
        RingMetricUi(RingMetricType.ACTIVITY, "运动/活动", activityText, activityUnit, periodLabel, Icons.Outlined.Timeline, Color(0xFFFF8A32)),
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
                labels = DATA_PERIOD_OPTIONS.map { it.first },
                selected = selectedPeriod,
                onSelected = { selectedPeriod = it },
            )
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
                HealthScoreCard(
                    summary = currentRhi,
                    error = rhiRefreshError,
                    modifier = Modifier.weight(1f),
                )
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
                onMeasure = { type ->
                    if (type == RingMetricType.ECG) {
                        showEcgInstructionsOnOpen = true
                        showEcgDetail = true
                    } else {
                        onMeasure(type)
                    }
                },
                measureEnabled = !state.isSyncing,
            )
        }
        if (!cloudMode) item {
            EcgDetailEntryCard(
                latest = state.ecgHistory.firstOrNull(),
                isMeasuring = state.measuringMetric == RingMetricType.ECG,
                onClick = { showEcgDetail = true },
            )
        }
        if (!cloudMode) item {
            DashboardSectionHeader(Icons.Outlined.DataUsage, "血液成分")
        }
        if (!cloudMode) item {
            MetricGrid(
                metrics = bloodComponentMetrics,
                measuringMetric = state.measuringMetric,
                onMeasure = onMeasure,
                measureEnabled = !state.isSyncing,
            )
        }
        if (!cloudMode) item {
            DashboardSectionHeader(Icons.Outlined.Assessment, "身体成分")
        }
        if (!cloudMode) item {
            MetricGrid(
                metrics = bodyComponentMetrics,
                measuringMetric = state.measuringMetric,
                onMeasure = { type ->
                    pendingMeasurementInstruction = measurementInstructionFor(type)?.let { type }
                    if (pendingMeasurementInstruction == null) onMeasure(type)
                },
                measureEnabled = !state.isSyncing,
            )
        }
        if (!cloudMode) item {
            DashboardSectionHeader(Icons.Outlined.AutoAwesome, "设备健康设置")
        }
        if (!cloudMode) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DeviceFeatureCard(
                    title = "血糖校准",
                    description = "使用指尖血糖仪参考值校准设备；不作为医疗诊断依据",
                    supported = RingFeatureType.BLOOD_GLUCOSE_CALIBRATION in state.supportedFeatures,
                    enabled = !state.isSyncing,
                    onClick = { showBloodGlucoseCalibration = true },
                )
                DeviceFeatureCard(
                    title = "女性功能",
                    description = "配置经期长度、周期和最近一次经期开始日期",
                    supported = RingFeatureType.WOMENS_HEALTH in state.supportedFeatures,
                    enabled = !state.isSyncing,
                    onClick = { showWomensHealthSetting = true },
                )
            }
        }
        item {
            DashboardSectionHeader(
                Icons.Outlined.Timeline,
                if (cloudMode) "云米云端数据" else "睡眠与活动",
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onSync,
                    enabled = !state.isSyncing,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                ) {
                    Icon(
                        Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        if (state.isSyncing) {
                            "正在同步 ${state.syncProgress}%"
                        } else if (cloudMode) {
                            "同步云米健康数据"
                        } else {
                            "同步睡眠、步数与活动"
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    if (cloudMode) {
                        "从云米云端获取心率、血氧和血压；后端持久化成功后保存到本机。首次最多回填 31 天。"
                    } else {
                        "从设备读取历史数据，保存到本机后加入云端同步队列"
                    },
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        if (!cloudMode) item {
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
                        if (cloudMode) {
                            "当前为云米云端手表历史数据。血压、血氧等结果仅用于健康管理参考，不能替代医疗诊断。"
                        } else {
                            "当前为智能戒指采集数据。血压、血氧等结果仅用于健康管理参考。"
                        },
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

    if (showBloodGlucoseCalibration) {
        BloodGlucoseCalibrationDialog(
            onDismiss = { showBloodGlucoseCalibration = false },
            onConfirm = { value ->
                showBloodGlucoseCalibration = false
                ringViewModel.setBloodGlucoseCalibration(enabled = true, referenceValue = value)
            },
        )
    }
    if (showWomensHealthSetting) {
        WomensHealthDialog(
            onDismiss = { showWomensHealthSetting = false },
            onConfirm = { periodLength, cycleLength, lastStart ->
                showWomensHealthSetting = false
                ringViewModel.setMenstrualCycle(periodLength, cycleLength, lastStart)
            },
        )
    }
    pendingMeasurementInstruction?.let { metricType ->
        MeasurementInstructionDialog(
            metricType = metricType,
            onDismiss = { pendingMeasurementInstruction = null },
            onConfirm = {
                pendingMeasurementInstruction = null
                onMeasure(metricType)
            },
        )
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
private fun DeviceFeatureCard(
    title: String,
    description: String,
    supported: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .border(1.dp, Color(0xFFE1E9E7), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = Mint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                if (supported) description else "当前设备未报告此能力",
                color = Muted,
                fontSize = 9.sp,
                lineHeight = 13.sp,
            )
        }
        TextButton(onClick = onClick, enabled = supported && enabled) {
            Text(if (supported) "设置" else "未支持")
        }
    }
}

@Composable
private fun BloodGlucoseCalibrationDialog(onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var value by remember { mutableStateOf("") }
    val parsed = value.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("血糖校准") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("请输入同一时刻指尖血糖仪的参考值，单位应与设备当前血糖单位一致。此功能不能替代医疗检测。")
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("血糖仪参考值") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = value.isNotEmpty() && parsed == null,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null) { Text("启用校准") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun WomensHealthDialog(
    onDismiss: () -> Unit,
    onConfirm: (periodLengthDays: Int, cycleLengthDays: Int, lastPeriodStartAt: Long) -> Unit,
) {
    var periodLength by remember { mutableStateOf("5") }
    var cycleLength by remember { mutableStateOf("28") }
    var lastStart by remember { mutableStateOf(LocalDate.now().toString()) }
    val period = periodLength.toIntOrNull()?.takeIf { it in 4..28 }
    val cycle = cycleLength.toIntOrNull()?.takeIf { period != null && it >= period }
    val date = runCatching { LocalDate.parse(lastStart) }.getOrNull()?.takeIf { !it.isAfter(LocalDate.now()) }
    val valid = period != null && cycle != null && date != null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("女性健康周期") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("这些属于敏感健康信息，仅在你确认后写入当前佩戴设备。")
                OutlinedTextField(
                    value = periodLength,
                    onValueChange = { periodLength = it },
                    label = { Text("经期长度（4–28 天）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = cycleLength,
                    onValueChange = { cycleLength = it },
                    label = { Text("周期长度（天）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = lastStart,
                    onValueChange = { lastStart = it },
                    label = { Text("最近经期开始日期（yyyy-MM-dd）") },
                    singleLine = true,
                    isError = lastStart.isNotEmpty() && date == null,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (period != null && cycle != null && date != null) {
                        onConfirm(
                            period,
                            cycle,
                            date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        )
                    }
                },
                enabled = valid,
            ) { Text("保存到设备") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
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
private fun HealthScoreCard(
    summary: RhiPeriodSummary?,
    error: String?,
    modifier: Modifier,
) {
    val presentation = dataHealthIndexPresentation(summary, error)
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
                    sweepAngle = presentation.sweepAngle,
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
                Text(presentation.scoreText, color = Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                Text(
                    presentation.statusText,
                    color = Mint,
                    fontSize = 10.sp,
                    modifier = Modifier.clip(CircleShape).background(MintSoft)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        Text(
            presentation.supportingText,
            color = Muted,
            fontSize = 9.sp,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

internal data class DataRiskPresentation(
    val scoreText: String,
    val riskLevelText: String,
    val sourceText: String,
    val summary: String,
)

internal fun dataRiskPresentation(status: RemoteFeatureEvaluateStatus?): DataRiskPresentation {
    val confirmedScore = status?.riskScore?.takeIf {
        status.reachable && status.isMock == false && it.isFinite() && it in 0.0..1.0
    }
    val confirmed = confirmedScore != null
    return DataRiskPresentation(
        scoreText = confirmedScore?.let { String.format(Locale.US, "%.1f%%", it * 100.0) } ?: "--",
        riskLevelText = if (confirmed) status?.riskLevel.riskLevelLabel() else "待评估",
        sourceText = if (confirmed) {
            "RDI-16 · 风险概率"
        } else if (status?.isMock == true) {
            "结果不可用"
        } else if (status?.reachable == false) {
            "不可用"
        } else {
            "评估中"
        },
        summary = when {
            confirmed -> status?.summary ?: "RDI-16 风险评估已完成"
            status?.isMock == true -> "当前结果未通过真实性校验，未作为风险分展示"
            status != null -> status.summary
            else -> "正在请求 RDI-16 风险评估"
        },
    )
}

internal data class DataHealthIndexPresentation(
    val scoreText: String,
    val statusText: String,
    val supportingText: String,
    val sweepAngle: Float,
)

internal fun dataHealthIndexPresentation(
    summary: RhiPeriodSummary?,
    error: String?,
): DataHealthIndexPresentation {
    val score = summary?.score
    if (score == null) {
        return DataHealthIndexPresentation(
            scoreText = "--",
            statusText = "积累中",
            supportingText = error ?: summary?.let {
                "RHI 有效数据 ${it.validDays}/${it.requiredValidDays} 天"
            } ?: "正在计算 RHI-100",
            sweepAngle = 0f,
        )
    }
    val status = when {
        score >= 80.0 -> "优秀"
        score >= 60.0 -> "良好"
        score >= 50.0 -> "平稳"
        else -> "待改善"
    }
    val periodText = if (summary.aggregation == RhiPeriodAggregation.CURRENT_7_DAY) {
        "近7日有效数据"
    } else {
        "${summary.periodDays}日稳健中位数"
    }
    val deltaText = summary.trendDelta?.let { delta ->
        val sign = if (delta > 0.0) "+" else ""
        " · 期内$sign${String.format(Locale.US, "%.1f", delta)}"
    }.orEmpty()
    return DataHealthIndexPresentation(
        scoreText = String.format(Locale.US, "%.1f", score),
        statusText = status,
        supportingText = "RHI-100 · $periodText$deltaText",
        sweepAngle = (score.coerceIn(0.0, 100.0) / 100.0 * 360.0).toFloat(),
    )
}

@Composable
private fun SmartRingOverviewCard(state: RingUiState, modifier: Modifier) {
    val cloudMode = state.acquisitionMode == RingAcquisitionMode.CLOUD
    val hasConnectedDevice = state.connectedDevice != null
    val hasLocalData = state.measurements.isNotEmpty() || state.sleep != null
    Column(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .border(1.dp, Color(0xFFD6E5E1), RoundedCornerShape(20.dp))
            .padding(11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (cloudMode) "云米云端手表" else "睿禾智能戒指", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
            Box(Modifier.size(16.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
        Text(
            when {
                hasConnectedDevice -> if (cloudMode) "设备已绑定" else "设备已连接"
                hasLocalData -> "有历史数据，需重新连接"
                else -> "设备未连接"
            },
            color = Muted,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (cloudMode) {
                Icon(
                    Icons.Outlined.Watch,
                    contentDescription = "云米云端手表",
                    tint = Mint,
                    modifier = Modifier.size(56.dp),
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.smart_ring),
                    contentDescription = "睿禾智能戒指",
                    modifier = Modifier.fillMaxWidth().height(67.dp),
                    contentScale = ContentScale.Fit,
                )
            }
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
        Toast.makeText(
            context,
            "开始${metric.actionLabel}${metric.title}，请保持设备佩戴稳定",
            Toast.LENGTH_SHORT,
        ).show()
        onMeasure(metric.type)
    }
    Column(
        modifier = modifier.height(if (metric.showAction) 116.dp else 102.dp).clip(RoundedCornerShape(18.dp))
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
            if (!metric.showAction && metric.showChart) {
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
            if (metric.showAction) {
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (metric.manualMeasure) MintSoft else Color(0xFFF0F2F2))
                        .border(1.dp, if (metric.manualMeasure) Mint.copy(alpha = 0.22f) else Line, RoundedCornerShape(999.dp))
                        .clickable(enabled = metric.manualMeasure && measureEnabled && !measuring, onClick = startMeasure)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        when {
                            measuring -> metric.measuringLabel
                            metric.manualMeasure -> metric.actionLabel
                            else -> "未支持"
                        },
                        color = if (metric.manualMeasure) Mint else Muted,
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
