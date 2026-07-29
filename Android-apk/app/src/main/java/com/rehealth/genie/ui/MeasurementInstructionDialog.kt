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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted

internal data class MeasurementInstruction(
    val title: String,
    val steps: List<String>,
    val healthNotice: String,
)

internal fun measurementInstructionFor(type: RingMetricType): MeasurementInstruction? = when (type) {
    RingMetricType.ECG -> MeasurementInstruction(
        title = "ECG 测量说明",
        steps = listOf(
            "将设备贴合佩戴在手腕，确保背部传感器紧贴皮肤。",
            "用另一只手的一根手指持续触摸设备金属电极片。",
            "双臂放松并保持静止，不要说话或移动，直到测量完成。",
            "若提示接触不良，请调整佩戴并让手指完整覆盖电极片。",
        ),
        healthNotice = "这是便携式单导联 ECG，仅供健康参考，不能替代医疗诊断。",
    )

    RingMetricType.BODY_COMPOSITION -> MeasurementInstruction(
        title = "身体成分测量说明",
        steps = listOf(
            "将设备贴合佩戴，确保背部电极紧贴手腕皮肤。",
            "用另一只手的手指持续触摸金属电极片，形成完整测量回路。",
            "双臂自然分开，避免手臂或身体互相接触，并保持静止。",
            "保持手腕和手指清洁干燥，接触不良时请重新调整。",
        ),
        healthNotice = "身体成分由设备算法估算，仅供健康参考，不能替代医疗诊断。",
    )

    else -> null
}

@Composable
internal fun MeasurementInstructionDialog(
    metricType: RingMetricType,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val instruction = measurementInstructionFor(metricType) ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(instruction.title, color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                instruction.steps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MintSoft, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Mint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = step,
                            color = Ink,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                }
                Surface(color = MintSoft, shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = instruction.healthNotice,
                        color = Muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Mint),
            ) {
                Text("我已准备好，开始测量")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Muted)
            }
        },
        containerColor = Color.White,
    )
}
