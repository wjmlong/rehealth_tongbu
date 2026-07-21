package com.rehealth.genie.ring.vendor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import java.util.ArrayList;

/**
 * Singleton LE scanner copied from the MRD SDK demo
 * (com.manridy.sdkdemo_mrd2019.bluetooth.SearchBle).
 */
public class SearchBle {
    private final static String TAG = SearchBle.class.getSimpleName();
    private static SearchBle instance;
    private BleTool mBleTool;
    private ArrayList<SearchListener.ScanListener> ListenerList = new ArrayList<>();

    private SearchBle(Context mContext) {
        mBleTool = new BleTool(mContext);
    }

    public static SearchBle getInstance(Context mContext) {
        if (instance == null) {
            synchronized (SearchBle.class) {
                if (instance == null) {
                    instance = new SearchBle(mContext.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void addListener(SearchListener.ScanListener SearchListener) {
        if (!ListenerList.contains(SearchListener)) {
            ListenerList.add(SearchListener);
        }
    }

    public void removeListener(SearchListener.ScanListener SearchListener) {
        ListenerList.remove(SearchListener);
        if (ListenerList.size() == 0) {
            stop();
        }
    }

    @SuppressLint("MissingPermission")
    public boolean search() {
        if (!mBleTool.hasBleOpen()) {
            return false;
        }
        if (!mBleTool.isBleOpen()) {
            return false;
        }
        mBleTool.GetAdapter().stopLeScan(scanCallback);
        return mBleTool.GetAdapter().startLeScan(scanCallback);
    }

    @SuppressLint("MissingPermission")
    public void stop() {
        try {
            mBleTool.GetAdapter().stopLeScan(scanCallback);
        } catch (Exception e) {
        }
    }

    BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
            if (TextUtils.isEmpty(bluetoothDevice.getName())) {
                return;
            }
            Bundle bun = new Bundle();
            bun.putParcelable(hanKey_Ble, bluetoothDevice);
            bun.putByteArray(hanKey_scanRecord, scanRecord);
            bun.putInt(hanKey_Rssi, rssi);
            Message msg = new Message();
            msg.setData(bun);
            hanLeScan.sendMessage(msg);
        }
    };

    private String hanKey_Ble = "mBle";
    private String hanKey_Rssi = "mRssi";
    private String hanKey_scanRecord = "scanRecord";
    @SuppressLint("HandlerLeak")
    Handler hanLeScan = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle bun = msg.getData();
            BluetoothDevice mBle = bun.getParcelable(hanKey_Ble);
            int rssi = bun.getInt(hanKey_Rssi, 0);
            if (mBle == null) {
                return;
            }
            try {
                for (SearchListener.ScanListener listener : ListenerList) {
                    if (listener != null) {
                        listener.onLeScan(mBle, rssi);
                    }
                }
            } catch (Exception r) {
            }
        }
    };
}
