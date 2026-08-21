package com.rehealth.genie.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rehealth.genie.R
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.data.HealthChatMessageEntity
import com.rehealth.genie.data.HealthChatConversationEntity
import com.rehealth.genie.data.HealthChatRepository
import com.rehealth.genie.data.profileAvatarStorageKey
import com.rehealth.genie.network.PatientInterventionPayload
import com.rehealth.genie.network.dto.BehaviorRecordDto
import com.rehealth.genie.phm.Intervention
import com.rehealth.genie.ui.theme.Canvas
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted

@Composable
internal fun HomeScreen() {
    var input by remember { mutableStateOf("") }
    var showActions by remember { mutableStateOf(false) }
    var voiceMessage by remember { mutableStateOf<String?>(null) }
    var showMicrophonePermissionDialog by remember { mutableStateOf(false) }
    var showMicrophoneSettingsDialog by remember { mutableStateOf(false) }
    var showConversationManager by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<HealthChatConversationEntity?>(null) }
    var showClearConversationsConfirmation by remember { mutableStateOf(false) }
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val application = context.applicationContext as ReHealthApplication
    val chatOwnerKey = remember(application.sessionStore.userId, application.sessionStore.username) {
        profileAvatarStorageKey(
            application.sessionStore.userId ?: application.sessionStore.username ?: "signed-out",
        )
    }
    val chatViewModel: HealthChatViewModel = viewModel(
        key = "health-chat-$chatOwnerKey",
        factory = remember(application) { HealthChatViewModel.Factory(application) },
    )
    val messages by chatViewModel.messages.collectAsState()
    val conversations by chatViewModel.conversations.collectAsState()
    val activeConversationId by chatViewModel.activeConversationId.collectAsState()
    val chatState by chatViewModel.uiState.collectAsState()
    val behaviorViewModel: BehaviorRecordViewModel = viewModel(
        key = "behavior-records-$chatOwnerKey",
        factory = remember(application) { BehaviorRecordViewModel.Factory(application) },
    )
    val behaviorState by behaviorViewModel.state.collectAsState()

    LaunchedEffect(chatOwnerKey) {
        behaviorViewModel.refreshToday()
    }

    val session = application.sessionStore
    val displayName = session.realname?.takeIf(String::isNotBlank)
        ?: session.username?.takeIf(String::isNotBlank)
    val messageListState = rememberLazyListState()
    val latestMessageId = messages.lastOrNull()?.messageId
    LaunchedEffect(activeConversationId, latestMessageId, chatState.isLoading) {
        if (messages.isEmpty() && !chatState.isLoading) return@LaunchedEffect
        withFrameNanos { }
        val lastItemIndex = messageListState.layoutInfo.totalItemsCount - 1
        if (lastItemIndex >= 0) {
            messageListState.animateScrollToItem(lastItemIndex)
        }
    }
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
        val question = text.trim()
        if (question.isBlank() || chatState.isLoading) return
        chatViewModel.send(question)
        input = ""
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val recognized = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!recognized.isNullOrBlank()) {
                input = recognized
                voiceMessage = "语音已转换为文字，请确认后发送"
            } else {
                voiceMessage = "没有听清，请再试一次或使用键盘输入"
            }
        }
    }
    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出你的健康问题")
        }
        runCatching { voiceLauncher.launch(intent) }
            .onFailure { voiceMessage = "当前设备没有可用的系统语音输入，请使用键盘输入" }
    }
    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startVoiceInput()
        } else {
            voiceMessage = "麦克风权限未授权，你仍可以使用键盘输入"
            showMicrophoneSettingsDialog = true
        }
    }
    fun requestVoiceInput() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceInput()
        } else {
            showMicrophonePermissionDialog = true
        }
    }

    fun createPhotoTarget(): Pair<File, Uri> {
        val directory = File(context.cacheDir, "behavior-photos").apply { mkdirs() }
        val file = File.createTempFile("behavior-", ".jpg", directory)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val photoFile = pendingPhotoPath?.let(::File)
        pendingPhotoPath = null
        if (captured && photoFile != null) {
            behaviorViewModel.analyzePhoto(photoFile)
        } else if (photoFile != null) {
            runCatching { photoFile.delete() }
            voiceMessage = "已取消拍照"
        }
    }
    fun startCamera() {
        runCatching {
            createPhotoTarget().also { (photoFile, uri) ->
                pendingPhotoPath = photoFile.absolutePath
                cameraLauncher.launch(uri)
            }
        }.onFailure {
            voiceMessage = "当前设备无法打开系统相机"
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else voiceMessage = "未授权相机权限，无法拍照记录"
    }
    fun requestCamera() {
        showActions = false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
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
            IconButton(onClick = chatViewModel::newConversation) {
                Icon(Icons.Outlined.AddComment, "新建对话", tint = Ink)
            }
            IconButton(onClick = { showConversationManager = true }) {
                Icon(Icons.Outlined.History, "本机会话", tint = Ink)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.NotificationsNone, "通知", tint = Ink)
            }
        }

        LazyColumn(
            state = messageListState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            userScrollEnabled = true,
        ) {
            item(key = "hero") {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        displayName?.let { "$greetingPrefix，$it" } ?: greetingPrefix,
                        color = Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "今天想从哪里开始？",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                }
            }
            if (behaviorState.records.isNotEmpty() || behaviorState.isLoading || behaviorState.isUploading) {
                item(key = "today-behavior") {
                    HomeTodayBehaviorCard(behaviorState)
                }
            }
            items(messages, key = { it.messageId }) { message ->
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                    HomeChatPreview(message)
                }
            }
            if (chatState.isLoading) {
                item(key = "loading") {
                    Text(
                        "小禾灵正在结合你的健康画像思考…",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
                    )
                }
            }
            chatState.errorMessage?.let { error ->
                item(key = "error") {
                    Text(
                        error,
                        color = Color(0xFFD94C4C),
                        fontSize = 11.sp,
                        modifier = Modifier.clickable(onClick = chatViewModel::clearError)
                            .padding(horizontal = 22.dp, vertical = 4.dp),
                    )
                }
            }
            item(key = "bottom-space") { Spacer(Modifier.height(8.dp)) }
        }

        if (showActions) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HomeQuickAction(Icons.Outlined.AddAPhoto, "拍照记录", Modifier.weight(1f)) {
                    requestCamera()
                }
                HomeQuickAction(Icons.Outlined.Assessment, "健康记录", Modifier.weight(1f)) {
                    input = "请结合我的健康记录给出建议"
                    showActions = false
                }
                HomeQuickAction(Icons.Outlined.Devices, "戒指同步", Modifier.weight(1f)) {
                    input = "请分析我最近同步的可穿戴设备数据"
                    showActions = false
                }
            }
        }

        voiceMessage?.let { message ->
            Text(
                message,
                color = Muted,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            )
        }
        (behaviorState.message ?: behaviorState.error)?.let { message ->
            Text(
                message,
                color = if (behaviorState.error == null) Mint else Color(0xFFD94C4C),
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().clickable(onClick = behaviorViewModel::clearNotice)
                    .padding(horizontal = 22.dp, vertical = 2.dp),
            )
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
                onClick = ::requestVoiceInput,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Mint),
            ) {
                Icon(Icons.Outlined.MicNone, "系统语音输入", tint = Color.White)
            }
        }
    }

    if (showMicrophonePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showMicrophonePermissionDialog = false },
            title = { Text("使用系统语音输入") },
            text = { Text("授权后，系统语音服务会把本次问题转换为文字供你确认。睿禾精灵不会保存录音。") },
            confirmButton = {
                Button(
                    onClick = {
                        showMicrophonePermissionDialog = false
                        microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                ) {
                    Text("授权并继续")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicrophonePermissionDialog = false }) {
                    Text("使用键盘")
                }
            },
        )
    }

    if (showMicrophoneSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showMicrophoneSettingsDialog = false },
            title = { Text("麦克风权限未开启") },
            text = { Text("如需系统语音输入，请在系统设置中允许睿禾精灵使用麦克风。") },
            confirmButton = {
                Button(
                    onClick = {
                        showMicrophoneSettingsDialog = false
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                ) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicrophoneSettingsDialog = false }) {
                    Text("使用键盘")
                }
            },
        )
    }

    if (showConversationManager) {
        ConversationManagerDialog(
            conversations = conversations,
            activeConversationId = activeConversationId,
            onNewConversation = {
                chatViewModel.newConversation()
                showConversationManager = false
            },
            onSelectConversation = { conversationId ->
                chatViewModel.selectConversation(conversationId)
                showConversationManager = false
            },
            onDeleteConversation = { conversation ->
                showConversationManager = false
                conversationToDelete = conversation
            },
            onClearConversations = {
                showConversationManager = false
                showClearConversationsConfirmation = true
            },
            onDismiss = { showConversationManager = false },
        )
    }

    conversationToDelete?.let { conversation ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("删除本机会话？") },
            text = {
                Text("“${conversation.title}”及其消息将从本机删除。云端完整历史不会因此删除。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatViewModel.deleteLocalConversation(conversation.conversationId)
                        conversationToDelete = null
                    },
                ) {
                    Text("删除", color = Color(0xFFD94C4C))
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) { Text("取消") }
            },
        )
    }

    if (showClearConversationsConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConversationsConfirmation = false },
            title = { Text("清空本机会话？") },
            text = { Text("当前账号的全部本机会话和消息将被清空。云端完整历史不会因此删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatViewModel.clearLocalConversations()
                        showClearConversationsConfirmation = false
                        showConversationManager = false
                    },
                ) {
                    Text("清空", color = Color(0xFFD94C4C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConversationsConfirmation = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun ConversationManagerDialog(
    conversations: List<HealthChatConversationEntity>,
    activeConversationId: String?,
    onNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (HealthChatConversationEntity) -> Unit,
    onClearConversations: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 2.dp,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "历史对话",
                        color = Ink,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, "关闭", tint = Muted)
                    }
                }
                Text(
                    "对话保存在当前账号下，切换账号不会混用。",
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                if (conversations.isEmpty()) {
                    Text(
                        "暂无历史对话",
                        color = Muted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(conversations.size, key = { conversations[it].conversationId }) { index ->
                            val conversation = conversations[index]
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (conversation.conversationId == activeConversationId) {
                                            MintSoft
                                        } else {
                                            Color(0xFFF7F9F8)
                                        },
                                    )
                                    .clickable {
                                        onSelectConversation(conversation.conversationId)
                                    }
                                    .padding(start = 12.dp, top = 9.dp, bottom = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        conversation.title,
                                        color = Ink,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                    Text(
                                        formatSyncTime(conversation.updatedAt),
                                        color = Muted,
                                        fontSize = 10.sp,
                                    )
                                }
                                if (conversation.conversationId == activeConversationId) {
                                    Text("当前", color = Mint, fontSize = 10.sp)
                                }
                                IconButton(onClick = { onDeleteConversation(conversation) }) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        "删除本地对话",
                                        tint = Color(0xFFD94C4C),
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onClearConversations, enabled = conversations.isNotEmpty()) {
                        Text("清空本地", color = Color(0xFFD94C4C))
                    }
                    Button(
                        onClick = onNewConversation,
                        colors = ButtonDefaults.buttonColors(containerColor = Mint),
                    ) {
                        Text("新建对话")
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeChatPreview(message: HealthChatMessageEntity) {
    val isUser = message.role == HealthChatRepository.ROLE_USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier.widthIn(max = 320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isUser) MintSoft else Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (isUser) {
                Text(message.content, color = Ink, fontSize = 12.sp, lineHeight = 17.sp)
            } else {
                SafeMarkdownText(message.content, fontSize = 12.sp)
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
private fun HomeTodayBehaviorCard(state: BehaviorRecordUiState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp)).background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(18.dp)).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("今日行为记录", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("相机照片由 AI 识别，营养数据仅为估算", color = Muted, fontSize = 9.sp)
            }
            if (state.isUploading || state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Mint)
            } else {
                Text("${state.records.size} 条", color = Mint, fontSize = 10.sp)
            }
        }
        state.records.take(3).forEach { record ->
            HomeBehaviorRecordRow(record)
        }
        if (state.isUploading) {
            Text("正在压缩、上传并识别照片…", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun HomeBehaviorRecordRow(record: BehaviorRecordDto) {
    val glyph = when (record.category?.uppercase(Locale.ROOT)) {
        "FOOD" -> "餐"
        "OCR" -> "文"
        else -> "记"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(12.dp))
            .background(MintSoft.copy(alpha = 0.55f)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Text(glyph, color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(record.title ?: "照片记录", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                record.ocrText?.takeIf(String::isNotBlank) ?: record.summary ?: "已完成识别",
                color = Muted,
                fontSize = 9.sp,
                maxLines = 2,
                modifier = Modifier.padding(top = 2.dp),
            )
            record.caloriesKcal?.let { calories ->
                Text("约 ${calories.toInt()} kcal", color = Mint, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Text(
            record.occurredAt?.let { SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(it)) } ?: "今天",
            color = Muted,
            fontSize = 9.sp,
        )
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
