package com.rehealth.genie.ui

import androidx.compose.foundation.lazy.LazyListScope
import com.rehealth.genie.ring.RingUiState

@Suppress("UNUSED_PARAMETER")
internal fun LazyListScope.runtimeDeviceBindingExtras(
    state: RingUiState,
    onSwitchProduct: (String) -> Unit,
    onRuntimeDataChanged: () -> Unit,
) = Unit
