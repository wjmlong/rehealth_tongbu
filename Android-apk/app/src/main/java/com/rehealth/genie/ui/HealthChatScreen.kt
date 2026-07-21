package com.rehealth.genie.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.network.HealthChatService
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingViewModel
import com.rehealth.genie.ui.theme.*
import kotlinx.coroutines.launch

/**
 * AI健康问答页面 - 连接GPT-5.6 Luna
 */
@Composable
fun HealthChatScreen(
    onBack: () -> Unit,
    ringViewModel: RingViewModel
) {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var currentModel by remember { mutableStateOf("DeepSeek V4 Pro") }
    val chatService = remember { HealthChatService() }
    val scope = rememberCoroutineScope()
    val ringState by ringViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "返回", tint = Ink)
            }
            Column(Modifier.weight(1f)) {
                Text("AI健康问答", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(currentModel, color = Muted, fontSize = 11.sp)
            }
            // 切换AI模型按钮
            IconButton(
                onClick = {
                    scope.launch {
                        if (currentModel.contains("Luna")) {
                            chatService.switchToDeepSeek()
                            currentModel = "DeepSeek V4 Pro (国内直连)"
                        } else {
                            chatService.switchToLuna()
                            currentModel = "GPT-5.6 Luna (需要VPN)"
                        }
                    }
                }
            ) {
                Icon(Icons.Outlined.Sync, "切换模型", tint = Mint, modifier = Modifier.size(20.dp))
            }
        }

        // 快捷问题
        if (messages.isEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("💬 问我一些健康问题", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("例如：", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }

                item {
                    QuickQuestionCard("我的睡眠质量如何改善？") {
                        sendMessage(it, messages, chatService, scope) { newMessages, loading ->
                            messages = newMessages
                            isLoading = loading
                        }
                    }
                }

                item {
                    QuickQuestionCard("如何提高心率变异性？") {
                        sendMessage(it, messages, chatService, scope) { newMessages, loading ->
                            messages = newMessages
                            isLoading = loading
                        }
                    }
                }

                item {
                    QuickQuestionCard("我的血压偏高，应该注意什么？") {
                        sendMessage(it, messages, chatService, scope) { newMessages, loading ->
                            messages = newMessages
                            isLoading = loading
                        }
                    }
                }

                item {
                    QuickQuestionCard("根据我的数据，给我健康建议") {
                        scope.launch {
                            isLoading = true
                            // Requirement A+B: 绑定用户真实体征；Requirement C: 无实时读数时回退到
                            // 本地 7 天聚合(由 Room 历史计算得出，非随机/写死)
                            val agg = runCatching { ringViewModel.loadPeriodAggregate(7) }.getOrNull()
                            val hr = ringState.measurements[RingMetricType.HEART_RATE]?.primaryValue ?: agg?.avgHeartRate
                            val spo2 = ringState.measurements[RingMetricType.BLOOD_OXYGEN]?.primaryValue ?: agg?.avgSpo2
                            val stepsVal = ringState.measurements[RingMetricType.STEPS]?.primaryValue ?: agg?.totalSteps?.toDouble()
                            val sleepMin = ringState.sleep?.let { (it.deepMinutes + it.lightMinutes + it.remMinutes + it.awakeMinutes).toDouble() } ?: agg?.avgSleepMinutes
                            val healthData = buildMap<String, String> {
                                put("心率", hr?.let { "%.0f bpm".format(it) } ?: "—")
                                put("血氧", spo2?.let { "%.0f%%".format(it) } ?: "—")
                                put("睡眠", sleepMin?.let { "${it.toInt() / 60}h${it.toInt() % 60}m" } ?: "—")
                                put("步数", stepsVal?.let { "%,d步".format(it.toLong()) } ?: "—")
                            }
                            val response = chatService.generateHealthInsight(healthData)
                            messages = messages + ChatMessage("根据我的数据，给我健康建议", true)
                            messages = messages + ChatMessage(response, false)
                            isLoading = false
                        }
                    }
                }
            }
        } else {
            // 聊天消息列表
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages.size) { index ->
                    ChatBubble(messages[index])
                }

                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .padding(12.dp)
                            ) {
                                Text("正在思考...", color = Muted, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // 输入框
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入健康问题...", color = Muted, fontSize = 13.sp) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Mint,
                    unfocusedBorderColor = Line
                ),
                maxLines = 3
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isLoading) {
                        sendMessage(inputText, messages, chatService, scope) { newMessages, loading ->
                            messages = newMessages
                            isLoading = loading
                        }
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) Mint else Line)
            ) {
                Icon(
                    Icons.Outlined.Send,
                    "发送",
                    tint = if (inputText.isNotBlank()) Color.White else Muted
                )
            }
        }
    }
}

@Composable
private fun QuickQuestionCard(question: String, onClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(question) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.QuestionAnswer, null, tint = Mint, modifier = Modifier.size(20.dp))
            Text(
                question,
                color = Ink,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MintSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.SmartToy, null, tint = Mint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (message.isUser) Mint else Color.White)
                .padding(12.dp)
        ) {
            Text(
                message.content,
                color = if (message.isUser) Color.White else Ink,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        if (message.isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5F2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Person, null, tint = Mint, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun sendMessage(
    text: String,
    currentMessages: List<ChatMessage>,
    chatService: HealthChatService,
    scope: kotlinx.coroutines.CoroutineScope,
    onUpdate: (List<ChatMessage>, Boolean) -> Unit
) {
    val userMessage = ChatMessage(text, true)
    onUpdate(currentMessages + userMessage, true)

    scope.launch {
        try {
            val response = chatService.ask(text)
            val aiMessage = ChatMessage(response, false)
            onUpdate(currentMessages + userMessage + aiMessage, false)
        } catch (e: Exception) {
            val errorMessage = ChatMessage("抱歉，连接失败：${e.message}", false)
            onUpdate(currentMessages + userMessage + errorMessage, false)
        }
    }
}

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
