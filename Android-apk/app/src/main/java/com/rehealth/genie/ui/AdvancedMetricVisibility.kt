package com.rehealth.genie.ui

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingMeasurementEntity

private val VALUE_GATED_ADVANCED_METRICS = setOf(
    RingMetricType.HRV,
    RingMetricType.STRESS,
    RingMetricType.MET,
)

internal fun isDisplayableAdvancedMeasurement(
    type: RingMetricType,
    measurement: RingMeasurementEntity?,
): Boolean {
    if (type !in VALUE_GATED_ADVANCED_METRICS || measurement == null) return false
    val source = measurement.source.trim()
    if (
        source.isEmpty() ||
        source.equals("ring_sim", ignoreCase = true) ||
        source.contains("mock", ignoreCase = true) ||
        source.contains("synthetic", ignoreCase = true)
    ) {
        return false
    }
    val value = measurement.primaryValue
    if (!value.isFinite()) return false
    return when (type) {
        RingMetricType.HRV, RingMetricType.MET -> value > 0.0
        RingMetricType.STRESS -> value in 1.0..100.0
        else -> false
    }
}
