package com.rehealth.genie.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rehealth.genie.R
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.BuildConfig
import com.rehealth.genie.chat.ChatTurn
import com.rehealth.genie.chat.DeepSeekClient
import com.rehealth.genie.phm.Intervention
import com.rehealth.genie.phm.ModelInputStage
import com.rehealth.genie.phm.ModelInputStatus
import com.rehealth.genie.phm.MockPhmService
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ring.RingViewModel
import com.rehealth.genie.ui.theme.Canvas
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppStage { Splash, Login, InterviewSession, DeviceSetup, Main }
private enum class Tab(val label: String, val icon: ImageVector) {
    Home("首页", Icons.Outlined.Home),
    Data("数据", Icons.Outlined.ShowChart),
    Attribution("归因", Icons.Outlined.Assessment),
    Model("模型", Icons.Outlined.SmartToy),
    Profile("我的", Icons.Outlined.PersonOutline),
}

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

private data class DemoVitalPreset(
    val heartRate: Int,
    val bloodOxygen: Int,
    val systolic: Int,
    val diastolic: Int,
    val temperature: Double,
) {
    fun valueFor(type: RingMetricType): String = when (type) {
        RingMetricType.HEART_RATE -> heartRate.toString()
        RingMetricType.BLOOD_OXYGEN -> bloodOxygen.toString()
        RingMetricType.BLOOD_PRESSURE -> "$systolic/$diastolic"
        RingMetricType.TEMPERATURE -> String.format(Locale.getDefault(), "%.1f", temperature)
        else -> "--"
    }

    fun asValueMap(): Map<RingMetricType, String> = mapOf(
        RingMetricType.HEART_RATE to valueFor(RingMetricType.HEART_RATE),
        RingMetricType.BLOOD_OXYGEN to valueFor(RingMetricType.BLOOD_OXYGEN),
        RingMetricType.BLOOD_PRESSURE to valueFor(RingMetricType.BLOOD_PRESSURE),
        RingMetricType.TEMPERATURE to valueFor(RingMetricType.TEMPERATURE),
    )
}

private val demoVitalPresets = listOf(
    DemoVitalPreset(heartRate = 72, bloodOxygen = 98, systolic = 118, diastolic = 76, temperature = 36.5),
    DemoVitalPreset(heartRate = 78, bloodOxygen = 97, systolic = 122, diastolic = 79, temperature = 36.7),
    DemoVitalPreset(heartRate = 68, bloodOxygen = 99, systolic = 115, diastolic = 74, temperature = 36.4),
)

private data class DataPeriodDemo(
    val subtitle: String,
    val statusMessage: String,
    val healthScore: Int,
    val healthLabel: String,
    val healthSummary: String,
    val vitals: DemoVitalPreset,
    val sleep: String,
    val steps: String,
    val metricStatus: String,
)

private val dataPeriodDemos = listOf(
    DataPeriodDemo(
        subtitle = "今日身体状态概览",
        statusMessage = "今日状态最佳，健康指数较近7天提升 2 分",
        healthScore = 91,
        healthLabel = "优秀",
        healthSummary = "今日表现最佳，较近7天提升 2 分 ›",
        vitals = DemoVitalPreset(70, 99, 116, 74, 36.5),
        sleep = "7h48m",
        steps = "8,240",
        metricStatus = "今日累计",
    ),
    DataPeriodDemo(
        subtitle = "近7天身体状态总览",
        statusMessage = "近7天状态优于近30天，健康指数提升 2 分",
        healthScore = 89,
        healthLabel = "优秀",
        healthSummary = "近7天优于近30天，继续保持 ›",
        vitals = DemoVitalPreset(72, 99, 118, 76, 36.5),
        sleep = "7h35m",
        steps = "7,680",
        metricStatus = "近7天平均",
    ),
    DataPeriodDemo(
        subtitle = "近30天身体状态总览",
        statusMessage = "近30天状态优于近90天，健康指数提升 3 分",
        healthScore = 87,
        healthLabel = "良好",
        healthSummary = "近30天优于近90天，趋势向好 ›",
        vitals = DemoVitalPreset(74, 98, 120, 78, 36.5),
        sleep = "7h24m",
        steps = "7,210",
        metricStatus = "近30天平均",
    ),
    DataPeriodDemo(
        subtitle = "近90天身体状态总览",
        statusMessage = "近90天健康基线稳定，近期状态持续改善",
        healthScore = 84,
        healthLabel = "良好",
        healthSummary = "近90天基线稳定，近期持续改善 ›",
        vitals = DemoVitalPreset(76, 97, 122, 80, 36.6),
        sleep = "7h10m",
        steps = "6,840",
        metricStatus = "近90天平均",
    ),
)

private fun demoMeasuringValue(type: RingMetricType, step: Int, target: DemoVitalPreset): String = when (type) {
    RingMetricType.HEART_RATE -> (target.heartRate + listOf(-2, 1, -1, 2, 0, 1)[step % 6]).toString()
    RingMetricType.BLOOD_OXYGEN -> (target.bloodOxygen + listOf(0, -1, 0, 1, 0, -1)[step % 6])
        .coerceIn(95, 99).toString()
    RingMetricType.BLOOD_PRESSURE -> {
        val systolic = target.systolic + listOf(-2, 1, -1, 2, 0, 1)[step % 6]
        val diastolic = target.diastolic + listOf(-1, 1, 0, 1, -1, 0)[step % 6]
        "$systolic/$diastolic"
    }
    RingMetricType.TEMPERATURE -> String.format(
        Locale.getDefault(),
        "%.1f",
        target.temperature + listOf(0.0, -0.1, 0.1, 0.0, 0.1, -0.1)[step % 6],
    )
    else -> "--"
}

private data class AttributionFactor(
    val section: String,
    val title: String,
    val value: String,
    val score: String,
    val share: String,
    val progress: Float,
    val detail: String,
    val evidence: String,
    val action: String,
)

private val attributionFactors = listOf(
    AttributionFactor("基础体征", "年龄", "32 岁", "基线项", "模型输入", 0.58f, "年龄用于校准长期心脑血管风险，当前健康画像年龄为 32 岁。", "来源：健康初始采访 · 已建立", "每年复查一次健康基线"),
    AttributionFactor("基础体征", "性别", "男", "基线项", "模型输入", 0.24f, "性别是模型的基础校准变量，与其他临床指标共同参与风险估计。", "来源：健康初始采访 · 已建立", "信息变化时更新健康画像"),
    AttributionFactor("基础体征", "BMI", "23.8 kg/m²", "-2 分", "贡献度 4%", 0.38f, "当前 BMI 处于健康范围，体重变化会影响血压、血糖和血脂相关风险。", "来源：身高体重记录 · 最近更新 06-11", "维持当前体重，优先关注腰围趋势"),
    AttributionFactor("血压与代谢", "收缩压", "118 mmHg", "-6 分", "贡献度 8%", 0.64f, "近 3 次测量均值 118 mmHg，较上一周期下降 4 mmHg，波动较小。", "来源：戒指血压测量 · 3 次有效", "测量前静坐 5 分钟，保持固定时间测量"),
    AttributionFactor("血压与代谢", "舒张压", "76 mmHg", "-4 分", "贡献度 6%", 0.52f, "舒张压处于当前个人基线附近，与收缩压一起用于计算血压相关风险。", "来源：戒指血压测量 · 3 次有效", "继续保持规律睡眠和适度运动"),
    AttributionFactor("血压与代谢", "空腹血糖", "5.2 mmol/L", "-3 分", "贡献度 5%", 0.45f, "空腹血糖在本次健康画像中保持稳定，饮食结构和运动依从性会影响后续趋势。", "来源：临床报告手工录入 · 最近更新 05-28", "复查时尽量保持空腹条件一致"),
    AttributionFactor("血压与代谢", "总胆固醇", "4.6 mmol/L", "-2 分", "贡献度 4%", 0.37f, "总胆固醇用于评估血脂整体水平，需要结合 LDL、HDL 和甘油三酯一起看。", "来源：临床报告手工录入 · 最近更新 05-28", "减少高油高盐饮食，按计划复查血脂"),
    AttributionFactor("血压与代谢", "LDL 胆固醇", "2.4 mmol/L", "-2 分", "贡献度 4%", 0.35f, "LDL 是血脂风险解释中的重点指标，当前记录较上一周期下降 0.2 mmol/L。", "来源：临床报告手工录入 · 最近更新 05-28", "保持膳食纤维摄入，减少反式脂肪"),
    AttributionFactor("血压与代谢", "HDL 胆固醇", "1.5 mmol/L", "-3 分", "贡献度 5%", 0.48f, "HDL 与运动、体重和饮食结构相关，当前水平对个人风险评估较友好。", "来源：临床报告手工录入 · 最近更新 05-28", "每周完成至少 4 天中等强度活动"),
    AttributionFactor("血压与代谢", "甘油三酯", "1.1 mmol/L", "-2 分", "贡献度 3%", 0.32f, "甘油三酯受晚间饮食、饮酒和运动影响，当前记录处于个人目标范围。", "来源：临床报告手工录入 · 最近更新 05-28", "晚餐控制精制碳水，避免夜间加餐"),
    AttributionFactor("生活方式", "每周运动天数", "4 天", "+18 分", "贡献度 28%", 0.78f, "本周完成 4 天中等强度活动，日均步数 6,842 步，是当前最主要的可干预改善因素。", "来源：戒指步数与活动记录 · 5/7 天有效", "晚餐后步行 15 分钟，把运动保持在每周 4-5 天"),
    AttributionFactor("生活方式", "吸烟", "不吸烟", "-5 分", "贡献度 7%", 0.72f, "当前健康画像记录为不吸烟，模型将该状态作为保护性生活方式因素。", "来源：健康初始采访 · 已建立", "继续保持，避免长期被动吸烟"),
    AttributionFactor("生活方式", "饮酒", "每周 1 次", "+1 分", "贡献度 2%", 0.18f, "当前饮酒频率较低，对本周期风险趋势影响有限，但与睡眠和甘油三酯有关联。", "来源：健康初始采访 · 最近更新 06-11", "控制饮酒频率，避免连续多日饮酒"),
    AttributionFactor("病史与家族史", "糖尿病史", "无", "-4 分", "贡献度 6%", 0.66f, "当前未记录糖尿病史，后续如果临床报告出现新结果，需要重新评估空腹血糖相关风险。", "来源：健康初始采访 · 已建立", "每年复查空腹血糖或糖化血红蛋白"),
    AttributionFactor("病史与家族史", "高血压史", "无", "-4 分", "贡献度 6%", 0.63f, "当前未记录高血压史，戒指测量结果会用于观察个人血压趋势，不替代临床诊断。", "来源：健康初始采访 · 已建立", "继续记录固定时间的血压变化"),
    AttributionFactor("病史与家族史", "家族史", "无明确家族史", "基线项", "模型输入", 0.22f, "家族史用于校准先天风险背景，当前健康画像未发现明确的早发心脑血管疾病家族史。", "来源：健康初始采访 · 已建立", "有新家族健康信息时及时补充"),
)

private data class MealInsight(
    val title: String,
    val calories: String,
    val targetGap: String,
    val description: String,
    val protein: String,
    val carbs: String,
    val fat: String,
)

private val lunchInsight = MealInsight(
    title = "红烧牛肉面 + 鸡蛋 + 无糖茶",
    calories = "780 kcal",
    targetGap = "+180 kcal",
    description = "午餐热量高于你的个人目标，主要来自面量和汤汁；蛋白质摄入基本充足。",
    protein = "蛋白质 29 g",
    carbs = "碳水 86 g",
    fat = "脂肪 25 g",
)

private data class AttributionTrend(
    val windowDays: Int,
    val baselineAverage: Float,
    val currentAverage: Float,
    val improvementScore: Int,
    val riskLevel: String,
    val trendLabel: String,
    val healthSeries: List<Float>,
    val statusQuo: List<Float>,
    val withPlan: List<Float>,
)

private val healthRiskHistory = List(180) { index ->
    val progress = index / 179f
    val noise = listOf(0.008f, -0.004f, 0.006f, -0.002f, 0.004f)[index % 5]
    (0.64f - progress * 0.19f + noise).coerceIn(0.38f, 0.68f)
}

private fun sampleSeries(values: List<Float>, count: Int): List<Float> {
    if (values.isEmpty()) return emptyList()
    if (values.size <= count) return values
    return List(count) { index ->
        val sourceIndex = index * (values.lastIndex) / (count - 1).coerceAtLeast(1)
        values[sourceIndex]
    }
}

private fun buildAttributionTrend(windowDays: Int): AttributionTrend {
    val currentStart = healthRiskHistory.size - windowDays
    val currentWindow = healthRiskHistory.subList(currentStart, healthRiskHistory.size)
    val baselineStart = (currentStart - 90).coerceAtLeast(0)
    val baselineWindow = healthRiskHistory.subList(baselineStart, currentStart)
    val currentAverage = currentWindow.average().toFloat()
    val baselineAverage = baselineWindow.average().toFloat()
    val improvementScore = ((baselineAverage - currentAverage) * 320f).toInt().coerceIn(0, 99)
    val riskLevel = when {
        currentAverage < 0.38f -> "较低"
        currentAverage < 0.55f -> "中等"
        else -> "偏高"
    }
    val trendLabel = if (currentAverage < baselineAverage) "趋势改善" else "需要关注"
    val currentPoint = currentWindow.last()
    val planEffect = 0.018f + windowDays / 90f * 0.042f
    val statusQuo = List(9) { index ->
        (currentPoint - 0.014f * index / 8f).coerceAtLeast(0.28f)
    }
    val withPlan = List(9) { index ->
        (currentPoint - (0.014f + planEffect) * index / 8f).coerceAtLeast(0.25f)
    }
    return AttributionTrend(
        windowDays = windowDays,
        baselineAverage = baselineAverage,
        currentAverage = currentAverage,
        improvementScore = improvementScore,
        riskLevel = riskLevel,
        trendLabel = trendLabel,
        healthSeries = sampleSeries(healthRiskHistory.map { 1f - it }, 8),
        statusQuo = statusQuo,
        withPlan = withPlan,
    )
}

private fun formatRiskIndex(value: Float): String = String.format(Locale.getDefault(), "%.1f", value * 100f)

@Composable
fun ReHealthApp() {
    val application = LocalContext.current.applicationContext as ReHealthApplication
    val profilePreferences = remember(application) {
        application.getSharedPreferences("rehealth_profile", 0)
    }
    var stage by remember {
        val onboardingComplete = profilePreferences.getBoolean("onboarding_complete", false)
        mutableStateOf(if (onboardingComplete) AppStage.Main else AppStage.Splash)
    }
    val ringViewModel: RingViewModel = viewModel(
        factory = remember(application) {
            RingViewModel.Factory(application.ringRepository, application.database.ringDataDao())
        },
    )
    val ringState by ringViewModel.uiState.collectAsState()
    LaunchedEffect(stage) {
        if …14814 tokens truncated…>
                    ModelInputRow(input)
                    if (index != inputs.lastIndex) HorizontalDivider(color = Line)
                }
            }
            SectionTitle("个性化学习状态")
            CardBlock {
                StatusRow("健康基线", "已建立")
                StatusRow("近 7 日有效数据", "86%")
                StatusRow("已参考用户反馈", "18 次")
                StatusRow("最近学习时间", "今天 08:32")
                StatusRow("下次夜间更新", "今晚 23:00")
            }
            SectionTitle("隐私与数据状态")
            CardBlock {
                StatusRow("原始健康数据上传", "否")
                StatusRow("图片原图上传", "否")
                StatusRow("本地数据加密", "已开启")
                StatusRow("匿名模型改进", "未开启")
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MintSoft).padding(14.dp),
            ) {
                Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(18.dp))
                Text(
                    "戒指数据先在本机形成趋势与个人基线；参与云端模型改进前，将单独征得用户授权。",
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text("PHM Core 1.0.0 · 特征引擎 0.1.0 · 风险模型 0.1.0", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ModelPipelineRow(step: String, title: String, status: String, complete: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape)
                .background(if (complete) Mint else Color(0xFFFFF2D8)),
            contentAlignment = Alignment.Center,
        ) {
            Text(step, color = if (complete) Color.White else Color(0xFFD38B18), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(status, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(if (complete) "完成" else "进行中", color = if (complete) Mint else Color(0xFFD38B18), fontSize = 11.sp)
    }
}

@Composable
private fun ModelInputRow(input: ModelInputStatus) {
    val status = when (input.stage) {
        ModelInputStage.READY -> "数据就绪"
        ModelInputStage.FEATURE_EXTRACTED -> "特征已提取"
        ModelInputStage.LEARNING -> "参与学习"
    }
    val color = when (input.stage) {
        ModelInputStage.READY -> Color(0xFF6587FF)
        ModelInputStage.FEATURE_EXTRACTED -> Color(0xFF2DA8A0)
        ModelInputStage.LEARNING -> Mint
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.DataUsage, null, tint = color, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(input.label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(input.feature, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            status,
            color = color,
            fontSize = 10.sp,
            modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.1f))
                .padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = if (value.contains("待")) Color(0xFFE39A22) else Mint, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProfileScreen(
    onDeviceBinding: () -> Unit,
    onRestartOnboarding: () -> Unit,
) {
    Page("我的") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.AccountCircle, null, tint = Mint, modifier = Modifier.size(38.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("李明", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("已陪伴 28 天", color = Muted, fontSize = 11.sp)
                    }
                    Text("Pro 会员", color = Color(0xFFB47A13), fontSize = 11.sp, modifier = Modifier.clip(CircleShape).background(Color(0xFFFFF1CD)).padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("睡眠时长", "7h30m", "够眠", Modifier.weight(1f))
                Metric("每日步数", "8,000", "步", Modifier.weight(1f))
                Metric("体重目标", "54.0", "kg", Modifier.weight(1f))
            }
            CardBlock {
                MenuRow(Icons.Outlined.Devices, "设备绑定", onDeviceBinding)
                MenuRow(Icons.Outlined.Lock, "隐私中心")
                MenuRow(Icons.Outlined.Download, "数据导出")
                MenuRow(Icons.Outlined.DeleteOutline, "数据删除")
                MenuRow(Icons.Outlined.NotificationsNone, "通知设置")
                MenuRow(Icons.Outlined.Settings, "关于睿禾精灵")
                MenuRow(Icons.Outlined.Timeline, "重新体验首次使用流程", onRestartOnboarding)
            }
        }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Ink, modifier = Modifier.size(19.dp))
        Text(label, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = Line)
}

@Composable
private fun DeviceBindingScreen(
    state: RingUiState,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onConnect: (RingDevice) -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit,
    onboarding: Boolean = false,
    onComplete: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(hasBluetoothPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        permissionGranted = results.values.all { it }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Canvas).statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, "返回", tint = Ink)
                }
                Column {
                    Text(if (onboarding) "连接你的智能戒指" else "设备绑定", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (onboarding) "连接后即可进入主页" else "连接睿禾智能戒指并同步健康数据",
                        color = Muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        item {
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Bluetooth, null, tint = Mint)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("蓝牙权限", color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (permissionGranted) "已授权，可连接真实戒指" else "真实戒指到货后需要授权",
                            color = Muted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    Button(
                        onClick = {
                            permissionLauncher.launch(requiredBluetoothPermissions())
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (permissionGranted) MintSoft else Mint),
                    ) {
                        Text(if (permissionGranted) "已授权" else "授权", color = if (permissionGranted) Mint else Color.White)
                    }
                }
                Text(
                    "已切换为真实戒指链路，数据默认保存在本机。",
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
        item {
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("设备状态", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(connectionLabel(state.connectionState), color = Mint, fontSize = 12.sp)
                }
                Text(
                    state.connectedDevice?.name ?: "尚未连接设备",
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp),
                )
                state.connectedDevice?.let {
                    Text("${it.address} · RSSI ${it.rssi ?: "--"}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                if (state.message != null) {
                    Text(state.message, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
                if (state.isSyncing) {
                    LinearProgressIndicator(
                        progress = { state.syncProgress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        color = Mint,
                        trackColor = MintSoft,
                    )
                    Text("${state.syncProgress}%", color = Mint, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onScan,
                        enabled = !state.isScanning && !state.isSyncing,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MintSoft, contentColor = Mint),
                    ) {
                        Text(if (state.isScanning) "搜索中" else "搜索智能戒指")
                    }
                    if (state.connectedDevice != null) {
                        Button(
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F4F3), contentColor = Ink),
                        ) {
                            Text("断开")
                        }
                    }
                }
            }
        }
        item {
            CardBlock {
                Text("数据采集目标", color = Ink, fontWeight = FontWeight.SemiBold)
                Text(
                    "健康数据：睡眠 · 血压 · 体温 · 心率 · 步数 · 血氧\n设备功能：遥控拍照 · 女性健康",
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeaturePill(Icons.Outlined.AddAPhoto, "遥控拍照", Modifier.weight(1f))
                    FeaturePill(Icons.Outlined.FavoriteBorder, "女性健康", Modifier.weight(1f))
                }
                Text(
                    "本机已保存 ${state.collectedMetricCount} / 6 项健康数据",
                    color = Mint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Button(
                    onClick = onSync,
                    enabled = !state.isSyncing,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                ) {
                    Text(if (state.isSyncing) "正在同步 ${state.syncProgress}%" else "同步全部健康数据")
                }
                state.lastSyncAt?.let {
                    Text(
                        "最近同步：${formatSyncTime(it)}",
                        color = Muted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
        items(state.devices) { device ->
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Devices, null, tint = Mint, modifier = Modifier.size(28.dp))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(device.name ?: "智能戒指", color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("${device.address} · ${device.rssi ?: "--"} dBm", color = Muted, fontSize = 10.sp)
                    }
                    Button(
                        onClick = { onConnect(device) },
                        enabled = state.connectedDevice?.address != device.address,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint),
                    ) {
                        Text(if (state.connectedDevice?.address == device.address) "已连接" else "连接")
                    }
                }
            }
        }
        if (onboarding) {
            item {
                Button(
                    onClick = { onComplete?.invoke() },
                    enabled = state.connectedDevice != null,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                ) {
                    Text(
                        if (state.connectedDevice == null) "请先连接戒指" else "完成设置，进入主页",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    "连接成功后，我们会同步戒指数据并进入主页。",
                    color = Muted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp),
                )
                TextButton(
                    onClick = { onSkip?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("暂时没有戒指，跳过", color = Muted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(icon: ImageVector, label: String, modifier: Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(MintSoft)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Mint, modifier = Modifier.size(18.dp))
        Text(
            label,
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

private fun requiredBluetoothPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

private fun hasBluetoothPermission(context: Context): Boolean {
    return requiredBluetoothPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

private fun connectionLabel(state: RingConnectionState): String = when (state) {
    RingConnectionState.SCANNING -> "搜索中"
    RingConnectionState.CONNECTING -> "连接中"
    RingConnectionState.CONNECTED -> "已连接"
    RingConnectionState.SYNCING -> "同步中"
    RingConnectionState.ERROR -> "异常"
    RingConnectionState.UNSUPPORTED -> "不支持蓝牙"
    RingConnectionState.PERMISSION_REQUIRED -> "需要权限"
    RingConnectionState.BLUETOOTH_OFF -> "蓝牙未开启"
    RingConnectionState.DISCONNECTED -> "未连接"
}

private fun formatSyncTime(timestamp: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}
