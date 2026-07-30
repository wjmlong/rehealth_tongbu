package com.rehealth.genie.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.data.HealthChatMessageEntity
import com.rehealth.genie.data.HealthChatRepository
import com.rehealth.genie.ring.RingViewModel
import com.rehealth.genie.ui.theme.Canvas
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted

@Composable
fun HealthChatScreen(
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") ringViewModel: RingViewModel,
) {
    val application = LocalContext.current.applicationContext as ReHealthApplication
    val chatViewModel: HealthChatViewModel = viewModel(
        factory = remember(application) { HealthChatViewModel.Factory(application) },
    )
    val messages by chatViewModel.messages.collectAsState()
    val uiState by chatViewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "返回", tint = Ink)
            }
            Column(Modifier.weight(1f)) {
                Text("AI健康问答", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("睿禾健康助手（服务端托管）", color = Muted, fontSize = 11.sp)
            }
        }
        Text(
            "仅供健康参考，不能替代医疗诊断；严重或突发症状请及时就医。",
            color = Muted,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp),
        )

        if (messages.isEmpty()) {
            QuickQuestions(
                modifier = Modifier.weight(1f),
                enabled = !uiState.isLoading,
                onQuestion = chatViewModel::send,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages, key = { it.messageId }) { message ->
                    ChatBubble(message)
                }
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                .background(Color.White).padding(12.dp),
                        ) {
                            Text("正在结合你的健康画像思考…", color = Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        uiState.errorMessage?.let { error ->
            Text(
                error,
                color = Color(0xFFD94C4C),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().clickable(onClick = chatViewModel::clearError)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White)
                .navigationBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入健康问题…", color = Muted, fontSize = 13.sp) },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Mint,
                    unfocusedBorderColor = Line,
                ),
                maxLines = 3,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val question = inputText.trim()
                    if (question.isNotEmpty()) {
                        chatViewModel.send(question)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !uiState.isLoading,
                modifier = Modifier.size(48.dp).clip(CircleShape)
                    .background(if (inputText.isNotBlank()) Mint else Line),
            ) {
                Icon(
                    Icons.Outlined.Send,
                    "发送",
                    tint = if (inputText.isNotBlank()) Color.White else Muted,
                )
            }
        }
    }
}

@Composable
private fun QuickQuestions(
    modifier: Modifier,
    enabled: Boolean,
    onQuestion: (String) -> Unit,
) {
    val questions = listOf(
        "我的睡眠质量如何改善？",
        "如何提高心率变异性？",
        "我的血压偏高，应该注意什么？",
        "根据我的档案和最新健康数据给出建议",
    )
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("💬 问我一些健康问题", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("每轮回答都会结合已同步的健康画像", color = Muted, fontSize = 13.sp)
        }
        items(questions) { question ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { onQuestion(question) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.QuestionAnswer, null, tint = Mint, modifier = Modifier.size(20.dp))
                    Text(
                        question,
                        color = Ink,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    )
                    Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: HealthChatMessageEntity) {
    val isUser = message.role == HealthChatRepository.ROLE_USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(MintSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.SmartToy, null, tint = Mint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (isUser) Mint else Color.White).padding(12.dp),
            ) {
                if (isUser) {
                    Text(
                        message.content,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                } else {
                    SafeMarkdownText(message.content)
                }
            }
            if (message.deliveryStatus == HealthChatRepository.DELIVERY_FAILED) {
                Text("发送失败，已保存在本机", color = Color(0xFFD94C4C), fontSize = 10.sp)
            }
        }
        if (isUser) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFE8F5F2)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Person, null, tint = Mint, modifier = Modifier.size(18.dp))
            }
        }
    }
}
