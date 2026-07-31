package com.rehealth.genie.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rehealth.genie.rhi.RhiCalculationSource

@Composable
internal fun RhiCalculationSourceSelector(
    source: RhiCalculationSource,
    onSourceSelected: (RhiCalculationSource) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("RHI 计算方式")
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = source == RhiCalculationSource.LOCAL,
                onClick = { onSourceSelected(RhiCalculationSource.LOCAL) },
                label = { Text("本地即时") },
            )
            FilterChip(
                selected = source == RhiCalculationSource.REMOTE,
                onClick = { onSourceSelected(RhiCalculationSource.REMOTE) },
                label = { Text("JeecgBoot 远程（预览）") },
            )
        }
    }
}
