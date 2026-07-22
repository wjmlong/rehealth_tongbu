package com.rehealth.genie.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Line
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.rehealth.genie.work.MeasurementSyncWorker

@Composable
internal fun ProfileScreen(
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
            ReHealthCardBlock {
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
            ReHealthCardBlock {
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
            ReHealthCardBlock {
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
