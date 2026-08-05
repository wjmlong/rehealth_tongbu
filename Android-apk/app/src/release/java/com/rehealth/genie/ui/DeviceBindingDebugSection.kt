package com.rehealth.genie.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.qa.FullChainSimulationReport
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.Muted

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun DeviceBindingDebugSection(
    state: RingUiState,
    onSwitchProduct: (String) -> Unit,
    simulationAvailable: Boolean,
    simulationRunning: Boolean,
    simulationReport: FullChainSimulationReport?,
    onRunFullChainSimulation: () -> Unit,
) {
    var pendingProductCode by remember { mutableStateOf<String?>(null) }

    pendingProductCode?.let { productCode ->
        val product = state.wearableProducts.firstOrNull { it.productCode == productCode }
        AlertDialog(
            onDismissRequest = { pendingProductCode = null },
            title = { Text("切换设备型号") },
            text = {
                Text("将断开当前设备并切换到 ${product?.displayName ?: productCode}。历史健康数据会保留。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingProductCode = null
                        onSwitchProduct(productCode)
                    },
                ) { Text("确认切换") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingProductCode = null }) { Text("取消") }
            },
        )
    }

    if (state.wearableProducts.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReHealthCardBlock {
            Text("设备型号", color = Ink, fontWeight = FontWeight.SemiBold)
            Text(
                "选择实际佩戴设备；云米型号使用 IMEI 绑定，其他设备使用蓝牙连接。",
                color = Muted,
                fontSize = 10.sp,
            )
            state.wearableProducts.chunked(2).forEach { rowProducts ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowProducts.forEach { product ->
                        AssistChip(
                            onClick = {
                                if (product.productCode != state.activeProductCode) {
                                    pendingProductCode = product.productCode
                                }
                            },
                            label = { Text(product.displayName, fontSize = 10.sp) },
                            enabled = !state.isScanning && !state.isSyncing,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Text(
                "当前：${state.activeProductCode ?: "未选择"}",
                color = Mint,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
