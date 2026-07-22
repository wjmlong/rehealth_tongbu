package com.rehealth.genie.ring.mrd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MrdBleRingRepositoryScanTest {
    @Test
    fun scanCandidateFilterPreservesAdvertisedNamedAndRssiRules() {
        assertTrue(shouldIncludeMrdScanResult(advertisesMrd = true, name = null, rssi = -110))
        assertTrue(shouldIncludeMrdScanResult(advertisesMrd = false, name = "Nearby BLE", rssi = -110))
        assertTrue(shouldIncludeMrdScanResult(advertisesMrd = false, name = null, rssi = -88))
        assertFalse(shouldIncludeMrdScanResult(advertisesMrd = false, name = null, rssi = -89))
    }

    @Test
    fun scanDisplayNamePreservesMrdLabels() {
        assertTrue(mrdScanDisplayName(advertisesMrd = true, name = null).contains("MRD"))
        assertEquals("Nearby BLE · MRD", mrdScanDisplayName(advertisesMrd = true, name = "Nearby BLE"))
        assertTrue(mrdScanDisplayName(advertisesMrd = false, name = null).isNotBlank())
        assertEquals("Nearby BLE", mrdScanDisplayName(advertisesMrd = false, name = "Nearby BLE"))
    }
}
