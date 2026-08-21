package com.rehealth.genie.ui

import android.os.Build
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

    @Test
    fun backgroundCollectionRequiresBindingAndBluetoothPermissionButCanAlwaysBeStopped() {
        assertFalse(canChangeBackgroundCollection(false, false, false))
        assertFalse(canChangeBackgroundCollection(false, true, false))
        assertFalse(canChangeBackgroundCollection(false, false, true))
        assertTrue(canChangeBackgroundCollection(false, true, true))
        assertTrue(canChangeBackgroundCollection(true, false, false))
    }

    @Test
    fun notificationPermissionIsRequestedOnlyOnAndroid13AndLater() {
        assertFalse(requiresNotificationPermission(Build.VERSION_CODES.S_V2, permissionGranted = false))
        assertTrue(requiresNotificationPermission(Build.VERSION_CODES.TIRAMISU, permissionGranted = false))
        assertFalse(requiresNotificationPermission(Build.VERSION_CODES.TIRAMISU, permissionGranted = true))
    }
}
