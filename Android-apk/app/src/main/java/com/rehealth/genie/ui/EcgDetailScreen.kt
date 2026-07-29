package com.rehealth.genie.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.ring.RingEcgContactStatus
import com.rehealth.genie.ring.RingEcgLead
import com.rehealth.genie.ring.RingEcgMeasurementPhase
import com.rehealth.genie.ring.RingEcgWaveform
import com.rehealth.genie.ring.RingEcgWaveformDecoder
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ui.theme.Canvas as AppCanvas
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
internal fun EcgDetailScreen(
    state: RingUiState,
    onBack: () -> Unit,
    onMeasure: (RingMetricType) -> Unit,
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.ecgHistory.firstOrNull()?.id) {
        if (selectedId == null || state.ecgHistory.none { it.id == selectedId }) {
            selectedId = state.ecgHistory.firstOrNull()?.id
        }
    }
    val selected = state.ecgHistory.firstOrNull { it.id == selectedId }
        ?: state.ecgHistory.firstOrNull()
    val live = state.liveEcg
    val isMeasuring = state.measuringMetric == RingMetricType.ECG ||
        live.phase == RingEcgMeasurementPhase.PREPARING ||
        live.phase == RingEcgMeasurementPhase.MEASURING
    val savedWaveform = remember(selected?.id) {
        selected?.let(RingEcgWaveformDecoder::decode) ?: RingEcgWaveform(FloatArray(0), false)
    }
    val waveform = if (isMeasuring) {
        RingEcgWaveform(live.samplesMv, live.isCalibrated)
    } else {
        savedWaveform
    }
    val sampleRate = if (isMeasuring) live.sampleRateHz else selected?.sampleRateHz
    val drawFrequency = if (isMeasuring) live.drawFrequencyHz else selected?.drawFrequencyHz
    val durationSeconds = if (isMeasuring) {
        sampleRate?.takeIf { it > 0 }?.let { live.samplesMv.size / it }
    } else {
        selected?.durationSeconds ?: selected?.let { record ->
            sampleRate?.takeIf { it > 0 }?.let { record.sampleCount / it }
        }
    }
    val lead = if (isMeasuring) live.lead else RingEcgLead.fromStored(selected?.leadType)
    val heartRate = if (isMeasuring) {
        live.currentHeartRate ?: live.averageHeartRate
    } else {
        selected?.averageHeartRate
    }
    val contactStatus = if (isMeasuring) {
        live.contactStatus
    } else {
        RingEcgContactStatus.fromStored(selected?.contactQuality)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppCanvas),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回", tint = Ink)
                }
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text("单导联 ECG", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("便携式单导联记录，不是医院标准 12 导联心电图", color = Muted, fontSize = 11.sp)
                }
            }
        }

        if (isMeasuring || live.phase == RingEcgMeasurementPhase.FAILED) {
            item {
                LiveMeasurementCard(state)
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Favorite, null, tint = ECG_RED, modifier = Modifier.size(20.dp))
                        Text(
                            if (isMeasuring) "实时波形" else "历史波形",
                            color = Ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 7.dp),
                        )
                        Spacer(Modifier.weight(1f))
                        Text(lead.displayName, color = Mint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    EcgWaveformChart(waveform, Modifier.fillMaxWidth().height(230.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        EcgFact("心率", heartRate?.let { "$it bpm" } ?: "--")
                        EcgFact("采样率", sampleRate?.let { "$it Hz" } ?: "--")
                        EcgFact("时长", durationSeconds?.let { "$it 秒" } ?: "--")
                        EcgFact("幅值", if (waveform.isMillivolts) "mV" else "相对值")
                    }
                    Text(
                        listOfNotNull(
                            drawFrequency?.let { "绘制频率 $it Hz" },
                            selected?.ecgType?.let { "设备 ECG 类型 $it" },
                            contactStatus.takeUnless { it == RingEcgContactStatus.UNKNOWN }?.displayName,
                            selected?.startedAt?.takeUnless { isMeasuring }?.let(::formatEcgTime),
                        ).joinToString(" · ").ifEmpty { "设备未返回更多结构化信息" },
                        color = Muted,
                        fontSize = 10.sp,
                    )
                    Text(
                        if (waveform.isMillivolts) {
                            "波形已按 HBand 官方增益换算为 mV；页面按实际采样率显示时间范围。"
                        } else {
                            "该记录缺少可验证的增益或为升级前旧数据，仅显示相对幅值，不能按临床电压刻度解读。"
                        },
                        color = Muted,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        item {
            Button(
                onClick = { onMeasure(RingMetricType.ECG) },
                enabled = RingMetricType.ECG in state.supportedMetrics && !state.isSyncing,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.PlayArrow, null, modifier = Modifier.size(20.dp))
                Text(if (isMeasuring) "正在测量" else "开始新的 ECG 测量", modifier = Modifier.padding(start = 6.dp))
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MintSoft),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Mint.copy(alpha = 0.2f)),
            ) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(20.dp))
                    Text(
                        "设备 SDK 返回的疾病风险和异常分类不在本页作为诊断展示。ECG、心率及相关设备算法结果仅供健康参考，不能替代医疗诊断；如有胸痛、呼吸困难或持续不适，请及时就医。",
                        color = Ink,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }

        item {
            Text("历史记录", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        if (state.ecgHistory.isEmpty()) {
            item {
                Text("暂无已保存的 ECG 波形", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 18.dp))
            }
        } else {
            items(state.ecgHistory, key = { it.id }) { record ->
                EcgHistoryRow(record, selected = record.id == selected?.id) { selectedId = record.id }
            }
        }
    }
}

@Composable
private fun LiveMeasurementCard(state: RingUiState) {
    val live = state.liveEcg
    val failed = live.phase == RingEcgMeasurementPhase.FAILED
    Card(
        colors = CardDefaults.cardColors(containerColor = if (failed) Color(0xFFFFF1F1) else MintSoft),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (failed) ECG_RED.copy(alpha = .25f) else Mint.copy(alpha = .2f)),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(if (failed) ECG_RED else Mint, RoundedCornerShape(99.dp)))
                Text(
                    live.message ?: if (failed) "ECG 测量失败" else "正在采集单导联 ECG",
                    color = Ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 7.dp),
                )
            }
            if (!failed) {
                LinearProgressIndicator(
                    progress = { live.progress.coerceIn(0, 100) / 100f },
                    modifier = Modifier.fillMaxWidth().height(5.dp),
                    color = Mint,
                    trackColor = Color.White,
                )
            }
            Text(
                live.contactStatus.displayName,
                color = if (live.contactStatus == RingEcgContactStatus.POOR) ECG_RED else Muted,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
internal fun EcgDetailEntryCard(
    latest: RingSignalChunkEntity?,
    isMeasuring: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).background(ECG_RED.copy(alpha = .1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Favorite, null, tint = ECG_RED, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("查看单导联 ECG 详情", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                when {
                    isMeasuring -> "正在采集实时波形"
                    latest != null -> "最新记录 ${formatEcgTime(latest.startedAt)} · ${latest.sampleCount} 点"
                    else -> "查看实时测量和本机历史波形"
                },
                color = Muted,
                fontSize = 10.sp,
            )
        }
        Text("进入", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EcgWaveformChart(waveform: RingEcgWaveform, modifier: Modifier = Modifier) {
    val points = remember(waveform.samples) {
        RingEcgWaveformDecoder.downsample(waveform.samples, MAX_DRAW_POINTS)
    }
    Box(
        modifier = modifier.background(ECG_PAPER, RoundedCornerShape(12.dp))
            .border(1.dp, ECG_GRID_MAJOR.copy(alpha = .45f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            for (index in 0..20) {
                val x = size.width * index / 20f
                drawLine(
                    color = if (index % 5 == 0) ECG_GRID_MAJOR else ECG_GRID_MINOR,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = if (index % 5 == 0) 1.2f else .6f,
                )
            }
            for (index in 0..10) {
                val y = size.height * index / 10f
                drawLine(
                    color = if (index % 5 == 0) ECG_GRID_MAJOR else ECG_GRID_MINOR,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = if (index % 5 == 0) 1.2f else .6f,
                )
            }
            if (points.size < 2) return@Canvas
            val finite = points.filter(Float::isFinite)
            if (finite.size < 2) return@Canvas
            val maxAbs = if (waveform.isMillivolts) {
                max(.5f, finite.maxOf { abs(it) } * 1.15f)
            } else {
                max(1f, finite.maxOf { abs(it) })
            }
            val path = Path()
            points.forEachIndexed { index, value ->
                if (!value.isFinite()) return@forEachIndexed
                val x = index.toFloat() / (points.size - 1) * size.width
                val y = size.height / 2f - (value / maxAbs) * size.height * .44f
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, ECG_RED, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
        }
        if (points.isEmpty()) Text("等待 ECG 波形数据", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun EcgFact(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun EcgHistoryRow(record: RingSignalChunkEntity, selected: Boolean, onClick: () -> Unit) {
    val duration = record.durationSeconds
        ?: record.sampleRateHz?.takeIf { it > 0 }?.let { record.sampleCount / it }
    Row(
        modifier = Modifier.fillMaxWidth().background(
            if (selected) MintSoft else Color.White,
            RoundedCornerShape(14.dp),
        ).border(1.dp, if (selected) Mint.copy(alpha = .35f) else Line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Favorite, null, tint = ECG_RED, modifier = Modifier.size(19.dp))
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Text(formatEcgTime(record.startedAt), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${RingEcgLead.fromStored(record.leadType).displayName} · ${record.sampleCount} 点" +
                    (duration?.let { " · $it 秒" } ?: ""),
                color = Muted,
                fontSize = 10.sp,
            )
        }
        Text(record.averageHeartRate?.let { "$it bpm" } ?: "--", color = Mint, fontSize = 11.sp)
    }
}

private fun formatEcgTime(timestamp: Long): String = ECG_TIME_FORMATTER.format(
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()),
)

private val ECG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA)
private const val MAX_DRAW_POINTS = 1_500
private val ECG_RED = Color(0xFFE44A5A)
private val ECG_PAPER = Color(0xFFFFFAFA)
private val ECG_GRID_MINOR = Color(0xFFF6DADD)
private val ECG_GRID_MAJOR = Color(0xFFEAB7BD)
