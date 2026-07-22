package com.rehealth.genie.chat

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.rehealth.genie.BuildConfig
import com.rehealth.genie.logging.SafeLogValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * DeepSeek 大模型客户端（首页 AI 问答 / 健康助手）。
 *
 * 与 demo-simulated 的 DeepSeekClient 一致的实现：直接调用
 * `${baseUrl}/chat/completions`，模型固定 `deepseek-chat`。
 * API key 来自 [BuildConfig.DEEPSEEK_API_KEY]（由 local.properties 注入，绝不硬编码）。
 * 当 key 为空时返回友好的占位提示，不抛出异常。
 */
data class ChatTurn(val role: String, val content: String)

class DeepSeekClient(
    private val baseUrl: String = BuildConfig.DEEPSEEK_BASE_URL,
    private val apiKey: String = BuildConfig.DEEPSEEK_API_KEY,
) {
    suspend fun reply(history: List<ChatTurn>): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext "当前还没有配置大模型密钥，请在 local.properties 中配置 DEEPSEEK_API_KEY。"
        }

        val messages = JsonArray().apply {
            add(JsonObject().apply {
                addProperty("role", "system")
                addProperty(
                    "content",
                    "你是睿禾精灵的小禾灵，是综合健康管理助手。" +
                        "请用简洁、温和、易懂的中文回答。可以帮助用户理解健康数据、生活方式和戒指数据，" +
                        "但不能替代医生诊断；遇到胸痛、呼吸困难、昏厥等紧急情况时提醒用户立即就医。" +
                        "不要编造测量结果，也不要声称已经读取到未提供的数据。",
                )
            })
            history.forEach { turn ->
                add(JsonObject().apply {
                    addProperty("role", turn.role)
                    addProperty("content", turn.content)
                })
            }
        }
        val requestBody = JsonObject().apply {
            addProperty("model", "deepseek-chat")
            add("messages", messages)
            addProperty("temperature", 0.3)
            addProperty("stream", false)
        }.toString()

        val connection = (URL("${baseUrl.trimEnd('/')}/chat/completions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                Log.w("DeepSeekClient", "non-2xx code=$responseCode responseLength=${responseText.length}")
                return@withContext "服务暂时不可用（$responseCode），请稍后再试。"
            }
            val root = JsonParser.parseString(responseText).asJsonObject
            root["choices"]?.asJsonArray?.firstOrNull()
                ?.asJsonObject?.getAsJsonObject("message")?.get("content")?.asString
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "我暂时没有生成有效回答，请换一种方式描述。"
        } catch (e: Exception) {
            Log.w("DeepSeekClient", "request failed error=${SafeLogValues.exceptionType(e)}")
            "暂时无法连接健康助手，请检查网络后重试。"
        } finally {
            connection.disconnect()
        }
    }
}
