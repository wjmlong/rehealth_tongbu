package com.rehealth.genie.ring.vendor;

import android.bluetooth.BluetoothDevice;

/**
 * Copied from the MRD SDK demo. LE scan callback surface.
 */
public class SearchListener {
    public interface ScanListener {
        void onLeScan(BluetoothDevice bluetoothDevice, int rssi);
    }
}
