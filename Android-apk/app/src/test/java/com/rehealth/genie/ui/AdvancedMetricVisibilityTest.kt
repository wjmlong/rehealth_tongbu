package com.rehealth.genie.ui

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingMeasurementEntity
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdvancedMetricVisibilityTest {
    @Test
    fun `shows only valid advanced values from real providers`() {
        assertTrue(isDisplayableAdvancedMeasurement(RingMetricType.HRV, measurement(48.0)))
        assertTrue(isDisplayableAdvancedMeasurement(RingMetricType.STRESS, measurement(1.0)))
        assertTrue(isDisplayableAdvancedMeasurement(RingMetricType.STRESS, measurement(100.0)))
        assertTrue(isDisplayableAdvancedMeasurement(RingMetricType.MET, measurement(1.4)))

        assertFalse(isDisplayableAdvancedMeasurement(RingMetricType.HRV, measurement(0.0)))
        assertFalse(isDisplayableAdvancedMeasurement(RingMetricType.STRESS, measurement(0.0)))
        assertFalse(isDisplayableAdvancedMeasurement(RingMetricType.STRESS, measurement(101.0)))
        assertFalse(isDisplayableAdvancedMeasurement(RingMetricType.MET, measurement(Double.NaN)))
        assertFalse(isDisplayableAdvancedMeasurement(RingMetricType.MET, measurement(1.4, "ring_sim")))
        assertFalse(isDisplayableAdvancedMeasurement(RingMetricType.HRV, measurement(48.0, "synthetic_qa")))
        assertFalse(isDisplayableAdvancedMeasurement(RingMetricType.STRESS, measurement(50.0, "mock_hband")))
        assertFalse(isDisplayableAdvancedMeasurement(RingMetricType.HRV, null))
    }

    @Test
    fun `synthetic advanced measurements are visible only when explicitly allowed`() {
        assertTrue(
            isDisplayableAdvancedMeasurement(
                RingMetricType.HRV,
                measurement(48.0, "ring_sim"),
                allowSynthetic = true,
            ),
        )
        assertTrue(
            isDisplayableAdvancedMeasurement(
                RingMetricType.STRESS,
                measurement(50.0, "synthetic_qa"),
                allowSynthetic = true,
            ),
        )
    }

    private fun measurement(value: Double, source: String = "hband_wearable") = RingMeasurementEntity(
        id = "id-$value-$source",
        metricType = RingMetricType.HRV.name,
        measuredAt = 1_700_000_000_000L,
        primaryValue = value,
        unit = "vendor_unit",
        source = source,
    )
}
