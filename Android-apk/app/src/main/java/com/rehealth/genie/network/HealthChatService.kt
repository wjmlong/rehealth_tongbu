package com.rehealth.genie.network

import com.rehealth.genie.network.dto.HealthAgentMessageRequest
import java.util.UUID

/**
 * AI 健康问答服务。
 *
 * 所有生产问答都经已认证的 JeecgBoot 健康助手端点，由后端组装用户健康上下文并代理模型。
 * 当后端暂时不可用时，仅回退到保守的本地通用建议，不把本地兜底伪装成云端结果。
 */
class HealthChatService(
    private val apiClient: AuthenticatedApiClient,
) {
    fun currentModelName(): String = "睿禾健康助手（后端托管）"

    suspend fun ask(text: String): String {
        val request = HealthAgentMessageRequest(
            requestId = UUID.randomUUID().toString(),
            message = text.trim(),
        )
        return when (val result = apiClient.sendHealthAgentMessage(request)) {
            is ApiResult.Success -> result.data.answer
                ?.takeIf(String::isNotBlank)
                ?: localFallback(text)
            else -> localFallback(text)
        }
    }

    private fun localFallback(text: String): String = when {
        text.contains("睡眠") ->
            "离线通用建议（未结合云端档案）：保持规律作息、睡前1小时避免蓝光、卧室温度控制在18-22℃、减少咖啡因摄入。"
        text.contains("心率") || text.contains("HRV") || text.contains("心率变异性") ->
            "离线通用建议（未结合云端档案）：规律有氧运动、充足睡眠、进行压力管理并限制酒精。"
        text.contains("血压") ->
            "离线通用建议（未结合云端档案）：注意低盐饮食、规律运动、控制体重并记录血压；如有头晕胸闷请及时就医。"
        else ->
            "离线通用建议（未结合云端档案）：保持规律作息、均衡饮食与适度运动。如需更具体的建议，请描述关注指标。"
    }

    suspend fun generateHealthInsight(healthData: Map<String, String>): String {
        val summary = healthData.entries.joinToString("，") { "${it.key}: ${it.value}" }
        val request = HealthAgentMessageRequest(
            requestId = UUID.randomUUID().toString(),
            message = "请基于服务器中我的最新健康档案和趋势，给出保守、可执行的健康建议。",
        )
        return when (val result = apiClient.sendHealthAgentMessage(request)) {
            is ApiResult.Success -> result.data.answer
                ?.takeIf(String::isNotBlank)
                ?: "暂时无法生成个性化建议。当前本地数据：$summary。请继续关注长期趋势，如有不适及时就医。"
            else -> "健康助手暂时不可用。当前本地数据：$summary。请继续保持规律作息与适度运动，如有不适及时就医。"
        }
    }
}
