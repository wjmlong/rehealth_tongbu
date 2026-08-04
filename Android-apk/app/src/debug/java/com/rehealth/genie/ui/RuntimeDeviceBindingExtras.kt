package com.rehealth.genie.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rehealth.genie.ReHealthApplication
import com.rehealth.genie.qa.FullChainSimulationReport
import com.rehealth.genie.qa.SimulationStageStatus
import com.rehealth.genie.qa.createRuntimeFullChainSimulationRunner
import com.rehealth.genie.ring.RingUiState
import com.rehealth.genie.ui.theme.Ink
import com.rehealth.genie.ui.theme.Mint
import com.rehealth.genie.ui.theme.MintSoft
import com.rehealth.genie.ui.theme.Muted
import kotlinx.coroutines.launch

internal fun LazyListScope.runtimeDeviceBindingExtras(
    state: RingUiState,
    onSwitchProduct: (String) -> Unit,
    onRuntimeDataChanged: () -> Unit,
) {
    item {
        val application = LocalContext.current.applicationContext as ReHealthApplication
        val runner = remember(application) { createRuntimeFullChainSimulationRunner(application) }
        val scope = rememberCoroutineScope()
        var pendingProductCode by remember { mutableStateOf<String?>(null) }
        var confirmSimulation by remember { mutableStateOf(false) }
        var simulationRunning by remember { mutableStateOf(false) }
        var simulationReport by remember { mutableStateOf<FullChainSimulationReport?>(null) }

        pendingProductCode?.let { productCode ->
            val product = state.wearableProducts.firstOrNull { it.productCode == productCode }
            AlertDialog(
                onDismissRequest = { pendingProductCode = null },
                title = { Text("切换设备套餐") },
                text = { Text("将断开当前设备并切换到 ${product?.displayName ?: productCode}。历史健康数据会保留。") },
                confirmButton = {
                    Button(onClick = {
                        pendingProductCode = null
                        onSwitchProduct(productCode)
                    }) { Text("确认切换") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { pendingProductCode = null }) { Text("取消") }
                },
            )
        }
        if (confirmSimulation) {
            AlertDialog(
                onDismissRequest = { confirmSimulation = false },
                title = { Text("运行 50 岁男性全链路演练") },
                text = {
                    Text(
                        "仅限 Debug：将当前测试账号健康档案改为 50 岁男性正常值，写入 90 天 synthetic_qa 数据，" +
                            "并真实调用设备绑定、遥测上传、RHI、RDI-16 与 PIAS。请勿在真实用户账号运行。",
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        confirmSimulation = false
                        if (!simulationRunning) {
                            scope.launch {
                                simulationRunning = true
                                simulationReport = runCatching { runner.run() }.getOrNull()
                                simulationRunning = false
                                onRuntimeDataChanged()
                            }
                        }
                    }) { Text("确认运行") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { confirmSimulation = false }) { Text("取消") }
                },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.wearableProducts.isNotEmpty()) {
                ReHealthCardBlock {
                    Text("Debug 套餐设备", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(
                        "仅用于验证 productCode 路由；Release 不提供此入口。",
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
            if (runner.available) {
                ReHealthCardBlock {
                    Text("Debug 全链路演练", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(
                        "唯一模拟数据入口；结果值仍由真实 RHI / RDI-16 / PIAS 链路计算。",
                        color = Muted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Button(
                        onClick = { confirmSimulation = true },
                        enabled = !simulationRunning,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint),
                    ) {
                        Text(if (simulationRunning) "正在真实跑完整链路…" else "生成 50 岁男性正常数据并运行")
                    }
                    if (simulationRunning) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            color = Mint,
                            trackColor = MintSoft,
                        )
                    }
                    simulationReport?.stages?.forEach { stage ->
                        val color = when (stage.status) {
                            SimulationStageStatus.SUCCESS -> Mint
                            SimulationStageStatus.WARNING -> Color(0xFFD38B18)
                            SimulationStageStatus.FAILED -> Color(0xFFC94B4B)
                        }
                        val marker = when (stage.status) {
                            SimulationStageStatus.SUCCESS -> "✓"
                            SimulationStageStatus.WARNING -> "!"
                            SimulationStageStatus.FAILED -> "×"
                        }
                        Text(
                            "$marker ${stage.label}：${stage.detail}",
                            color = color,
                            fontSize = 10.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 7.dp),
                        )
                    }
                }
            }
        }
    }
}
