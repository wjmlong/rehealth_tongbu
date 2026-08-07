package com.rehealth.genie.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceBindingScreenTest {
    @Test
    fun bluetoothScanRequiresPermissionAndIdleState() {
        assertFalse(canStartBluetoothScan(permissionGranted = false, isScanning = false, isSyncing = false))
        assertFalse(canStartBluetoothScan(permissionGranted = true, isScanning = true, isSyncing = false))
        assertFalse(canStartBluetoothScan(permissionGranted = true, isScanning = false, isSyncing = true))
        assertTrue(canStartBluetoothScan(permissionGranted = true, isScanning = false, isSyncing = false))
    }
}
