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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.R
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.network.PatientInterventionPayload
import com.rehealth.genie.phm.Intervention
import com.rehealth.genie.ui.theme.Canvas
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreen(onStartInterview: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var showActions by remember { mutableStateOf(false) }
    var lastMessage by remember { mutableStateOf<String?>(null) }
    var aiReply by remember { mutableStateOf<String?>(null) }
    var isAsking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val chatService = remember { com.rehealth.genie.network.HealthChatService() }
    val session = (LocalContext.current.applicationContext as? ReHealthApplication)?.sessionStore
    val greetingPrefix = remember {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            h < 6 -> "凌晨好"
            h < 12 -> "早上好"
            h < 14 -> "中午好"
            h < 18 -> "下午好"
            else -> "晚上好"
        }
    }

    fun askAi(text: String) {
        if (text.isBlank() || isAsking) return
        lastMessage = text
        input = ""
        isAsking = true
        aiReply = null
        scope.launch {
            val reply = runCatching { chatService.ask(text) }.getOrDefault("暂时无法连接健康助手，请检查网络后重试。")
            aiReply = reply
            isAsking = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Canvas).statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("小禾灵", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("你的健康伙伴一直在这里", color = Muted, fontSize = 11.sp)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.NotificationsNone, "通知", tint = Ink)
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier.size(250.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color.White, MintSoft, Color.Transparent))),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.xiaohelin),
                    contentDescription = "小禾灵",
                    modifier = Modifier.size(230.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Text(
                if (session?.username.isNullOrBlank()) greetingPrefix else "$greetingPrefix，${session?.username}",
                color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            )
            Text("今天想从哪里开始？", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))

            lastMessage?.let {
                Text(
                    "你：$it",
                    color = Ink,
                    fontSize = 12.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp)).background(MintSoft)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
                if (isAsking) {
                    Text(
                        "小禾灵正在思考…",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
                    )
                } else {
                    aiReply?.let { reply ->
                        Text(
                            "小禾灵：$reply",
                            color = Ink,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp)).background(Color.White)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
            } ?: Spacer(Modifier.height(12.dp))

            Spacer(Modifier.weight(1f))
        }

        if (showActions) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HomeQuickAction(Icons.Outlined.AddAPhoto, "拍照记录", Modifier.weight(1f)) {
                    lastMessage = "我想拍照记录饮食或报告"
                    showActions = false
                }
                HomeQuickAction(Icons.Outlined.Assessment, "健康记录", Modifier.weight(1f)) {
                    lastMessage = "打开我的健康记录"
                    showActions = false
                }
                HomeQuickAction(Icons.Outlined.Devices, "戒指同步", Modifier.weight(1f)) {
                    lastMessage = "查看智能戒指状态"
                    showActions = false
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { showActions = !showActions },
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MintSoft),
            ) {
                Icon(Icons.Outlined.Add, "更多", tint = Mint)
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("问问小禾灵…", color = Muted) },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                shape = RoundedCornerShape(22.dp),
                maxLines = 2,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSend = {
                        askAi(input.trim())
                    },
                ),
                trailingIcon = {
                    if (input.isNotBlank()) {
                        IconButton(
                            onClick = {
                                askAi(input.trim())
                            },
                        ) {
                            Icon(Icons.Outlined.ChatBubbleOutline, "发送", tint = Mint)
                        }
                    }
                },
            )
            IconButton(
                onClick = onStartInterview,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Mint),
            ) {
                Icon(Icons.Outlined.MicNone, "实时语音", tint = Color.White)
            }
        }
    }
}

@Composable
private fun PatientPlanRow(item: PatientInterventionPayload, feedbackViewModel: InterventionFeedbackViewModel) {
    val feedbackState by feedbackViewModel.uiState.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FCFA)).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.TaskAlt, null, tint = Mint, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(item.title ?: "干预计划", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(item.action ?: item.reason ?: "按计划完成今日任务", color = Muted, fontSize = 9.sp, maxLines = 1)
        }
        Column(
            modifier = Modifier.widthIn(min = 172.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FeedbackButton(
                    label = "完成",
                    icon = Icons.Outlined.Check,
                    containerColor = Mint,
                    contentColor = Color.White,
                    enabled = !feedbackState.isSubmitting && item.id != null,
                    onClick = {
                        item.id?.let { interventionId ->
                            feedbackViewModel.submitFeedback(interventionId, "completed", null)
                        }
                    },
                )
                FeedbackButton(
                    label = "不适用",
                    icon = Icons.Outlined.Close,
                    containerColor = Color.White,
                    contentColor = Mint,
                    enabled = !feedbackState.isSubmitting && item.id != null,
                    onClick = {
                        item.id?.let { interventionId ->
                            feedbackViewModel.submitFeedback(interventionId, "not_applicable", null)
                        }
                    },
                )
                FeedbackButton(
                    label = "稍后",
                    containerColor = Color.White,
                    contentColor = Muted,
                    enabled = !feedbackState.isSubmitting && item.id != null,
                    onClick = {
                        item.id?.let { interventionId ->
                            feedbackViewModel.submitFeedback(interventionId, "skipped", null)
                        }
                    },
                )
            }
            feedbackState.message?.let { message ->
                Text(
                    text = message,
                    color = Mint,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FeedbackButton(
    label: String,
    icon: ImageVector? = null,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        modifier = Modifier.height(30.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(label, fontSize = 10.sp)
    }
}

@Composable
private fun HomeQuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = Mint, modifier = Modifier.size(19.dp))
        Text(label, color = Ink, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun ActionTile(icon: ImageVector, text: String, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp)).clickable { }.padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Mint, modifier = Modifier.size(19.dp))
        }
        Text(text, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
private fun InterventionCard(item: Intervention) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.TaskAlt, null, tint = Mint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(item.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${item.reason} · ${item.duration}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Text("去完成", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun Metric(label: String, value: String, state: String, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(14.dp)).padding(10.dp),
    ) {
        Text(label, color = Muted, fontSize = 10.sp)
        Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        Text(state, color = Mint, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
    }
}
