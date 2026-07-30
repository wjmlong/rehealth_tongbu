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
import com.rehealth.genie.phm.ModelInputStage
import com.rehealth.genie.phm.ModelInputStatus
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted

@Composable
internal fun ModelScreen(
    state: RingUiState,
    canonicalRiskStatus: androidx.compose.runtime.State<RemoteFeatureEvaluateStatus?>,
) {
    val inputs = remember(state.measurements, state.sleep, state.activity) { modelInputsFromRingState(state) }
    val current = canonicalRiskStatus.value
    val profile = AttributionDataProvenance.trustedProfile(state.patientMvp)
    val coreFactors = remember(state, current) {
        AttributionUiMapper.mapCore16Factors(
            // The model page exposes the 16 input fields and availability, not internal SHAP/contribution values.
            evaluation = null,
            values = attributionFactorValues(state, profile),
        )
    }
    Page("健康模型", "结合本机健康数据进行云端风险评估") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                StatusRow("风险等级", current?.riskLevel.riskLevelLabel())
                StatusRow("风险分数", current?.riskScore.riskScoreLabel())
                StatusRow("模型版本", current?.modelVersion ?: "待返回")
                Text(
                    current?.summary ?: "正在整理本机健康数据，评估完成后将在这里展示结果。",
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            SectionTitle("健康评估进度")
            ReHealthCardBlock {
                ModelPipelineRow("1", "健康数据采集", "${state.collectedMetricCount} 项数据", state.collectedMetricCount > 0)
                ModelPipelineRow("2", "风险评估", current?.riskScore?.let { "今日已完成" } ?: "等待足够数据", current?.riskScore != null)
                ModelPipelineRow("3", "个性化建议", current?.summary?.let { "已生成" } ?: "等待评估结果", current?.summary != null)
            }
            SectionTitle("戒指健康数据输入")
            ReHealthCardBlock {
                inputs.forEachIndexed { index, input ->
                    ModelInputRow(input)
                    if (index != inputs.lastIndex) HorizontalDivider(color = Line)
                }
            }
            SectionTitle("现有 CVD 16 项输入")
            Core16InputCard(coreFactors)
            SectionTitle("个性化学习状态")
            ReHealthCardBlock {
                StatusRow("健康档案", if (state.patientMvp?.profile != null) "已读取" else "待补充")
                StatusRow("可穿戴数据", if (state.collectedMetricCount > 0) "已读取 ${state.collectedMetricCount} 项" else "待同步")
                StatusRow("今日评估", if (current?.riskScore != null) "已完成" else "等待结果")
            }
            SectionTitle("隐私与数据状态")
            ReHealthCardBlock {
                StatusRow("原始健康信号上传", "否")
                StatusRow("规范化健康指标", "按授权同步")
                StatusRow("模型服务连接", "由云端安全管理")
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
            Text(
                "当前模型版本：${current?.modelVersion ?: "等待评估"}",
                color = Muted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Core16InputCard(groups: List<AttributionFactorGroupUi>) {
    ReHealthCardBlock {
        groups.forEachIndexed { groupIndex, group ->
            Text(
                group.title,
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = if (groupIndex == 0) 0.dp else 12.dp, bottom = 3.dp),
            )
            group.factors.forEachIndexed { factorIndex, factor ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(factor.label, color = Ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(
                        factor.value ?: "待补充",
                        color = if (factor.value == null) Color(0xFFE39A22) else Mint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (factorIndex != group.factors.lastIndex) {
                    HorizontalDivider(color = Line)
                }
            }
        }
        Text(
            "这里只展示模型输入值与缺失状态，不展示内部贡献值。",
            color = Muted,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
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

internal fun modelInputsFromRingState(state: RingUiState): List<ModelInputStatus> =
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
