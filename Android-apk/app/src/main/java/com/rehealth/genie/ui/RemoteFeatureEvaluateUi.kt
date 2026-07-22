package com.rehealth.genie.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.features.HealthFeatureExtractor
import com.rehealth.genie.features.HealthMemorySnapshot
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted

internal fun String?.riskLevelLabel(): String = when (this?.lowercase()) {
    "low" -> "低"
    "moderate" -> "中"
    "medium" -> "中"
    "high" -> "高"
    "very_high" -> "很高"
    null -> "待评估"
    else -> this
}

internal fun Double?.riskScoreLabel(): String =
    this?.let { "${(it * 100).toInt()}分" } ?: "--"

/**
 * Minimal remote feature-evaluate UI status. Holds only display strings, not raw health data.
 * Built to keep business logic outside the composable: parsing happens in
 * [refreshRemoteFeatureEvaluateStatus]; the composable only renders [summary].
 */
internal data class RemoteFeatureEvaluateStatus(
    val reachable: Boolean,
    val modelVersion: String?,
    val isMock: Boolean?,
    val riskLevel: String?,
    val riskScore: Double?,
    val featureContributions: Map<String, Double> = emptyMap(),
    val requestId: String? = null,
    val usedMockFallback: Boolean,
    val fallbackReason: String? = null,
    val missingFields: List<String> = emptyList(),
    val qualityWarnings: List<String> = emptyList(),
    val summary: String,
) {
    val modeLabel: String
        get() = when {
            usedMockFallback -> "本地mock兜底"
            isMock == true -> "云端mock"
            reachable -> "云端"
            else -> "不可用"
        }
}

internal fun RemoteFeatureEvaluateStatus.toAttributionRiskEvaluation(): AttributionRiskEvaluation =
    AttributionRiskEvaluation(
        riskScore = riskScore,
        riskLevel = riskLevel,
        contributions = featureContributions,
        confirmed = reachable && isMock == false && !usedMockFallback,
    )

internal suspend fun refreshRemoteFeatureEvaluateStatus(
    application: com.rehealth.genie.ReHealthApplication,
    state: RingUiState,
    target: androidx.compose.runtime.MutableState<RemoteFeatureEvaluateStatus?>,
) {
    val now = System.currentTimeMillis()
    val since = now - RISK_FEATURE_LOOKBACK_MILLIS
    val dao = application.database.ringDataDao()
    val measurements = runCatching { dao.getMeasurementsSince(since) }.getOrDefault(emptyList())
    val activities = runCatching { dao.getActivitiesSince(since) }.getOrDefault(emptyList())
    val vector = HealthFeatureExtractor(nowProvider = { now }).extract(
        HealthMemorySnapshot.fromPatientProfile(
            profile = AttributionDataProvenance.trustedProfile(state.patientMvp),
            ringMeasurements = measurements,
            ringActivities = activities,
            ringSleepSessions = state.sleep?.let { listOf(it) }.orEmpty(),
        ),
    )

    val outcome = application.remotePhmService.evaluateFeatures(vector)
    val result = outcome.result
    if (result != null) {
        application.riskHistoryRepository.recordConfirmedRemoteRisk(result)
        target.value = RemoteFeatureEvaluateStatus(
            reachable = true,
            modelVersion = result.normalizedModelVersion,
            isMock = result.normalizedIsMock,
            riskLevel = result.normalizedRiskLevel,
            riskScore = result.normalizedRiskScore,
            featureContributions = result.normalizedFeatureContributions,
            requestId = result.normalizedRequestId ?: outcome.requestId,
            usedMockFallback = false,
            missingFields = result.normalizedMissingFields,
            qualityWarnings = result.normalizedQualityWarnings,
            summary = result.summary ?: "后端已基于本机特征完成风险评估。",
        )
    } else {
        target.value = RemoteFeatureEvaluateStatus(
            reachable = false,
            modelVersion = null,
            isMock = null,
            riskLevel = null,
            riskScore = null,
            requestId = outcome.requestId,
            usedMockFallback = false,
            fallbackReason = outcome.mockFallbackReason,
            missingFields = vector.missingFields,
            summary = "暂时无法完成风险评估，请检查网络和登录状态后重试。" +
                "（${outcome.error?.eventName ?: "unavailable"}）",
        )
    }
}

private const val RISK_FEATURE_LOOKBACK_MILLIS = 7L * 24L * 60L * 60L * 1000L

@Composable
internal fun RemoteFeatureEvaluateRow(status: androidx.compose.runtime.State<RemoteFeatureEvaluateStatus?>) {
    val current = status.value
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (current?.reachable == true) MintSoft else Color(0xFFFFF2D8)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.SmartToy,
                "云端特征评估",
                tint = if (current?.reachable == true) Mint else Color(0xFFD38B18),
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text("云端特征评估", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                current?.summary ?: "正在提取本机特征并请求后端评估…",
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
        val riskLabel = current?.riskScore?.let { "${(it * 100).toInt()}分" } ?: "--"
        Column(horizontalAlignment = Alignment.End) {
            Text(current?.modeLabel ?: "检查中", color = Muted, fontSize = 11.sp)
            Text(riskLabel, color = Mint, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
