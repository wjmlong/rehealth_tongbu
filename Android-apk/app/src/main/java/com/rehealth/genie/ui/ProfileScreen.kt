package com.rehealth.genie.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.data.profileAvatarStorageKey
import com.rehealth.genie.network.PatientProfilePayload
import com.rehealth.genie.network.dto.HealthInterviewSubmitRequestDto
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

internal fun healthArchiveRows(
    profile: PatientProfilePayload?,
    interview: HealthInterviewSubmitRequestDto?,
): List<Pair<String, String>> = buildList {
    val diagnoses = profile?.diagnoses.orEmpty()
        .takeIf { it.isNotEmpty() }
        ?.joinToString("、")
        .orEmpty()
        .ifBlank { "待补全" }
    add("诊断标签" to diagnoses)
    add("家族史" to profile?.familyHistory.toArchiveBoolean())
    add("高血压史" to profile?.hypertensionHistory.toArchiveBoolean())
    add("糖尿病史" to profile?.diabetesHistory.toArchiveBoolean())
    interview?.baselineItems.orEmpty()
        .filterNot { it.label == "基本资料" }
        .forEach { item -> add(item.label to item.value) }
    add(
        "关注方向" to interview?.focusAreas.orEmpty()
            .takeIf { it.isNotEmpty() }
            ?.joinToString("、")
            .orEmpty()
            .ifBlank { "暂无健康问答记录" },
    )
}

private fun Boolean?.toArchiveBoolean(): String = when (this) {
    true -> "有"
    false -> "无"
    null -> "待补全"
}

@Composable
internal fun ProfileScreen(
    state: RingUiState,
    onDeviceBinding: () -> Unit,
    onRestartOnboarding: () -> Unit,
    onGoToLogin: () -> Unit,
    onStartInterview: () -> Unit = {},
    onProfileUpdated: () -> Unit = {},
    onRefreshProfile: () -> Unit = {},
    onHealthMetricsUpdated: () -> Unit = {},
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showRhiInputDialog by remember { mutableStateOf(false) }
    var showScanLinkDialog by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    val profile = AttributionDataProvenance.trustedProfile(state.patientMvp)
    val latestInterview = state.patientMvp?.latestHealthInterview
    val session = (context.applicationContext as? ReHealthApplication)?.sessionStore
    val avatarIdentity = session?.userId ?: session?.username ?: "signed-out"
    val avatarStorageKey = remember(avatarIdentity) { profileAvatarStorageKey(avatarIdentity) }
    val avatarViewModel: ProfileAvatarViewModel = viewModel(
        key = "profile-avatar-$avatarStorageKey",
        factory = remember(context, avatarIdentity) {
            ProfileAvatarViewModel.Factory(context.applicationContext, avatarIdentity)
        },
    )
    val avatarState by avatarViewModel.uiState.collectAsState()
    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let(avatarViewModel::save)
    }
    val editViewModel: ProfileEditViewModel = viewModel(factory = ProfileEditViewModel.Factory(context))
    val editState by editViewModel.uiState.collectAsState()
    val rhiInputViewModel: RhiManualInputViewModel = viewModel(
        factory = RhiManualInputViewModel.Factory(context),
    )
    val rhiInputState by rhiInputViewModel.uiState.collectAsState()
    val serviceContactViewModel: ServiceContactViewModel = viewModel(
        factory = ServiceContactViewModel.Factory(context),
    )
    val serviceContactState by serviceContactViewModel.uiState.collectAsState()
    val scanLinkViewModel: ScanLinkViewModel = viewModel(
        factory = ScanLinkViewModel.Factory(context),
    )
    val scanLinkState by scanLinkViewModel.uiState.collectAsState()
    val planBindingViewModel: InsurancePlanBindingViewModel = viewModel(
        factory = InsurancePlanBindingViewModel.Factory(context),
    )
    val planBindingState by planBindingViewModel.uiState.collectAsState()
    // 机构侧可能随时变更（取消保单关联/换专员等），页面每次回到前台强制刷新保险数据。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                serviceContactViewModel.loadForCurrentUser(force = true)
                planBindingViewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        onRefreshProfile()
        rhiInputViewModel.observeCurrentUser()
        serviceContactViewModel.loadForCurrentUser()
        planBindingViewModel.loadForCurrentUser()
    }
    LaunchedEffect(editState.saved) {
        if (editState.saved) {
            showEditDialog = false
            editViewModel.reset()
            onProfileUpdated()
        }
    }
    LaunchedEffect(rhiInputState.savedVersion) {
        if (rhiInputState.savedVersion > 0) {
            showRhiInputDialog = false
            onHealthMetricsUpdated()
        }
    }
    Page("我的") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ReHealthCardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(58.dp).clip(CircleShape).background(MintSoft).clickable(
                            enabled = !avatarState.isSaving,
                        ) {
                            avatarPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        val avatar = avatarState.bitmap
                        if (avatar == null) {
                            Icon(Icons.Outlined.AccountCircle, "选择本机头像", tint = Mint, modifier = Modifier.size(38.dp))
                        } else {
                            Image(
                                bitmap = avatar.asImageBitmap(),
                                contentDescription = "本机头像",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(58.dp),
                            )
                        }
                        if (avatarState.isSaving) {
                            Box(
                                Modifier.size(58.dp).background(Color.Black.copy(alpha = 0.28f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(profile?.name ?: session?.username ?: "未命名用户", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("${profile?.age ?: "--"}岁 · BMI ${profile?.bmi ?: "--"} · 已陪伴 ${session?.firstUseDays() ?: 0} 天", color = Muted, fontSize = 11.sp)
                        Text(
                            "点击头像从系统相册选择 · 仅保存在本机",
                            color = Muted,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                    Text("Pro 会员", color = Color(0xFFB47A13), fontSize = 11.sp, modifier = Modifier.clip(CircleShape).background(Color(0xFFFFF1CD)).padding(horizontal = 10.dp, vertical = 5.dp))
                }
                avatarState.errorMessage?.let { error ->
                    Text(
                        error,
                        color = Color(0xFFD94C4C),
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth().clickable(onClick = avatarViewModel::clearError)
                            .padding(top = 8.dp),
                    )
                }
            }
            ReHealthCardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("我的服务专员", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (serviceContactState.loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Mint, strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = { serviceContactViewModel.loadForCurrentUser(force = true) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Outlined.Refresh, "刷新服务专员", tint = Muted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                val contact = serviceContactState.contact
                if (contact == null) {
                    Text(
                        if (serviceContactState.loading) "正在获取服务关系…" else "暂无专属服务专员",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    StatusRow("服务专员", contact.employeeName ?: "未命名员工")
                    StatusRow("所属项目", contact.projectName ?: "默认服务项目")
                    StatusRow(
                        "服务角色",
                        when (contact.roleType) {
                            "PRIMARY" -> "主负责人"
                            "BACKUP" -> "协作者"
                            "TEMPORARY" -> "临时代理"
                            "SUPERVISOR" -> "主管"
                            else -> contact.roleType ?: "—"
                        },
                    )
                    contact.startTime?.let { StatusRow("开始服务", it.take(10)) }
                }
                Text(
                    "服务关系由保险机构管理，如需变更负责人请联系您的保险服务人员",
                    color = Muted,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    if (contact == null) "扫码关联服务专员" else "扫码更换服务专员",
                    color = Mint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .clickable { showScanLinkDialog = true },
                )
            }
            ReHealthCardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("保险计划", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (planBindingState.loading || planBindingState.binding) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Mint, strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick = { planBindingViewModel.refresh() },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(Icons.Outlined.Refresh, "刷新保险计划", tint = Muted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                if (planBindingState.bindings.isNotEmpty()) {
                    Text("已加入的机构计划", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                    planBindingState.bindings.forEach { binding ->
                        Column(
                            Modifier.fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MintSoft.copy(alpha = 0.35f))
                                .border(1.dp, Line, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                        ) {
                            Text(
                                binding.planName ?: binding.planId.takeUnless { it == "NONE" } ?: "健康管理授权",
                                color = Ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "保单 ${binding.policyNo.takeIf { it.isNotBlank() } ?: "—"}",
                                color = Muted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                            Text(
                                if (binding.status == "ACTIVE") "生效中" else binding.status,
                                color = Mint,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
                val boundPolicyNos = planBindingState.bindings.map { it.policyNo }.toSet()
                val unboundCandidates = planBindingState.candidates.filterNot { it.policyNo in boundPolicyNos }
                if (unboundCandidates.isEmpty() && planBindingState.bindings.isEmpty() && !planBindingState.loading) {
                    Text(
                        planBindingState.message ?: "未发现可绑定的保单",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else if (unboundCandidates.isNotEmpty()) {
                    Text(
                        if (planBindingState.bindings.isNotEmpty()) "还可加入的机构保单" else "检测到可绑定的保单",
                        color = Muted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    unboundCandidates.forEach { candidate ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MintSoft.copy(alpha = 0.35f))
                                .border(1.dp, Line, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    candidate.planName ?: candidate.productName ?: "健康保险",
                                    color = Ink,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "保单 ${candidate.policyNoMasked}" +
                                        (if (candidate.hasPlan) " · 含健康计划" else " · 仅授权，暂无健康计划"),
                                    color = Muted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                            Button(
                                onClick = { planBindingViewModel.bindSelected(candidate) },
                                enabled = planBindingState.agreed && !planBindingState.binding,
                            ) {
                                Text("加入", fontSize = 12.sp)
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Checkbox(
                            checked = planBindingState.agreed,
                            onCheckedChange = planBindingViewModel::setAgreed,
                        )
                        Text("我已阅读并同意《健康数据授权协议》", color = Muted, fontSize = 11.sp)
                    }
                }
                planBindingState.message?.takeIf { planBindingState.bindings.isNotEmpty() }?.let { message ->
                    Text(message, color = Mint, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            ReHealthCardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("健康档案", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (state.isPatientMvpLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Mint, strokeWidth = 2.dp)
                    }
                    TextButton(
                        onClick = {
                            rhiInputViewModel.clearError()
                            showRhiInputDialog = true
                        },
                    ) {
                        Text("编辑指标", color = Mint, fontSize = 12.sp)
                    }
                }
                healthArchiveRows(profile, latestInterview).forEach { row ->
                    StatusRow(row.first, row.second)
                }
                StatusRow(
                    "日均久坐",
                    rhiInputState.input?.sedentaryHoursPerDay?.let { "%.1f 小时".format(it) } ?: "待补全",
                )
                StatusRow(
                    "腰围",
                    rhiInputState.input?.waistCircumferenceCm?.let { "%.1f cm".format(it) } ?: "待补全",
                )
                StatusRow(
                    "最大摄氧量",
                    rhiInputState.input?.vo2MaxMlKgMin?.let { "%.1f mL/kg/min".format(it) } ?: "待补全",
                )
                StatusRow(
                    "糖化血红蛋白",
                    rhiInputState.input?.hba1cPercent?.let { "%.1f%%".format(it) } ?: "待补全",
                )
                StatusRow(
                    "估算肾小球滤过率",
                    rhiInputState.input?.egfrMlMin173m2?.let { "%.1f mL/min/1.73m²".format(it) } ?: "待补全",
                )
                StatusRow(
                    "上臂袖带血压",
                    rhiInputState.input?.takeIf { it.cuffConfirmed }
                        ?.let { "${it.cuffSbp7dMean ?: "--"}/${it.cuffDbp7dMean ?: "--"} mmHg · ${it.cuffValidDays ?: 0}天" }
                        ?: "待核对",
                )
                StatusRow(
                    "医院血检",
                    rhiInputState.input?.takeIf { it.labConfirmed }?.let { "已核对报告" } ?: "待核对",
                )
                val updatedAt = listOfNotNull(profile?.updatedAt, latestInterview?.generatedAt).maxOrNull()
                StatusRow("最近更新", updatedAt?.let { formatSyncTime(it) } ?: "待同步")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric("睡眠时长", formatSleepMinutes(state.sleep), "昨夜", Modifier.weight(1f))
                Metric(
                    "每日步数",
                    formatSteps(
                        state.todayActivitySteps?.toDouble()
                            ?: state.measurements[RingMetricType.STEPS]?.primaryValue,
                    ),
                    "步",
                    Modifier.weight(1f),
                )
                Metric("体重", profile?.weightKg?.let { "%.1f".format(it) } ?: "--", "kg", Modifier.weight(1f))
            }
            ReHealthCardBlock {
                MenuRow(Icons.Outlined.EditNote, "编辑个人资料") {
                    editViewModel.reset()
                    showEditDialog = true
                }
                MenuRow(Icons.Outlined.EditNote, "编辑健康与归因指标") {
                    rhiInputViewModel.clearError()
                    showRhiInputDialog = true
                }
                MenuRow(Icons.Outlined.QuestionAnswer, "更新健康问答", onStartInterview)
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
    if (showEditDialog) {
        ProfileEditDialog(
            initialName = profile?.name ?: session?.username.orEmpty(),
            initialGender = profile?.gender,
            initialAge = profile?.age?.toString().orEmpty(),
            initialHeight = profile?.heightCm?.toString().orEmpty(),
            initialWeight = profile?.weightKg?.toString().orEmpty(),
            initialSmoking = profile?.smoking,
            initialDrinking = profile?.drinking,
            initialDiabetesHistory = profile?.diabetesHistory,
            initialHypertensionHistory = profile?.hypertensionHistory,
            initialFamilyHistory = profile?.familyHistory,
            isSaving = editState.isSaving,
            errorMessage = editState.errorMessage,
            onSave = { name, gender, age, height, weight, smoking, drinking, diabetes, hypertension, family ->
                editViewModel.save(
                    name,
                    gender,
                    age,
                    height,
                    weight,
                    smoking,
                    drinking,
                    diabetes,
                    hypertension,
                    family,
                )
            },
            onDismiss = {
                if (!editState.isSaving) {
                    showEditDialog = false
                    editViewModel.reset()
                }
            },
        )
    }
    if (showRhiInputDialog) {
        RhiManualInputDialog(
            current = rhiInputState.input,
            isSaving = rhiInputState.isSaving,
            errorMessage = rhiInputState.errorMessage,
            onSave = rhiInputViewModel::save,
            onDismiss = {
                if (!rhiInputState.isSaving) {
                    showRhiInputDialog = false
                    rhiInputViewModel.clearError()
                }
            },
        )
    }
    if (showScanLinkDialog) {
        ScanLinkDialog(
            state = scanLinkState,
            onCodeChange = scanLinkViewModel::updateCode,
            onScan = scanLinkViewModel::scan,
            onConfirm = scanLinkViewModel::confirm,
            onBack = scanLinkViewModel::backToInput,
            onOpenCamera = { showScanner = true },
            onDismiss = {
                showScanLinkDialog = false
                scanLinkViewModel.backToInput()
                serviceContactViewModel.loadForCurrentUser(force = true)
            },
        )
    }
    if (showScanner) {
        ScanCameraScreen(
            onCodeFound = { code ->
                showScanner = false
                scanLinkViewModel.updateCode(code)
                scanLinkViewModel.scan()
            },
            onClose = { showScanner = false },
            onDenied = { showScanner = false },
        )
    }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("确认退出登录") },
            text = { Text("退出后需要重新登录才能同步反馈数据") },
            confirmButton = {
                TextButton(
                    onClick = {
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

@Composable
private fun RhiManualInputDialog(
    current: com.rehealth.genie.rhi.RhiManualHealthInputEntity?,
    isSaving: Boolean,
    errorMessage: String?,
    onSave: (RhiManualInputDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    var sedentary by remember(current) { mutableStateOf(current?.sedentaryHoursPerDay?.toString().orEmpty()) }
    var waist by remember(current) { mutableStateOf(current?.waistCircumferenceCm?.toString().orEmpty()) }
    var vo2Max by remember(current) { mutableStateOf(current?.vo2MaxMlKgMin?.toString().orEmpty()) }
    var hba1c by remember(current) { mutableStateOf(current?.hba1cPercent?.toString().orEmpty()) }
    var egfr by remember(current) { mutableStateOf(current?.egfrMlMin173m2?.toString().orEmpty()) }
    var cuffSbp by remember(current) { mutableStateOf(current?.cuffSbp7dMean?.toString().orEmpty()) }
    var cuffDbp by remember(current) { mutableStateOf(current?.cuffDbp7dMean?.toString().orEmpty()) }
    var cuffDays by remember(current) { mutableStateOf(current?.cuffValidDays?.toString().orEmpty()) }
    var cuffConfirmed by remember(current) { mutableStateOf(current?.cuffConfirmed == true) }
    var fastingGlucose by remember(current) { mutableStateOf(current?.fastingGlucoseMmolL?.toString().orEmpty()) }
    var totalCholesterol by remember(current) { mutableStateOf(current?.totalCholesterolMmolL?.toString().orEmpty()) }
    var ldl by remember(current) { mutableStateOf(current?.ldlMmolL?.toString().orEmpty()) }
    var hdl by remember(current) { mutableStateOf(current?.hdlMmolL?.toString().orEmpty()) }
    var triglycerides by remember(current) { mutableStateOf(current?.triglyceridesMmolL?.toString().orEmpty()) }
    var labConfirmed by remember(current) { mutableStateOf(current?.labConfirmed == true) }
    var labDate by remember(current) {
        mutableStateOf(
            current?.labRecordedAt?.let {
                java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
            }.orEmpty(),
        )
    }
    val numericOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        titleContentColor = Ink,
        textContentColor = Ink,
        tonalElevation = 0.dp,
        title = { Text("健康与归因指标", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    "请填写真实测量值。留空表示缺失并降低数据可信度，不会自动填入正常值。",
                    color = Muted,
                    fontSize = 11.sp,
                )
                RhiManualField(sedentary, { sedentary = it }, "日均久坐（小时）", "范围 0–24", numericOptions)
                RhiManualField(waist, { waist = it }, "腰围（cm）", "范围 40–200", numericOptions)
                RhiManualField(vo2Max, { vo2Max = it }, "最大摄氧量（mL/kg/min）", "范围 5–100", numericOptions)
                RhiManualField(hba1c, { hba1c = it }, "糖化血红蛋白（%）", "范围 3–20", numericOptions)
                RhiManualField(egfr, { egfr = it }, "估算肾小球滤过率（mL/min/1.73m²）", "范围 0–250", numericOptions)
                Text("归因 16 项 · 上臂袖带血压", color = Ink, fontWeight = FontWeight.SemiBold)
                RhiManualField(cuffSbp, { cuffSbp = it }, "7日平均收缩压（mmHg）", "范围 70–250", numericOptions)
                RhiManualField(cuffDbp, { cuffDbp = it }, "7日平均舒张压（mmHg）", "范围 40–150", numericOptions)
                RhiManualField(cuffDays, { cuffDays = it }, "有效测量天数", "至少 3 天，建议 7 天", numericOptions)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = cuffConfirmed, onCheckedChange = { cuffConfirmed = it })
                    Text("确认数据来自经验证的上臂袖带血压计", color = Muted, fontSize = 11.sp)
                }
                Text("归因 16 项 · 医院血检", color = Ink, fontWeight = FontWeight.SemiBold)
                RhiManualField(fastingGlucose, { fastingGlucose = it }, "空腹血糖（mmol/L）", "填写医院报告实测值", numericOptions)
                RhiManualField(totalCholesterol, { totalCholesterol = it }, "总胆固醇（mmol/L）", "填写医院报告实测值", numericOptions)
                RhiManualField(ldl, { ldl = it }, "LDL-C（mmol/L）", "填写医院报告实测值", numericOptions)
                RhiManualField(hdl, { hdl = it }, "HDL-C（mmol/L）", "填写医院报告实测值", numericOptions)
                RhiManualField(triglycerides, { triglycerides = it }, "甘油三酯（mmol/L）", "填写医院报告实测值", numericOptions)
                OutlinedTextField(
                    value = labDate,
                    onValueChange = { labDate = it.filter { ch -> ch.isDigit() || ch == '-' }.take(10) },
                    label = { Text("血检报告日期") },
                    supportingText = { Text("格式 YYYY-MM-DD，用于 90/180/365 天新鲜度") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = reHealthTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = labConfirmed, onCheckedChange = { labConfirmed = it })
                    Text("确认已逐项核对医院报告原件", color = Muted, fontSize = 11.sp)
                }
                errorMessage?.let { Text(it, color = Color(0xFFD94C4C), fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                shape = RoundedCornerShape(14.dp),
                onClick = {
                    onSave(
                        RhiManualInputDraft(
                            sedentaryHoursPerDay = sedentary,
                            waistCircumferenceCm = waist,
                            vo2MaxMlKgMin = vo2Max,
                            hba1cPercent = hba1c,
                            egfrMlMin173m2 = egfr,
                            cuffSbp7dMean = cuffSbp,
                            cuffDbp7dMean = cuffDbp,
                            cuffValidDays = cuffDays,
                            cuffConfirmed = cuffConfirmed,
                            fastingGlucoseMmolL = fastingGlucose,
                            totalCholesterolMmolL = totalCholesterol,
                            ldlMmolL = ldl,
                            hdlMmolL = hdl,
                            triglyceridesMmolL = triglycerides,
                            labConfirmed = labConfirmed,
                            labRecordedDate = labDate,
                        ),
                    )
                },
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text("保存")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("取消") } },
    )
}

@Composable
private fun RhiManualField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String,
    keyboardOptions: KeyboardOptions,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { updated ->
            onValueChange(updated.filter { it.isDigit() || it == '.' }.take(8))
        },
        label = { Text(label) },
        supportingText = { Text(supportingText) },
        keyboardOptions = keyboardOptions,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = reHealthTextFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun reHealthTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Mint,
    unfocusedBorderColor = Line,
    focusedLabelColor = Mint,
    unfocusedLabelColor = Muted,
    cursorColor = Mint,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
)

@Composable
private fun ProfileEditDialog(
    initialName: String,
    initialGender: String?,
    initialAge: String,
    initialHeight: String,
    initialWeight: String,
    initialSmoking: Boolean?,
    initialDrinking: Boolean?,
    initialDiabetesHistory: Boolean?,
    initialHypertensionHistory: Boolean?,
    initialFamilyHistory: Boolean?,
    isSaving: Boolean,
    errorMessage: String?,
    onSave: (
        name: String,
        gender: String,
        age: String,
        height: String,
        weight: String,
        smoking: Boolean?,
        drinking: Boolean?,
        diabetesHistory: Boolean?,
        hypertensionHistory: Boolean?,
        familyHistory: Boolean?,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var gender by remember { mutableStateOf(normalizeProfileGender(initialGender)) }
    var age by remember { mutableStateOf(initialAge) }
    var height by remember { mutableStateOf(initialHeight) }
    var weight by remember { mutableStateOf(initialWeight) }
    var smoking by remember { mutableStateOf(initialSmoking) }
    var drinking by remember { mutableStateOf(initialDrinking) }
    var diabetesHistory by remember { mutableStateOf(initialDiabetesHistory) }
    var hypertensionHistory by remember { mutableStateOf(initialHypertensionHistory) }
    var familyHistory by remember { mutableStateOf(initialFamilyHistory) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        titleContentColor = Ink,
        textContentColor = Ink,
        tonalElevation = 0.dp,
        title = { Text("编辑个人资料", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(32) },
                    label = { Text("姓名 / 昵称") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = reHealthTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("归因 16 项档案", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                BooleanFactorSelector("当前吸烟", smoking) { smoking = it }
                BooleanFactorSelector("当前饮酒", drinking) { drinking = it }
                BooleanFactorSelector("糖尿病史", diabetesHistory) { diabetesHistory = it }
                BooleanFactorSelector("高血压史", hypertensionHistory) { hypertensionHistory = it }
                BooleanFactorSelector("早发心血管家族史", familyHistory) { familyHistory = it }
                Text("性别", color = Ink, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = gender == "male",
                        onClick = { gender = "male" },
                        label = { Text("男") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = gender == "female",
                        onClick = { gender = "female" },
                        label = { Text("女") },
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it.filter { c -> c.isDigit() }.take(3) },
                    label = { Text("年龄") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = reHealthTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("身高 (cm)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = reHealthTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("体重 (kg)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = reHealthTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                errorMessage?.let {
                    Text(it, color = Color(0xFFD94C4C), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                shape = RoundedCornerShape(14.dp),
                onClick = {
                    gender?.let {
                        onSave(
                            name,
                            it,
                            age,
                            height,
                            weight,
                            smoking,
                            drinking,
                            diabetesHistory,
                            hypertensionHistory,
                            familyHistory,
                        )
                    }
                },
                enabled = !isSaving && name.isNotBlank() && gender != null,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("取消") }
        },
    )
}

@Composable
private fun BooleanFactorSelector(
    label: String,
    value: Boolean?,
    onValueChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
        FilterChip(
            selected = value == false,
            onClick = { onValueChange(false) },
            label = { Text("否") },
        )
        FilterChip(
            selected = value == true,
            onClick = { onValueChange(true) },
            label = { Text("是") },
            modifier = Modifier.padding(start = 6.dp),
        )
    }
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

/** 扫码关联弹窗：相机扫码 / 手动输码 → 预览员工 → 确认建立/更换服务关系。 */
@Composable
private fun ScanLinkDialog(
    state: ScanLinkUiState,
    onCodeChange: (String) -> Unit,
    onScan: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        titleContentColor = Ink,
        textContentColor = Ink,
        title = { Text("扫码关联服务专员", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state.phase) {
                    ScanLinkUiState.Phase.INPUT -> {
                        Text(
                            "扫描服务人员展示的二维码，或输入 8 位员工码",
                            color = Muted,
                            fontSize = 12.sp,
                        )
                        OutlinedTextField(
                            value = state.codeInput,
                            onValueChange = onCodeChange,
                            label = { Text("员工码") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = reHealthTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "打开相机扫码",
                            color = Mint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MintSoft, RoundedCornerShape(12.dp))
                                .clickable(enabled = !state.busy) { onOpenCamera() }
                                .padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                    ScanLinkUiState.Phase.PREVIEW -> {
                        val preview = state.preview
                        val employee = preview?.employee
                        Text(
                            "确认添加该服务专员？",
                            color = Ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            listOfNotNull(
                                employee?.name,
                                employee?.orgName,
                                employee?.departmentName,
                            ).joinToString(" · ").ifBlank { "服务专员" },
                            color = Ink,
                            fontSize = 12.sp,
                        )
                        preview?.existingContact?.let { existing ->
                            Text(
                                "您已有服务专员「${existing.employeeName ?: "—"}」，确认更换？",
                                color = com.rehealth.genie.ui.theme.Muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    ScanLinkUiState.Phase.DONE -> {
                        Text(
                            state.message ?: "服务关系已建立",
                            color = Ink,
                            fontSize = 13.sp,
                        )
                    }
                }
                state.message?.takeIf { state.phase == ScanLinkUiState.Phase.INPUT }?.let { error ->
                    Text(error, color = Color(0xFFE5484D), fontSize = 12.sp)
                }
                if (state.busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Mint, strokeWidth = 2.dp)
                }
            }
        },
        confirmButton = {
            when (state.phase) {
                ScanLinkUiState.Phase.INPUT -> TextButton(enabled = !state.busy, onClick = onScan) { Text("识别") }
                ScanLinkUiState.Phase.PREVIEW -> TextButton(enabled = !state.busy, onClick = onConfirm) { Text("确认") }
                ScanLinkUiState.Phase.DONE -> TextButton(onClick = onDismiss) { Text("完成") }
            }
        },
        dismissButton = {
            when (state.phase) {
                ScanLinkUiState.Phase.PREVIEW -> TextButton(enabled = !state.busy, onClick = onBack) { Text("返回") }
                else -> TextButton(enabled = !state.busy, onClick = onDismiss) { Text("取消") }
            }
        },
    )
}
