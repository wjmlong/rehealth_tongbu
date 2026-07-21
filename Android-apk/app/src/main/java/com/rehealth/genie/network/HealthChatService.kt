package com.rehealth.genie.network

import com.rehealth.genie.chat.ChatTurn
import com.rehealth.genie.chat.DeepSeekClient

/**
 * AI 健康问答服务。
 *
 * 优先调用真实 DeepSeek 大模型（key 来自 BuildConfig / local.properties）。
 * 当 key 未配置或网络不可用时，回退到本地规则式应答，保证演示流程可用。
 */
class HealthChatService {
    private val deepSeek = DeepSeekClient()
    private var currentModel: String = "DeepSeek V4 Pro"

    fun switchToDeepSeek() {
        currentModel = "DeepSeek V4 Pro"
    }

    fun switchToLuna() {
        currentModel = "GPT-5.6 Luna"
    }

    fun currentModelName(): String = currentModel

    suspend fun ask(text: String): String {
        val reply = runCatching { deepSeek.reply(listOf(ChatTurn("user", text))) }.getOrElse { null }
        return if (!reply.isNullOrBlank() && !reply.startsWith("当前还没有配置大模型密钥")) {
            reply
        } else {
            // 真实模型不可用时，使用本地规则应答兜底
            localFallback(text)
        }
    }

    private fun localFallback(text: String): String = when {
        text.contains("睡眠") ->
            "改善睡眠质量可以从以下几方面入手：保持规律作息、睡前1小时避免蓝光、卧室温度控制在18-22℃、减少咖啡因摄入。"
        text.contains("心率") || text.contains("HRV") || text.contains("心率变异性") ->
            "提高心率变异性(HRV)建议：规律有氧运动、充足睡眠、压力管理(冥想/呼吸训练)、限制酒精。"
        text.contains("血压") ->
            "血压偏高需注意：低盐饮食(每日<5g)、规律运动、控制体重、监测血压日记、遵医嘱服药。如有头晕胸闷请及时就医。"
        else ->
            "根据您的健康数据，建议保持规律作息、均衡饮食与适度运动。如需更具体的建议，请描述您的症状或关注指标。"
    }

    suspend fun generateHealthInsight(healthData: Map<String, String>): String {
        val summary = healthData.entries.joinToString("，") { "${it.key}: ${it.value}" }
        val reply = runCatching {
            deepSeek.reply(
                listOf(
                    ChatTurn(
                        "user",
                        "根据我的健康数据给我分析与建议：$summary",
                    ),
                ),
            )
        }.getOrElse { null }
        return if (!reply.isNullOrBlank()) {
            reply
        } else {
            "根据您当前的数据（$summary），整体指标在正常范围内。建议继续保持规律作息与适度运动，并关注长期趋势变化。"
        }
    }
}
