package com.rehealth.genie.ui

import androidx.compose.runtime.Composable
import com.rehealth.genie.qa.FullChainSimulationReport
import com.rehealth.genie.ring.RingUiState

@Composable
@Suppress("UNUSED_PARAMETER")
internal fun DeviceBindingDebugSection(
    state: RingUiState,
    onSwitchProduct: (String) -> Unit,
    simulationAvailable: Boolean,
    simulationRunning: Boolean,
    simulationReport: FullChainSimulationReport?,
    onRunFullChainSimulation: () -> Unit,
) = Unit
