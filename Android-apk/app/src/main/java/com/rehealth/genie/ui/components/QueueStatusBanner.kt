package com.rehealth.genie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rehealth.genie.data.sync.QueueState

/**
 * D3 queue status banner shown at top of screen.
 *
 * States:
 * - Active + uploading: "Syncing N feedback..." (blue)
 * - Active + terminal failure: show a retry-required error instead of an endless sync state
 * - Paused: "Session expired, tap to login" (yellow, clickable)
 * - Active + no pending: Hidden
 */
@Composable
fun QueueStatusBanner(
    queueState: QueueState,
    pendingCount: Int,
    failedCount: Int,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (queueState) {
        QueueState.Paused -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable(onClick = onLoginClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "会话已过期",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = "点击重新登录以同步 ${pendingCount + failedCount} 条反馈",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }
        QueueState.Active -> {
            if (failedCount > 0) {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = "$failedCount 条反馈同步失败，请检查登录和网络后重新提交" +
                            if (pendingCount > 0) "；另有 $pendingCount 条等待同步" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            } else if (pendingCount > 0) {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "正在同步 $pendingCount 条反馈...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            // else: no banner when active and no pending items
        }
    }
}
