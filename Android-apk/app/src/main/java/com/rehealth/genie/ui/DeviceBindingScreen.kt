package com.rehealth.genie.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rehealth.genie.qa.FullChainSimulationReport
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingAcquisitionMode
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ui.theme.Canvas
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted

@Composable
internal fun DeviceBindingScreen(
    state: RingUiState,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onConnect: (RingDevice) -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit,
    onStartBackgroundCollection: () -> Unit = {},
    onStopBackgroundCollection: () -> Unit = {},
    onMeasurementIntervalChanged: (Int) -> Unit = {},
    onUploadIntervalChanged: (Int) -> Unit = {},
    onSwitchProduct: (String) -> Unit = {},
    simulationAvailable: Boolean = false,
    simulationRunning: Boolean = false,
    simulationReport: FullChainSimulationReport? = null,
    onRunFullChainSimulation: () -> Unit = {},
    onboarding: Boolean = false,
    onComplete: (() -> Unit)? = null,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(hasBluetoothPermission(context))
    }
    var viomiImei by remember(state.activeProductCode) { mutableStateOf("") }
    var notificationPermissionDenied by remember { mutableStateOf(false) }
    var customMeasurementInterval by remember(state.measurementIntervalMinutes) {
        mutableStateOf(state.measurementIntervalMinutes.toString())
    }
    var customUploadInterval by remember(state.uploadIntervalMinutes) {
        mutableStateOf(state.uploadIntervalMinutes.toString())
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        permissionGranted = results.values.all { it }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionDenied = !granted
        if (granted) onStartBackgroundCollection()
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
                    Text(if (onboarding) "连接你的健康设备" else "设备绑定", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (onboarding) "连接后即可进入主页" else "绑定可穿戴设备并同步健康数据",
                        color = Muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        item {
            DeviceBindingDebugSection(
                state = state,
                onSwitchProduct = onSwitchProduct,
                simulationAvailable = simulationAvailable,
                simulationRunning = simulationRunning,
                simulationReport = simulationReport,
                onRunFullChainSimulation = onRunFullChainSimulation,
            )
        }
        if (state.acquisitionMode == RingAcquisitionMode.BLUETOOTH) item {
            ReHealthCardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(MintSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Bluetooth, null, tint = Mint)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("蓝牙权限", color = Ink, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (permissionGranted) "已授权，可连接 HBand 设备" else "连接 HBand 设备需要授权",
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
                    "已切换为 HBand 蓝牙链路，健康数据会先保存在本机。",
                    color = Muted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
        if (state.acquisitionMode == RingAcquisitionMode.CLOUD) item {
            ReHealthCardBlock {
                Text("绑定云米云端手表", color = Ink, fontWeight = FontWeight.SemiBold)
                Text(
                    "输入手表 IMEI。云米密钥和 AccessToken 只保存在 ReHealth 后端。",
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                OutlinedTextField(
                    value = viomiImei,
                    onValueChange = { viomiImei = it.filter(Char::isDigit).take(32) },
                    label = { Text("手表 IMEI") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
                Button(
                    onClick = { onConnect(RingDevice(viomiImei, "云米云端手表", null)) },
                    enabled = viomiImei.length >= 8 && !state.isSyncing,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                ) {
                    Text("验证并绑定")
                }
            }
        }
        item {
            ReHealthCardBlock {
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
                    val identifier = if (state.acquisitionMode == RingAcquisitionMode.CLOUD) {
                        "IMEI ••••${it.address.takeLast(4)}"
                    } else {
                        "${it.address} · RSSI ${it.rssi ?: "--"}"
                    }
                    Text(identifier, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
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
                    if (state.acquisitionMode == RingAcquisitionMode.BLUETOOTH) Button(
                        onClick = onScan,
                        enabled = canStartBluetoothScan(
                            permissionGranted = permissionGranted,
                            isScanning = state.isScanning,
                            isSyncing = state.isSyncing,
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MintSoft, contentColor = Mint),
                    ) {
                        Text(if (state.isScanning) "搜索中" else "搜索 HBand 设备")
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
            ReHealthCardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(MintSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.NotificationsActive, null, tint = Mint)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            if (state.acquisitionMode == RingAcquisitionMode.CLOUD) "云端主动测量" else "后台自动采集",
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            when {
                                state.backgroundCollectionEnabled ->
                                    "已启用，每 ${state.measurementIntervalMinutes} 分钟主动测量；每 ${formatInterval(state.uploadIntervalMinutes)} 上传"
                                state.acquisitionMode == RingAcquisitionMode.CLOUD && state.connectedDevice == null ->
                                    "请先绑定云米设备"
                                state.acquisitionMode == RingAcquisitionMode.CLOUD -> "由服务器持续调度，App 关闭后仍可执行"
                                !state.hasBoundBluetoothDevice -> "请先绑定 HBand 设备"
                                else -> "锁屏或离开页面后继续主动测量"
                            },
                            color = Muted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
                Text(
                    "测量间隔",
                    color = Ink,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
                IntervalButtons(
                    options = listOf(3, 5, 10, 15),
                    selected = state.measurementIntervalMinutes,
                    label = { "$it 分钟" },
                    onSelected = onMeasurementIntervalChanged,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = customMeasurementInterval,
                        onValueChange = { customMeasurementInterval = it.filter(Char::isDigit).take(2) },
                        label = { Text("自定义 3–60 分钟", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { customMeasurementInterval.toIntOrNull()?.let(onMeasurementIntervalChanged) },
                        enabled = customMeasurementInterval.toIntOrNull() in 3..60,
                        colors = ButtonDefaults.buttonColors(containerColor = Mint),
                    ) { Text("应用") }
                }
                if (state.acquisitionMode == RingAcquisitionMode.BLUETOOTH) {
                    Text(
                        "上传间隔（数据先保存在本机）",
                        color = Ink,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    IntervalButtons(
                        options = listOf(30, 60, 120, 240),
                        selected = state.uploadIntervalMinutes,
                        label = ::formatInterval,
                        onSelected = onUploadIntervalChanged,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = customUploadInterval,
                            onValueChange = { customUploadInterval = it.filter(Char::isDigit).take(4) },
                            label = { Text("自定义 30–1440 分钟", fontSize = 10.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = { customUploadInterval.toIntOrNull()?.let(onUploadIntervalChanged) },
                            enabled = customUploadInterval.toIntOrNull() in 30..1440,
                            colors = ButtonDefaults.buttonColors(containerColor = Mint),
                        ) { Text("应用") }
                    }
                }
                Button(
                    onClick = {
                        if (state.backgroundCollectionEnabled) {
                            onStopBackgroundCollection()
                        } else if (
                            state.acquisitionMode == RingAcquisitionMode.BLUETOOTH &&
                            requiresNotificationPermission(context)
                        ) {
                            notificationPermissionLauncher.launch(POST_NOTIFICATIONS_PERMISSION)
                        } else {
                            onStartBackgroundCollection()
                        }
                    },
                    enabled = if (state.acquisitionMode == RingAcquisitionMode.CLOUD) {
                        state.connectedDevice != null
                    } else {
                        canChangeBackgroundCollection(
                            backgroundCollectionEnabled = state.backgroundCollectionEnabled,
                            bluetoothPermissionGranted = permissionGranted,
                            hasBoundBluetoothDevice = state.hasBoundBluetoothDevice,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.backgroundCollectionEnabled) Color(0xFFF1F4F3) else Mint,
                        contentColor = if (state.backgroundCollectionEnabled) Ink else Color.White,
                    ),
                ) {
                    Text(
                        if (state.backgroundCollectionEnabled) "关闭主动测量" else "启用主动测量",
                    )
                }
                if (notificationPermissionDenied) {
                    Text(
                        "需要通知权限才能清晰展示后台采集状态，请授权后重试。",
                        color = Color(0xFFC76555),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        item {
            ReHealthCardBlock {
                Text("数据采集目标", color = Ink, fontWeight = FontWeight.SemiBold)
                Text(
                    "健康数据：睡眠 · 血压 · 心率 · 步数 · 血氧\n设备功能：遥控拍照 · 女性健康",
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
                    "本机已保存 ${state.collectedMetricCount} 项健康数据",
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
                    Text(
                        if (state.isSyncing) "正在同步 ${state.syncProgress}%"
                        else if (state.acquisitionMode == RingAcquisitionMode.CLOUD) "从云米云端同步近 7 天数据"
                        else "同步全部健康数据",
                    )
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
            ReHealthCardBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Devices, null, tint = Mint, modifier = Modifier.size(28.dp))
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(device.name ?: "HBand 设备", color = Ink, fontWeight = FontWeight.SemiBold)
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
                        if (state.connectedDevice == null) "请先连接设备" else "完成设置，进入主页",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    "连接成功后，我们会同步设备数据并进入主页。",
                    color = Muted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                )
                // 暂时没有设备：跳过配对，直接进入主页（仍会标记首次引导已完成）
                OutlinedButton(
                    onClick = { onComplete?.invoke() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Mint),
                    border = BorderStroke(1.dp, Mint),
                ) {
                    Text("暂时没有设备，先跳过", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun IntervalButtons(
    options: List<Int>,
    selected: Int,
    label: (Int) -> String,
    onSelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
        options.chunked(2).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowOptions.forEach { option ->
                    OutlinedButton(
                        onClick = { onSelected(option) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, if (option == selected) Mint else Color(0xFFDCE5E2)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (option == selected) MintSoft else Color.Transparent,
                            contentColor = if (option == selected) Mint else Ink,
                        ),
                    ) {
                        Text(label(option), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun formatInterval(minutes: Int): String = when {
    minutes < 60 -> "$minutes 分钟"
    minutes % 60 == 0 -> "${minutes / 60} 小时"
    else -> "${minutes / 60} 小时 ${minutes % 60} 分"
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

internal fun canStartBluetoothScan(
    permissionGranted: Boolean,
    isScanning: Boolean,
    isSyncing: Boolean,
): Boolean = permissionGranted && !isScanning && !isSyncing

internal fun canChangeBackgroundCollection(
    backgroundCollectionEnabled: Boolean,
    bluetoothPermissionGranted: Boolean,
    hasBoundBluetoothDevice: Boolean,
): Boolean = backgroundCollectionEnabled ||
    (bluetoothPermissionGranted && hasBoundBluetoothDevice)

internal fun requiresNotificationPermission(context: Context): Boolean =
    requiresNotificationPermission(
        sdkInt = Build.VERSION.SDK_INT,
        permissionGranted = ContextCompat.checkSelfPermission(
            context,
            POST_NOTIFICATIONS_PERMISSION,
        ) == PackageManager.PERMISSION_GRANTED,
    )

internal fun requiresNotificationPermission(
    sdkInt: Int,
    permissionGranted: Boolean,
): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU && !permissionGranted

private const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"

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
