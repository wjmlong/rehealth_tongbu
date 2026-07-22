package com.rehealth.genie.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ring.RingViewModel
import com.rehealth.genie.ui.theme.Canvas
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.Muted
import com.rehealth.genie.ui.components.QueueStatusBanner

private enum class AppStage { Splash, Login, Register, InterviewSession, DeviceSetup, Main }
private enum class Tab(val label: String, val icon: ImageVector) {
    Home("首页", Icons.Outlined.Home),
    Data("数据", Icons.Outlined.ShowChart),
    Attribution("归因", Icons.Outlined.Assessment),
    Model("模型", Icons.Outlined.SmartToy),
    Profile("我的", Icons.Outlined.PersonOutline),
}

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
