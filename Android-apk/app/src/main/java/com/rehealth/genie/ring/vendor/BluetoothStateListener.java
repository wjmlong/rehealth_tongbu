package com.rehealth.genie.ring.vendor;

import android.bluetooth.BluetoothDevice;

/**
 * Copied from the MRD SDK demo. Connection state + inbound data callback surface.
 */
public interface BluetoothStateListener {
    void onChange(BluetoothDevice device, BleState state);
    void onReadChange(BluetoothDevice device, byte[] datas);
}
