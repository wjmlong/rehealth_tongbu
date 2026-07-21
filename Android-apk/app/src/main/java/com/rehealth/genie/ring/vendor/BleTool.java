package com.rehealth.genie.ring.vendor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;

/**
 * BLE helper copied from the MRD SDK demo (com.manridy.sdkdemo_mrd2019.bluetooth.BleTool).
 */
public class BleTool {
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private Context context;
    public static final int requestCode = 8989;

    public BleTool(Context context) {
        this.context = context;
        initialize(context);
    }

    public BluetoothAdapter GetAdapter() {
        return mBluetoothAdapter;
    }

    public boolean initialize(Context context) {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    /**
     * 是否支持蓝牙
     */
    public boolean hasBleOpen() {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * 蓝牙是否打开
     */
    public boolean isBleOpen() {
        if (mBluetoothAdapter == null) {
            return false;
        }
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * 打开蓝牙
     */
    public boolean openBLE() {
        if (mBluetoothAdapter == null) {
            return false;
        }
        try {
            return mBluetoothAdapter.enable();
        } catch (Exception e) {
            return false;
        }
    }

    public static String ByteToString(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte byteChar : data) {
            stringBuilder.append(String.format("%02X ", byteChar));
        }
        return stringBuilder.toString();
    }

    public static String ByteToString2(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte byteChar : data) {
            stringBuilder.append(String.format("%02X", byteChar));
        }
        return stringBuilder.toString();
    }

    public static byte[] intToBytes(int n) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (n >> (i * 8));
        }
        return b;
    }

    public static int byteToInt(byte[] b) {
        int data = 0;
        for (int i = 0; i < b.length; i++) {
            data += (b[i] & 0x0ff) << (i * 8);
        }
        return data;
    }

    public static int[] b_or_int(byte[] data) {
        int[] data_i = new int[data.length];
        for (int i = 0; i < data.length; i++) {
            data_i[i] = data[i] & 0x0ff;
        }
        return data_i;
    }
}
