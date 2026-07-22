package com.rehealth.genie.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rehealth.genie.R
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.features.HealthFeatureExtractor
import com.rehealth.genie.features.HealthMemorySnapshot
import com.rehealth.genie.network.PatientInterventionPayload
import com.rehealth.genie.phm.Intervention
import com.rehealth.genie.phm.ModelInputStage
import com.rehealth.genie.phm.ModelInputStatus
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ring.RingViewModel
import com.rehealth.genie.ring.PeriodAggregate
import com.rehealth.genie.ui.theme.Canvas
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.rehealth.genie.ui.components.QueueStatusBanner
import com.rehealth.genie.work.MeasurementSyncWorker

private enum class AppStage { Splash, Login, Register, InterviewSession, DeviceSetup, Main }
private enum class Tab(val label: String, val icon: ImageVector) {
    Home("首页", Icons.Outlined.Home),
    Data("数据", Icons.Outlined.ShowChart),
    Attribution("归因", Icons.Outlined.Assessment),
    Model("模型", Icons.Outlined.SmartToy),
    Profile("我的", Icons.Outlined.PersonOutline),
}

private data class RingMetricUi(
    val type: RingMetricType,
    val title: String,
    val value: String,
    val unit: String,
    val status: String,
    val icon: ImageVector,
    val color: Color,
    val manualMeasure: Boolean = false,
    val actionLabel: String = "测量",
    val measuringLabel: String = "测量中",
)

@Composable
fun ReHealthApp() {
    val application = LocalContext.current.applicationContext as ReHealthApplication
    val profilePreferences = remember(application) {
        application.getSharedPreferences("rehealth_profile", 0)
    }
    val onboardingComplete = remember(profilePreferences) {
        profilePreferences.getBoolean("onboarding_complete", false)
    }
    var stage by remember {
        mutableStateOf(if (onboardingComplete) AppStage.Main else AppStage.Splash)
    }
    val ringViewModel: RingViewModel = viewModel(
        factory = remember(application) {
            RingViewModel.Factory(
                application.ringRepository,
                application.database.ringDataDao(),
                application.backendClient,
            )
        },
    )
    val ringState by ringViewModel.uiState.collectAsState()
    LaunchedEffect(stage) {
        if (stage == AppStage.Main) {
            ringViewModel.startAutoCollection()
        } else {
            ringViewModel.stopAutoCollection()
        }
    }
    AnimatedContent(
        targetState = stage,
        transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(220)) },
        label = "app-stage",
    ) { current ->
        when (current) {
            AppStage.Splash -> SplashScreen { stage = AppStage.Login }
            AppStage.Login -> LoginScreen(
                onLoginSuccess = {
                    // Return to Main if onboarding is already complete (e.g. after re-login),
                    // otherwise continue the first-run interview/device-setup flow.
                    stage = if (onboardingComplete) AppStage.Main else AppStage.InterviewSession
                },
                onGoToRegister = { stage = AppStage.Register },
            )
            AppStage.Register -> RegisterScreen(
                onBackToLogin = { stage = AppStage.Login },
                onRegistered = {
                    // New users are not onboarded yet, so continue the first-run flow.
                    stage = if (onboardingComplete) AppStage.Main else AppStage.InterviewSession
                },
            )
            AppStage.InterviewSession -> HealthInterviewFlow(
                onBack = { stage = AppStage.Login },
                onComplete = { stage = AppStage.DeviceSetup },
                completionLabel = "连接智能戒指",
            )
            AppStage.DeviceSetup -> DeviceBindingScreen(
                state = ringState,
                onBack = { stage = AppStage.InterviewSession },
                onScan = ringViewModel::scan,
                onConnect = ringViewModel::connect,
                onDisconnect = ringViewModel::disconnect,
                onSync = ringViewModel::syncAll,
                onboarding = true,
                onComplete = {
                    profilePreferences.edit()
                        .putBoolean("device_setup_complete", true)
                        .putBoolean("onboarding_complete", true)
                        .apply()
                    stage = AppStage.Main
                },
            )
            AppStage.Main -> MainShell(
                ringState = ringState,
                ringViewModel = ringViewModel,
                onScan = ringViewModel::scan,
                onConnect = ringViewModel::connect,
                onDisconnect = ringViewModel::disconnect,
                onSync = ringViewModel::syncAll,
                onMeasure = ringViewModel::measure,
                onRestartOnboarding = {
                    profilePreferences.edit().clear().apply()
                    ringViewModel.disconnect()
                    stage = AppStage.Splash
                },
                onGoToLogin = { stage = AppStage.Login },
            )
        }
    }
}

@Composable
private fun SplashScreen(onStart: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3_000)
        onStart()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF8FDFB), Color(0xFFDCF8F2), Color(0xFFBEEFEA)),
                ),
            )
            .statusBarsPadding()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))
            Text("睿禾精灵", color = Color(0xFF08765D), fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("你的端侧 AI 健康伙伴", color = Ink, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(36.dp))
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.xiaohelin),
                contentDescription = "小禾灵",
                modifier = Modifier.size(260.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("健康数据默认保存在本机", color = Ink, fontSize = 13.sp)
            }
            Text("原始健康数据不会自动上传", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(38.dp))
        }
    }
}

@Composable
private fun InterviewScreen(onComplete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Canvas).statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Text("建立你的健康基线", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("我们一起聊聊你的健康", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.xiaohelin),
            contentDescription = null,
            modifier = Modifier.size(180.dp).padding(vertical = 8.dp),
            contentScale = ContentScale.Fit,
        )
        FeatureCard(Icons.Outlined.AutoAwesome, "AI 健康采访", "通过问答了解你，生成专属健康画像")
        FeatureCard(Icons.Outlined.Shield, "端侧模型守护隐私", "数据默认保存在本机，原始数据不上传")
        FeatureCard(Icons.Outlined.FavoriteBorder, "更懂你的健康伙伴", "越了解你，建议越精准，越有温度")
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Mint),
        ) {
            Text("开始健康采访", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("约 5 分钟", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(18.dp))
            .background(Color.White).border(1.dp, Line, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Mint)
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun MainShell(
    ringState: RingUiState,
    ringViewModel: RingViewModel,
    onScan: () -> Unit,
    onConnect: (RingDevice) -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit,
    onMeasure: (RingMetricType) -> Unit,
    onRestartOnboarding: () -> Unit,
    onGoToLogin: () -> Unit,
) {
    val application = LocalContext.current.applicationContext as ReHealthApplication
    var selected by remember { mutableStateOf(Tab.Home) }
    // D3: observe upload queue state + pending feedback for the status banner
    val queueState by application.syncRepository.queueState.collectAsState()
    val pendingFeedback by application.interventionFeedbackRepository.observePendingFeedback()
        .collectAsState(initial = emptyList())
    var showDeviceBinding by remember { mutableStateOf(false) }
    var showInterview by remember { mutableStateOf(false) }
    val canonicalRiskStatus = remember { mutableStateOf<RemoteFeatureEvaluateStatus?>(null) }
    LaunchedEffect(
        ringState.patientMvp?.updatedAt,
        ringState.lastSyncAt,
        ringState.collectedMetricCount,
    ) {
        refreshRemoteFeatureEvaluateStatus(application, ringState, canonicalRiskStatus)
    }
    if (showInterview) {
        HealthInterviewFlow(
            onBack = { showInterview = false },
            onComplete = { showInterview = false },
        )
        return
    }
    if (showDeviceBinding) {
        DeviceBindingScreen(
            state = ringState,
            onBack = { showDeviceBinding = false },
            onScan = onScan,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
            onSync = onSync,
        )
        return
    }
    Scaffold(
        containerColor = Canvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().height(66.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tab.entries.forEach { tab ->
                    Column(
                        modifier = Modifier.weight(1f).clickable { selected = tab },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(tab.icon, tab.label, tint = if (selected == tab) Mint else Muted, modifier = Modifier.size(22.dp))
                        Text(tab.label, color = if (selected == tab) Mint else Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // D3: queue status banner (sync progress / session expired)
                QueueStatusBanner(
                    queueState = queueState,
                    pendingCount = pendingFeedback.size,
                    onLoginClick = onGoToLogin,
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (selected) {
                        Tab.Home -> HomeScreen(onStartInterview = { showInterview = true })
                        Tab.Data -> DataScreen(ringState, ringViewModel, canonicalRiskStatus, onMeasure)
                        Tab.Attribution -> AttributionScreen(
                            ringState = ringState,
                            evaluation = canonicalRiskStatus.value?.toAttributionRiskEvaluation(),
                        )
                        Tab.Model -> ModelScreen(ringState, canonicalRiskStatus)
                        Tab.Profile -> ProfileScreen(
                            state = ringState,
                            onDeviceBinding = { showDeviceBinding = true },
                            onRestartOnboarding = onRestartOnboarding,
                            onGoToLogin = onGoToLogin,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Page(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Canvas).statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(title, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
        }
        item { content() }
    }
}

@Composable
private fun HomeScreen(onStartInterview: () -> Unit) {
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
private fun Metric(label: String, value: String, state: String, modifier: Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(14.dp)).padding(10.dp),
    ) {
        Text(label, color = Muted, fontSize = 10.sp)
        Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        Text(state, color = Mint, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun DataScreen(
    state: RingUiState,
    ringViewModel: RingViewModel,
    canonicalRiskStatus: androidx.compose.runtime.State<RemoteFeatureEvaluateStatus?>,
    onMeasure: (RingMetricType) -> Unit,
) {
    var selectedPeriod by remember { mutableIntStateOf(1) }
    // 真实周期聚合：切换 今日/7天/30天/90天 时从本地 Room 历史重新计算
    var aggregate by remember { mutableStateOf<PeriodAggregate?>(null) }
    LaunchedEffect(selectedPeriod) {
        val windowDays = when (selectedPeriod) { 0 -> 0; 1 -> 7; 2 -> 30; 3 -> 90; else -> 7 }
        aggregate = ringViewModel.loadPeriodAggregate(windowDays)
    }

    fun measurement(type: RingMetricType): String {
        val record = state.measurements[type] ?: return "--"
        return if (type == RingMetricType.BLOOD_PRESSURE) {
            "${record.primaryValue.toInt()}/${record.secondaryValue?.toInt() ?: "--"}"
        } else if (type == RingMetricType.TEMPERATURE) {
            String.format(Locale.getDefault(), "%.1f", record.primaryValue)
        } else {
            record.primaryValue.toInt().toString()
        }
    }
    val periodDays = listOf(0, 7, 30, 90)[selectedPeriod]
    val periodLabel = if (periodDays == 0) "今日" else "近 $periodDays 天"
    val hrText = aggregate?.avgHeartRate?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: measurement(RingMetricType.HEART_RATE)
    val spo2Text = aggregate?.avgSpo2?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: measurement(RingMetricType.BLOOD_OXYGEN)
    val bpText = aggregate?.let { agg ->
        val s = agg.avgSbp?.toInt()
        val d = agg.avgDbp?.toInt()
        if (s != null && d != null) "$s/$d" else null
    } ?: measurement(RingMetricType.BLOOD_PRESSURE)
    val tempText = aggregate?.avgTemp?.let { String.format(Locale.getDefault(), "%.1f", it) } ?: measurement(RingMetricType.TEMPERATURE)
    val sleepValue = aggregate?.avgSleepMinutes?.toInt()?.let { "${it / 60}h${it % 60}m" } ?: run {
        val m = state.sleep?.let { (it.endedAt - it.startedAt) / 60_000 }
        m?.let { "${it / 60}h${it % 60}m" } ?: "--"
    }
    val stepsText = aggregate?.totalSteps?.let { if (it > 0) it.toString() else null } ?: measurement(RingMetricType.STEPS)
    val vitalMetrics = listOf(
        RingMetricUi(RingMetricType.HEART_RATE, "心率", hrText, "bpm", periodLabel, Icons.Outlined.FavoriteBorder, Color(0xFFFF6078), manualMeasure = true),
        RingMetricUi(RingMetricType.BLOOD_OXYGEN, "血氧", spo2Text, "%", periodLabel, Icons.Outlined.DataUsage, Color(0xFF148BFF), manualMeasure = true),
        RingMetricUi(RingMetricType.BLOOD_PRESSURE, "血压", bpText, "mmHg", periodLabel, Icons.Outlined.FavoriteBorder, Color(0xFF8B63F6), manualMeasure = true),
        RingMetricUi(RingMetricType.TEMPERATURE, "体温", tempText, "°C", "定时采集", Icons.Outlined.Assessment, Color(0xFFFF8A32), manualMeasure = true, actionLabel = "开启", measuringLabel = "采集中"),
    )
    val dailyMetrics = listOf(
        RingMetricUi(RingMetricType.SLEEP, "睡眠", sleepValue, "", periodLabel, Icons.Outlined.AutoAwesome, Color(0xFF9668EF)),
        RingMetricUi(RingMetricType.STEPS, "步数", stepsText, "步", periodLabel, Icons.Outlined.ShowChart, Color(0xFF20B77A)),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(
            Brush.radialGradient(
                colors = listOf(Color(0xFFE9FBF6), Canvas),
                radius = 900f,
            ),
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            top = 32.dp,
            end = 16.dp,
            bottom = 12.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("健康数据", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (periodDays == 0) "今日身体状态" else "${periodLabel}身体状态总览",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape)
                        .border(1.dp, Ink, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Assessment, "数据概览", tint = Ink, modifier = Modifier.size(20.dp))
                }
            }
        }
        item {
            PeriodSelector(
                labels = listOf("今日", "7 天", "30 天", "90 天"),
                selected = selectedPeriod,
                onSelected = { selectedPeriod = it },
            )
        }
        item {
            RiskScoreCard(canonicalRiskStatus)
        }
        if (state.message != null || state.isSyncing) {
            item {
                DataStatusCard(state, canonicalRiskStatus)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(178.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HealthScoreCard(Modifier.weight(1f))
                SmartRingOverviewCard(state, Modifier.weight(1f))
            }
        }
        item {
            DashboardSectionHeader(Icons.Outlined.FavoriteBorder, "生命体征")
        }
        item {
            MetricGrid(
                metrics = vitalMetrics,
                measuringMetric = state.measuringMetric,
                onMeasure = onMeasure,
                measureEnabled = !state.isSyncing,
            )
        }
        item {
            DashboardSectionHeader(Icons.Outlined.Timeline, "睡眠与活动")
        }
        item {
            MetricGrid(dailyMetrics)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFF4FFFB), Color(0xFFE3F9F2))))
                    .border(1.dp, Color(0xFFCDEBE2), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text("健康洞察 · AI 提醒", color = Mint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "当前为智能戒指采集数据。血压、血氧等结果仅用于健康管理参考。",
                        color = Muted,
                        fontSize = 9.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = Mint, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PeriodSelector(labels: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
            .background(Color.White.copy(alpha = 0.88f))
            .border(1.dp, Color(0xFFD7E5E1), RoundedCornerShape(17.dp))
            .padding(3.dp),
    ) {
        labels.forEachIndexed { index, label ->
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                    .background(
                        if (index == selected) {
                            Brush.horizontalGradient(listOf(Color(0xFF0E9E8C), Color(0xFF11D7B0)))
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        },
                    )
                    .clickable { onSelected(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (index == selected) Color.White else Muted,
                    fontSize = 13.sp,
                    fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun DataStatusCard(
    state: RingUiState,
    canonicalRiskStatus: androidx.compose.runtime.State<RemoteFeatureEvaluateStatus?>,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .border(1.dp, Color(0xFFD7E5E1), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            state.message ?: if (state.isSyncing) "正在处理戒指数据" else "",
            color = Ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (state.isSyncing) {
            LinearProgressIndicator(
                progress = { state.syncProgress / 100f },
                modifier = Modifier.fillMaxWidth().height(5.dp).padding(top = 7.dp),
                color = Mint,
                trackColor = MintSoft,
            )
        }
        val risk = canonicalRiskStatus.value
        if (risk != null) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(16.dp))
                Text(
                    "规范风险 ${risk.riskLevel.riskLevelLabel()} · ${risk.riskScore.riskScoreLabel()} · ${risk.modeLabel}",
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun RiskScoreCard(
    canonicalRiskStatus: androidx.compose.runtime.State<RemoteFeatureEvaluateStatus?>,
) {
    val current = canonicalRiskStatus.value
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color.White, Color(0xFFEAF8F4))))
            .border(1.dp, Color(0xFFD7E5E1), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("今日风险分", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                current?.summary ?: "正在从本机特征生成云端风险参考",
                color = Muted,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(current?.riskScore.riskScoreLabel(), color = Mint, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(current?.riskLevel.riskLevelLabel(), color = Muted, fontSize = 10.sp)
            Text(current?.modeLabel ?: "评估中", color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun HealthScoreCard(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 11.dp.toPx()
                drawCircle(Color(0xFFD5F1E9), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFF0DA47C), Color(0xFF13D4A7), Color(0xFF0DA47C)),
                    ),
                    startAngle = -90f,
                    sweepAngle = 313f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round),
                )
                drawCircle(
                    color = Color(0xFFBEEBDD),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
                    radius = size.minDimension / 2 - 2.dp.toPx(),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("健康指数", color = Muted, fontSize = 10.sp)
                Text("87", color = Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                Text(
                    "良好",
                    color = Mint,
                    fontSize = 10.sp,
                    modifier = Modifier.clip(CircleShape).background(MintSoft)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        Text("身体状态良好，继续保持 ›", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SmartRingOverviewCard(state: RingUiState, modifier: Modifier) {
    val hasConnectedDevice = state.connectedDevice != null
    val hasLocalData = state.measurements.isNotEmpty() || state.sleep != null
    Column(
        modifier = modifier.fillMaxHeight().clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .border(1.dp, Color(0xFFD6E5E1), RoundedCornerShape(20.dp))
            .padding(11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Text("睿禾智能戒指", color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
            Box(Modifier.size(16.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
        Text(
            when {
                hasConnectedDevice -> "设备已连接"
                hasLocalData -> "有历史数据，需重新连接"
                else -> "设备未连接"
            },
            color = Muted,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.smart_ring),
                contentDescription = "睿禾智能戒指",
                modifier = Modifier.fillMaxWidth().height(67.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                .background(Color.White).border(1.dp, Line, RoundedCornerShape(13.dp))
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Timeline, null, tint = Mint, modifier = Modifier.size(17.dp))
            Text(
                if (state.lastSyncAt == null) "待同步" else "已同步",
                color = Ink,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 7.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                state.lastSyncAt?.let { "上次同步 ${formatSyncTime(it)}" } ?: "上次同步 --:--",
                color = Muted,
                fontSize = 8.sp,
            )
        }
    }
}

@Composable
private fun DashboardSectionHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Mint, modifier = Modifier.size(22.dp))
        }
        Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.dp))
        Spacer(Modifier.weight(1f))
        Text("查看全部  ›", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun MetricGrid(
    metrics: List<RingMetricUi>,
    measuringMetric: RingMetricType? = null,
    onMeasure: (RingMetricType) -> Unit = {},
    measureEnabled: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowMetrics.forEach { metric ->
                    DashboardMetricCard(
                        metric = metric,
                        measuring = measuringMetric == metric.type,
                        measureEnabled = measureEnabled,
                        onMeasure = onMeasure,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
    metric: RingMetricUi,
    measuring: Boolean,
    measureEnabled: Boolean,
    onMeasure: (RingMetricType) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val startMeasure = {
        val toast = if (metric.type == RingMetricType.TEMPERATURE) {
            "已开启体温定时采集，稍后会读取历史体温"
        } else {
            "开始测量${metric.title}，请保持戒指佩戴稳定"
        }
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        onMeasure(metric.type)
    }
    Column(
        modifier = modifier.height(if (metric.manualMeasure) 116.dp else 102.dp).clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.94f))
            .border(1.dp, Color(0xFFE1E9E7), RoundedCornerShape(18.dp))
            .clickable(
                enabled = metric.manualMeasure && measureEnabled && !measuring,
                onClick = startMeasure,
            )
            .padding(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(metric.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(metric.icon, null, tint = metric.color, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(metric.title, color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        metric.value,
                        color = Ink,
                        fontSize = if (metric.type == RingMetricType.BLOOD_PRESSURE) 12.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    if (metric.unit.isNotEmpty()) {
                        Text(
                            metric.unit,
                            color = Muted,
                            fontSize = if (metric.type == RingMetricType.BLOOD_PRESSURE) 5.sp else 7.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                        )
                    }
                }
            }
            if (!metric.manualMeasure) {
                MiniChart(
                    points = if (metric.type == RingMetricType.SLEEP || metric.type == RingMetricType.STEPS) {
                        listOf(.25f, .72f, .38f, .82f, .52f, .75f)
                    } else {
                        listOf(.35f, .65f, .48f, .72f, .28f, .55f)
                    },
                    color = metric.color,
                    modifier = Modifier.width(if (metric.type == RingMetricType.BLOOD_PRESSURE) 16.dp else 24.dp).height(32.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Mint))
            Text(metric.status, color = Muted, fontSize = 8.sp, modifier = Modifier.padding(start = 5.dp))
            if (metric.manualMeasure) {
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(999.dp))
                        .background(if (measuring) Mint.copy(alpha = 0.16f) else MintSoft)
                        .border(1.dp, Mint.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
                        .clickable(enabled = measureEnabled && !measuring, onClick = startMeasure)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (measuring) metric.measuringLabel else metric.actionLabel,
                        color = Mint,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
@Composable
private fun ChartCard(title: String, value: String, color: Color, points: List<Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(110.dp)) {
                Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                Text("平均", color = Muted, fontSize = 10.sp)
            }
            MiniChart(points, color, Modifier.weight(1f).height(62.dp))
        }
    }
}

@Composable
private fun MiniChart(points: List<Float>, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val step = size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = index * step
            val y = size.height * (1f - point)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color, radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun ModelScreen(
    state: RingUiState,
    canonicalRiskStatus: androidx.compose.runtime.State<RemoteFeatureEvaluateStatus?>,
) {
    val inputs = remember(state.measurements, state.sleep, state.activity) { modelInputsFromRingState(state) }
    val current = canonicalRiskStatus.value
    Page("端侧健康模型", "你的健康 AI 正在本机运行") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardBlock {
                RemoteFeatureEvaluateRow(status = canonicalRiskStatus)
            }
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.SmartToy, null, tint = Mint)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("规范风险评估", color = Mint, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "${state.collectedMetricCount} 项戒指数据 · ${current?.modeLabel ?: "评估中"}",
                            color = Muted,
                            fontSize = 11.sp,
                        )
                    }
                    Text(current?.riskScore.riskScoreLabel(), color = Mint, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            CardBlock {
                StatusRow("评估接口", "/rehealth/mobile/features/evaluate")
                StatusRow("算法模式", current?.modeLabel ?: "评估中")
                StatusRow("风险等级", current?.riskLevel.riskLevelLabel())
                StatusRow("风险分数", current?.riskScore.riskScoreLabel())
                StatusRow("模型版本", current?.modelVersion ?: "待返回")
                StatusRow("请求 ID", current?.requestId?.take(12) ?: "待返回")
                Text(
                    current?.summary ?: "正在读取本机特征并请求后端评估。",
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            SectionTitle("主要贡献因素")
            CardBlock {
                val contributions = current?.featureContributions.orEmpty()
                    .toList()
                    .sortedByDescending { kotlin.math.abs(it.second) }
                    .take(4)
                if (contributions.isEmpty()) {
                    Text("后端暂未返回贡献因素。", color = Muted, fontSize = 12.sp)
                } else {
                    contributions.forEachIndexed { index, contribution ->
                        ContributionRow(contribution.first, contribution.second)
                        if (index != contributions.lastIndex) HorizontalDivider(color = Line)
                    }
                }
            }
            SectionTitle("端侧学习流程")
            CardBlock {
                ModelPipelineRow("1", "戒指数据采集", "6 项健康数据", true)
                ModelPipelineRow("2", "本地特征工程", "HealthFeatureExtractor", true)
                ModelPipelineRow("3", "后端风险评估", current?.riskScore?.let { "今日已完成" } ?: "等待结果", current?.riskScore != null)
                ModelPipelineRow("4", "个性化学习", "后续闭环", false)
            }
            SectionTitle("戒指健康数据输入")
            CardBlock {
                inputs.forEachIndexed { index, input ->
                    ModelInputRow(input)
                    if (index != inputs.lastIndex) HorizontalDivider(color = Line)
                }
            }
            SectionTitle("个性化学习状态")
            CardBlock {
                StatusRow("健康基线", "已建立")
                StatusRow("近 7 日有效数据", "86%")
                StatusRow("已参考用户反馈", "18 次")
                StatusRow("最近学习时间", "今天 08:32")
                StatusRow("下次夜间更新", "今晚 23:00")
            }
            SectionTitle("隐私与数据状态")
            CardBlock {
                StatusRow("原始健康数据上传", "否")
                StatusRow("图片原图上传", "否")
                StatusRow("模型服务直连", "否")
                StatusRow("遥测批量上传", "否")
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MintSoft).padding(14.dp),
            ) {
                Icon(Icons.Outlined.Shield, null, tint = Mint, modifier = Modifier.size(18.dp))
                Text(
                    "戒指数据先在本机形成趋势与个人基线；参与云端模型改进前，将单独征得用户授权。",
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text("PHM Core 1.0.0 · 特征引擎 0.1.0 · 风险模型 0.1.0", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ModelPipelineRow(step: String, title: String, status: String, complete: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape)
                .background(if (complete) Mint else Color(0xFFFFF2D8)),
            contentAlignment = Alignment.Center,
        ) {
            Text(step, color = if (complete) Color.White else Color(0xFFD38B18), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(status, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(if (complete) "完成" else "进行中", color = if (complete) Mint else Color(0xFFD38B18), fontSize = 11.sp)
    }
}

@Composable
private fun ModelInputRow(input: ModelInputStatus) {
    val status = when (input.stage) {
        ModelInputStage.READY -> "数据就绪"
        ModelInputStage.FEATURE_EXTRACTED -> "特征已提取"
        ModelInputStage.LEARNING -> "参与学习"
    }
    val color = when (input.stage) {
        ModelInputStage.READY -> Color(0xFF6587FF)
        ModelInputStage.FEATURE_EXTRACTED -> Color(0xFF2DA8A0)
        ModelInputStage.LEARNING -> Mint
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.DataUsage, null, tint = color, modifier = Modifier.size(17.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(input.label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(input.feature, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            status,
            color = color,
            fontSize = 10.sp,
            modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.1f))
                .padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun ContributionRow(field: String, contribution: Double) {
    val direction = if (contribution >= 0.0) "↑" else "↓"
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(field.cvdFieldLabel(), color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            "$direction ${String.format(Locale.getDefault(), "%.3f", kotlin.math.abs(contribution))}",
            color = if (contribution >= 0.0) Color(0xFFE39A22) else Mint,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun String.cvdFieldLabel(): String = when (this) {
    "age" -> "年龄"
    "gender" -> "性别"
    "bmi" -> "BMI"
    "sbp" -> "收缩压"
    "dbp" -> "舒张压"
    "fasting_glucose" -> "空腹血糖"
    "total_cholesterol" -> "总胆固醇"
    "ldl" -> "LDL"
    "hdl" -> "HDL"
    "triglycerides" -> "甘油三酯"
    "exercise_days" -> "运动天数"
    "smoking" -> "吸烟"
    "drinking" -> "饮酒"
    "diabetes_history" -> "糖尿病史"
    "hypertension_history" -> "高血压史"
    "family_history" -> "家族史"
    else -> this
}

private fun modelInputsFromRingState(state: RingUiState): List<ModelInputStatus> =
    listOf(
        ModelInputStatus(
            RingMetricType.HEART_RATE,
            "心率",
            "静息心率、日内波动",
            stageForMeasurement(state, RingMetricType.HEART_RATE),
        ),
        ModelInputStatus(
            RingMetricType.BLOOD_OXYGEN,
            "血氧",
            "均值、低值时长",
            stageForMeasurement(state, RingMetricType.BLOOD_OXYGEN),
        ),
        ModelInputStatus(
            RingMetricType.BLOOD_PRESSURE,
            "血压",
            "收缩压、舒张压",
            stageForMeasurement(state, RingMetricType.BLOOD_PRESSURE),
        ),
        ModelInputStatus(
            RingMetricType.SLEEP,
            "睡眠",
            "时长、阶段、连续性",
            if (state.sleep != null) ModelInputStage.FEATURE_EXTRACTED else ModelInputStage.LEARNING,
        ),
        ModelInputStatus(
            RingMetricType.TEMPERATURE,
            "体温",
            "个人基线偏差",
            stageForMeasurement(state, RingMetricType.TEMPERATURE),
        ),
        ModelInputStatus(
            RingMetricType.STEPS,
            "步数",
            "活动天数、运动频率",
            if (state.activity != null || state.measurements[RingMetricType.STEPS] != null) {
                ModelInputStage.FEATURE_EXTRACTED
            } else {
                ModelInputStage.LEARNING
            },
        ),
    )

private fun stageForMeasurement(state: RingUiState, type: RingMetricType): ModelInputStage =
    if (state.measurements[type] != null) ModelInputStage.FEATURE_EXTRACTED else ModelInputStage.LEARNING

private fun String?.riskLevelLabel(): String = when (this?.lowercase()) {
    "low" -> "低"
    "moderate" -> "中"
    "medium" -> "中"
    "high" -> "高"
    "very_high" -> "很高"
    null -> "待评估"
    else -> this
}

private fun Double?.riskScoreLabel(): String =
    this?.let { "${(it * 100).toInt()}分" } ?: "--"

@Composable
private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = if (value.contains("待")) Color(0xFFE39A22) else Mint, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ProfileScreen(
    state: RingUiState,
    onDeviceBinding: () -> Unit,
    onRestartOnboarding: () -> Unit,
    onGoToLogin: () -> Unit,
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    val profile = state.patientMvp?.profile
    val session = (context.applicationContext as? ReHealthApplication)?.sessionStore
    Page("我的") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.AccountCircle, null, tint = Mint, modifier = Modifier.size(38.dp))
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(profile?.name ?: session?.username ?: "未命名用户", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("${profile?.age ?: "--"}岁 · BMI ${profile?.bmi ?: "--"} · 已陪伴 ${session?.firstUseDays() ?: 0} 天", color = Muted, fontSize = 11.sp)
                    }
                    Text("Pro 会员", color = Color(0xFFB47A13), fontSize = 11.sp, modifier = Modifier.clip(CircleShape).background(Color(0xFFFFF1CD)).padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
            CardBlock {
                Text("健康档案", color = Ink, fontWeight = FontWeight.SemiBold)
                StatusRow("诊断标签", profile?.diagnoses?.joinToString("、") ?: "待补全")
                StatusRow("家族史", if (profile?.familyHistory == true) "有" else "无")
                StatusRow("高血压史", if (profile?.hypertensionHistory == true) "有" else "无")
                StatusRow("糖尿病史", if (profile?.diabetesHistory == true) "有" else "无")
                StatusRow("最近更新", profile?.updatedAt?.let { formatSyncTime(it) } ?: "待同步")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("睡眠时长", formatSleepMinutes(state.sleep), "昨夜", Modifier.weight(1f))
                Metric("每日步数", formatSteps(state.measurements[RingMetricType.STEPS]?.primaryValue), "步", Modifier.weight(1f))
                Metric("体重", profile?.weightKg?.let { "%.1f".format(it) } ?: "--", "kg", Modifier.weight(1f))
            }
            CardBlock {
                MenuRow(Icons.Outlined.Devices, "设备绑定", onDeviceBinding)
                MenuRow(Icons.Outlined.Lock, "隐私中心")
                MenuRow(Icons.Outlined.Download, "数据导出")
                MenuRow(Icons.Outlined.DeleteOutline, "数据删除")
                MenuRow(Icons.Outlined.NotificationsNone, "通知设置")
                MenuRow(Icons.Outlined.Settings, "关于睿禾精灵")
                MenuRow(Icons.Outlined.Timeline, "重新体验首次使用流程", onRestartOnboarding)
                MenuRow(Icons.Outlined.ExitToApp, "退出登录") { showLogoutDialog = true }
            }
        }
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("确认退出登录") },
            text = { Text("退出后需要重新登录才能同步反馈数据") },
            confirmButton = {
                TextButton(
                    onClick = {
                        performLogout(context)
                        showLogoutDialog = false
                        onGoToLogin()
                    },
                ) { Text("确认退出") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("取消") }
            },
        )
    }
}

/** D3 logout: cancel the sync worker, clear the auth session, and pause the upload queue. */
private fun performLogout(context: Context) {
    val app = context.applicationContext as ReHealthApplication
    MeasurementSyncWorker.cancel(context)
    app.authenticatedApiClient.onLogout()
    app.syncRepository.pauseQueue()
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Ink, modifier = Modifier.size(19.dp))
        Text(label, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Icon(Icons.Outlined.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
    HorizontalDivider(color = Line)
}

@Composable
private fun DeviceBindingScreen(
    state: RingUiState,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onConnect: (RingDevice) -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit,
    onboarding: Boolean = false,
    onComplete: (() -> Unit)? = null,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(hasBluetoothPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        permissionGranted = results.values.all { it }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Canvas).statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, "返回", tint = Ink)
                }
                Column {
                    Text(if (onboarding) "连接你的智能戒指" else "设备绑定", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (onboarding) "连接后即可进入主页" else "连接睿禾智能戒指并同步健康数据",
                        color = Muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        item {
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Bluetooth, null, tint = Mint)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("蓝牙权限", color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (permissionGranted) "已授权，可连接真实戒指" else "真实戒指到货后需要授权",
                            color = Muted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    Button(
                        onClick = {
                            permissionLauncher.launch(requiredBluetoothPermissions())
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (permissionGranted) MintSoft else Mint),
                    ) {
                        Text(if (permissionGranted) "已授权" else "授权", color = if (permissionGranted) Mint else Color.White)
                    }
                }
                Text(
                    "已切换为真实戒指链路，数据默认保存在本机。",
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
        item {
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("设备状态", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(connectionLabel(state.connectionState), color = Mint, fontSize = 12.sp)
                }
                Text(
                    state.connectedDevice?.name ?: "尚未连接设备",
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp),
                )
                state.connectedDevice?.let {
                    Text("${it.address} · RSSI ${it.rssi ?: "--"}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                if (state.message != null) {
                    Text(state.message, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
                if (state.isSyncing) {
                    LinearProgressIndicator(
                        progress = { state.syncProgress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        color = Mint,
                        trackColor = MintSoft,
                    )
                    Text("${state.syncProgress}%", color = Mint, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onScan,
                        enabled = !state.isScanning && !state.isSyncing,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MintSoft, contentColor = Mint),
                    ) {
                        Text(if (state.isScanning) "搜索中" else "搜索智能戒指")
                    }
                    if (state.connectedDevice != null) {
                        Button(
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F4F3), contentColor = Ink),
                        ) {
                            Text("断开")
                        }
                    }
                }
            }
        }
        item {
            CardBlock {
                Text("数据采集目标", color = Ink, fontWeight = FontWeight.SemiBold)
                Text(
                    "健康数据：睡眠 · 血压 · 体温 · 心率 · 步数 · 血氧\n设备功能：遥控拍照 · 女性健康",
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeaturePill(Icons.Outlined.AddAPhoto, "遥控拍照", Modifier.weight(1f))
                    FeaturePill(Icons.Outlined.FavoriteBorder, "女性健康", Modifier.weight(1f))
                }
                Text(
                    "本机已保存 ${state.collectedMetricCount} / 6 项健康数据",
                    color = Mint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Button(
                    onClick = onSync,
                    enabled = !state.isSyncing,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                ) {
                    Text(if (state.isSyncing) "正在同步 ${state.syncProgress}%" else "同步全部健康数据")
                }
                state.lastSyncAt?.let {
                    Text(
                        "最近同步：${formatSyncTime(it)}",
                        color = Muted,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
        items(state.devices) { device ->
            CardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Devices, null, tint = Mint, modifier = Modifier.size(28.dp))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(device.name ?: "智能戒指", color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("${device.address} · ${device.rssi ?: "--"} dBm", color = Muted, fontSize = 10.sp)
                    }
                    Button(
                        onClick = { onConnect(device) },
                        enabled = state.connectedDevice?.address != device.address,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint),
                    ) {
                        Text(if (state.connectedDevice?.address == device.address) "已连接" else "连接")
                    }
                }
            }
        }
        if (onboarding) {
            item {
                Button(
                    onClick = { onComplete?.invoke() },
                    enabled = state.connectedDevice != null,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                ) {
                    Text(
                        if (state.connectedDevice == null) "请先连接戒指" else "完成设置，进入主页",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    "连接成功后，我们会同步戒指数据并进入主页。",
                    color = Muted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                )
                // 暂时没有戒指：跳过配对，直接进入主页（仍会标记首次引导已完成）
                OutlinedButton(
                    onClick = { onComplete?.invoke() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Mint),
                    border = BorderStroke(1.dp, Mint),
                ) {
                    Text("暂时没有戒指，先跳过", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(icon: ImageVector, label: String, modifier: Modifier) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(MintSoft)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Mint, modifier = Modifier.size(18.dp))
        Text(
            label,
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

private fun requiredBluetoothPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

private fun hasBluetoothPermission(context: Context): Boolean {
    return requiredBluetoothPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

private fun connectionLabel(state: RingConnectionState): String = when (state) {
    RingConnectionState.SCANNING -> "搜索中"
    RingConnectionState.CONNECTING -> "连接中"
    RingConnectionState.CONNECTED -> "已连接"
    RingConnectionState.SYNCING -> "同步中"
    RingConnectionState.ERROR -> "异常"
    RingConnectionState.UNSUPPORTED -> "不支持蓝牙"
    RingConnectionState.PERMISSION_REQUIRED -> "需要权限"
    RingConnectionState.BLUETOOTH_OFF -> "蓝牙未开启"
    RingConnectionState.DISCONNECTED -> "未连接"
}

private fun formatSyncTime(timestamp: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

/** Real sleep duration from the latest sleep session (deep+light+rem+awake minutes). Computed, never canned. */
private fun formatSleepMinutes(entity: RingSleepSessionEntity?): String {
    if (entity == null) return "--"
    val total = entity.deepMinutes + entity.lightMinutes + entity.remMinutes + entity.awakeMinutes
    if (total <= 0) return "--"
    return "${total / 60}h${total % 60}m"
}

/** Real step total from today's STEPS measurement, formatted with thousands separator. */
private fun formatSteps(value: Double?): String =
    if (value == null) "--" else String.format("%,d", value.roundToInt())

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

/**
 * Minimal remote feature-evaluate UI status. Holds only display strings, not raw health data.
 * Built to keep business logic outside the composable: parsing happens in
 * [refreshRemoteFeatureEvaluateStatus]; the composable only renders [summary].
 */
private data class RemoteFeatureEvaluateStatus(
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

private fun RemoteFeatureEvaluateStatus.toAttributionRiskEvaluation(): AttributionRiskEvaluation =
    AttributionRiskEvaluation(
        riskScore = riskScore,
        riskLevel = riskLevel,
        contributions = featureContributions,
        confirmed = reachable && isMock == false && !usedMockFallback,
    )

private suspend fun refreshRemoteFeatureEvaluateStatus(
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
private fun RemoteFeatureEvaluateRow(status: androidx.compose.runtime.State<RemoteFeatureEvaluateStatus?>) {
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
