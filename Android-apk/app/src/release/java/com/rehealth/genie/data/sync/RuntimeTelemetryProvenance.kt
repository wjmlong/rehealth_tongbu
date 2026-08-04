package com.rehealth.genie.data.sync

import com.rehealth.genie.ring.provider.WearableVendor

internal fun runtimeTelemetryProvenance(
    vendor: WearableVendor,
    containsNonProductionInput: Boolean,
): String {
    require(!containsNonProductionInput) {
        "Release builds refuse to upload non-production telemetry."
    }
    return "${vendor.name.lowercase()}_room"
}
