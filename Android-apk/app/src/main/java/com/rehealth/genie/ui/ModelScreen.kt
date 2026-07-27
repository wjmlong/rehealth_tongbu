package com.rehealth.genie.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.features.HealthFeatureExtractor
import com.rehealth.genie.phm.ModelInputStage
import com.rehealth.genie.phm.ModelInputStatus
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted
import java.util.Locale

@Composable
internal fun ModelScreen(
    state: RingUiState,
    canonicalRiskStatus: androidx.compose.runtime.State<RemoteFeatureEvaluateStatus?>,
) {
    val inputs = remember(state.measurements, state.sleep, state.activity) { modelInputsFromRingState(state) }
    val current = canonicalRiskStatus.value
    Page("端侧健康模型", "你的健康 AI 正在本机运行") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ReHealthCardBlock {
                RemoteFeatureEvaluateRow(status = canonicalRiskStatus)
            }
            ReHealthCardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.SmartToy, null, tint = Mint)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("规范风险评估", color = Mint, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "${state.collectedMetricCount} 项戒指数据 · ${current?.modeLabel ?: "评估中"}",
                            color = Muted,
                            fontSize = 11.sp,
                        )
                    }
                    Text(current?.riskScore.riskScoreLabel(), color = Mint, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            ReHealthCardBlock {
                StatusRow("评估接口", "/rehealth/mobile/features/evaluate")
                StatusRow("算法模式", current?.modeLabel ?: "评估中")
                StatusRow("风险等级", current?.riskLevel.riskLevelLabel())
                StatusRow("风险分数", current?.riskScore.riskScoreLabel())
                StatusRow("模型版本", current?.modelVersion ?: "待返回")
                StatusRow("请求 ID", current?.requestId?.take(12) ?: "待返回")
                Text(
                    current?.summary ?: "正在读取本机特征并请求后端评估。",
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            SectionTitle("主要贡献因素")
            ReHealthCardBlock {
                val contributions = current?.featureContributions.orEmpty()
                    .toList()
                    .sortedByDescending { kotlin.math.abs(it.second) }
                    .take(4)
                if (contributions.isEmpty()) {
                    Text("后端暂未返回贡献因素。", color = Muted, fontSize = 12.sp)
                } else {
                    contributions.forEachIndexed { index, contribution ->
                        ContributionRow(contribution.first, contribution.second)
                        if (index != contributions.lastIndex) HorizontalDivider(color = Line)
                    }
                }
            }
            SectionTitle("端侧学习流程")
            ReHealthCardBlock {
                ModelPipelineRow("1", "戒指数据采集", "6 项健康数据", true)
                ModelPipelineRow("2", "本地特征工程", "HealthFeatureExtractor", true)
                ModelPipelineRow("3", "后端风险评估", current?.riskScore?.let { "今日已完成" } ?: "等待结果", current?.riskScore != null)
                ModelPipelineRow("4", "个性化学习", "后续闭环", false)
            }
            SectionTitle("戒指健康数据输入")
            ReHealthCardBlock {
                inputs.forEachIndexed { index, input ->
                    ModelInputRow(input)
                    if (index != inputs.lastIndex) HorizontalDivider(color = Line)
                }
            }
            SectionTitle("个性化学习状态")
            ReHealthCardBlock {
                StatusRow("健康基线", "已建立")
                StatusRow("近 7 日有效数据", "86%")
                StatusRow("已参考用户反馈", "18 次")
                StatusRow("最近学习时间", "今天 08:32")
                StatusRow("下次夜间更新", "今晚 23:00")
            }
            SectionTitle("隐私与数据状态")
            ReHealthCardBlock {
                StatusRow("原始健康数据上传", "否")
                StatusRow("图片原图上传", "否")
                StatusRow("模型服务直连", "否")
                StatusRow("遥测批量上传", "否")
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
private fun ContributionRow(field: String, contribution: Double) {
    val direction = if (contribution >= 0.0) "↑" else "↓"
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(field.cvdFieldLabel(), color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            "$direction ${String.format(Locale.getDefault(), "%.3f", kotlin.math.abs(contribution))}",
            color = if (contribution >= 0.0) Color(0xFFE39A22) else Mint,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun String.cvdFieldLabel(): String = when (this) {
    "age" -> "年龄"
    "gender" -> "性别"
    "bmi" -> "BMI"
    "sbp" -> "收缩压"
    "dbp" -> "舒张压"
    "fasting_glucose" -> "空腹血糖"
    "total_cholesterol" -> "总胆固醇"
    "ldl" -> "LDL"
    "hdl" -> "HDL"
    "triglycerides" -> "甘油三酯"
    "exercise_days" -> "运动天数"
    "smoking" -> "吸烟"
    "drinking" -> "饮酒"
    "diabetes_history" -> "糖尿病史"
    "hypertension_history" -> "高血压史"
    "family_history" -> "家族史"
    else -> this
}

private fun modelInputsFromRingState(state: RingUiState): List<ModelInputStatus> =
    listOf(
        ModelInputStatus(
            RingMetricType.HEART_RATE,
            "心率",
            "静息心率、日内波动",
            stageForMeasurement(state, RingMetricType.HEART_RATE),
        ),
        ModelInputStatus(
            RingMetricType.BLOOD_OXYGEN,
            "血氧",
            "均值、低值时长",
            stageForMeasurement(state, RingMetricType.BLOOD_OXYGEN),
        ),
        ModelInputStatus(
            RingMetricType.BLOOD_PRESSURE,
            "血压",
            "收缩压、舒张压",
            stageForMeasurement(state, RingMetricType.BLOOD_PRESSURE),
        ),
        ModelInputStatus(
            RingMetricType.SLEEP,
            "睡眠",
            "时长、阶段、连续性",
            if (state.sleep != null) ModelInputStage.FEATURE_EXTRACTED else ModelInputStage.LEARNING,
        ),
        ModelInputStatus(
            RingMetricType.TEMPERATURE,
            "体温",
            "个人基线偏差",
            stageForMeasurement(state, RingMetricType.TEMPERATURE),
        ),
        ModelInputStatus(
            RingMetricType.STEPS,
            "步数",
            "活动天数、运动频率",
            if (state.activity != null || state.measurements[RingMetricType.STEPS] != null) {
                ModelInputStage.FEATURE_EXTRACTED
            } else {
                ModelInputStage.LEARNING
            },
        ),
    )

private fun stageForMeasurement(state: RingUiState, type: RingMetricType): ModelInputStage =
    if (state.measurements[type] != null) ModelInputStage.FEATURE_EXTRACTED else ModelInputStage.LEARNING

@Composable
internal fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = if (value.contains("待")) Color(0xFFE39A22) else Mint, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
