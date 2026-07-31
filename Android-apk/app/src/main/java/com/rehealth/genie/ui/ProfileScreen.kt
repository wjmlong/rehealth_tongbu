package com.rehealth.genie.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rehealth.genie.work.MeasurementSyncWorker

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
    add(
        "性别" to when (normalizeProfileGender(profile?.gender)) {
            "male" -> "男"
            "female" -> "女"
            else -> "待补全"
        },
    )
    add("家族史" to profile?.familyHistory.toArchiveBoolean())
    add("高血压史" to profile?.hypertensionHistory.toArchiveBoolean())
    add("糖尿病史" to profile?.diabetesHistory.toArchiveBoolean())
    interview?.baselineItems.orEmpty().forEach { item ->
        add("健康问答 · ${item.label}" to item.value)
    }
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
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showRhiInputDialog by remember { mutableStateOf(false) }
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
    LaunchedEffect(Unit) {
        onRefreshProfile()
        rhiInputViewModel.observeCurrentUser()
    }
    LaunchedEffect(editState.saved) {
        if (editState.saved) {
            showEditDialog = false
            editViewModel.reset()
            onProfileUpdated()
        }
    }
    LaunchedEffect(rhiInputState.savedVersion) {
        if (rhiInputState.savedVersion > 0) showRhiInputDialog = false
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
        title = { Text("健康与归因指标") },
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
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = labConfirmed, onCheckedChange = { labConfirmed = it })
                    Text("确认已逐项核对医院报告原件", color = Muted, fontSize = 11.sp)
                }
                Text(
                    "血压与代谢卡按 80% 实测贡献 + 20% 控制支持趋势展示；缺少可验证趋势时，20% 保持为 0，不补造数值。",
                    color = Muted,
                    fontSize = 11.sp,
                )
                errorMessage?.let { Text(it, color = Color(0xFFD94C4C), fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
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
        modifier = Modifier.fillMaxWidth(),
    )
}

/** D3 logout: cancel the sync worker, clear the auth session, and pause the upload queue. */
private fun performLogout(context: Context) {
    val app = context.applicationContext as ReHealthApplication
    MeasurementSyncWorker.cancel(context)
    app.authenticatedApiClient.onLogout()
    app.syncRepository.pauseQueue()
}

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
        title = { Text("编辑个人资料") },
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
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("身高 (cm)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' }.take(6) },
                    label = { Text("体重 (kg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                errorMessage?.let {
                    Text(it, color = Color(0xFFD94C4C), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
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
