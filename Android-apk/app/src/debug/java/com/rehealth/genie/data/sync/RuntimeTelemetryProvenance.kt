package com.rehealth.genie.data.sync

import com.rehealth.genie.ring.provider.WearableVendor

internal fun runtimeTelemetryProvenance(
    vendor: WearableVendor,
    containsNonProductionInput: Boolean,
): String = if (containsNonProductionInput) "synthetic_qa" else "${vendor.name.lowercase()}_room"
