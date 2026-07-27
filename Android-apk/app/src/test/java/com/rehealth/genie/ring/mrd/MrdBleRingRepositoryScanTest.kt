package com.rehealth.genie.ring.mrd

import com.rehealth.genie.ring.provider.ActiveWearableBinding
import com.rehealth.genie.ring.provider.DEFAULT_MRD_PRODUCT_CODE
import com.rehealth.genie.ring.provider.WearableVendor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    @Test
    fun autoConnectCandidateRequiresAnMrdBindingAddress() {
        assertNull(mrdBoundDevice(binding(address = null)))
        assertNull(mrdBoundDevice(binding(address = "AA:BB", vendor = WearableVendor.RWFIT)))

        val device = mrdBoundDevice(binding(address = "AA:BB", deviceName = "MR11"))
        assertEquals("AA:BB", device?.address)
        assertEquals("MR11", device?.name)
    }

    private fun binding(
        address: String?,
        deviceName: String? = null,
        vendor: WearableVendor = WearableVendor.MRD,
    ) = ActiveWearableBinding(
        productCode = DEFAULT_MRD_PRODUCT_CODE,
        vendor = vendor,
        address = address,
        deviceName = deviceName,
        modelCode = null,
        firmwareVersion = null,
        capabilityJson = null,
        boundAt = 0L,
        lastDeviceChangedAt = 0L,
    )
}
