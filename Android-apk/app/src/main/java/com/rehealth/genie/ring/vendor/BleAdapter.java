package com.rehealth.genie.ring.vendor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.manridy.sdk_mrd2019.install.MrdPushCore;

/**
 * Core BLE connection adapter copied from the MRD SDK demo
 * (com.manridy.sdkdemo_mrd2019.bluetooth.BleAdapter). Transport logic is kept
 * verbatim; only the package and Toast calls were adjusted for a headless
 * (repository) usage.
 */
public class BleAdapter extends BluetoothGattCallback {
    private final static String TAG = BleAdapter.class.getSimpleName();
    private BleTool mBleTool;
    private BluetoothGatt gatt;
    private BluetoothDevice device;
    private Context context;
    private BleState state = BleState.DISCONNECTED;
    private BluetoothStateListener listener;

    public BleAdapter(Context context) {
        this.context = context.getApplicationContext();
        mBleTool = new BleTool(this.context);
    }

    public boolean connect() {
        return connect(device);
    }

    @SuppressLint("MissingPermission")
    public boolean connect(BluetoothDevice device) {
        this.device = device;
        if (mBleTool.GetAdapter() == null) {
            Log.e(TAG, " mBleTool.GetAdapter() == null");
            return false;
        }
        if (device == null) {
            Log.e(TAG, "device == null");
            return false;
        }
        putHandler(BleState.CONNECTING, device);
        gatt = device.connectGatt(context, false, this);
        MrdPushCore.getInstance().init(gatt);
        return true;
    }

    @SuppressLint("MissingPermission")
    public void close() {
        Log.e(TAG, "close");
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
        state = BleState.DISCONNECTED;
        if (listener != null) {
            listener.onChange(device, state);
        }
        han.removeCallbacksAndMessages(null);
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.i(TAG, "mtu is " + mtu + " status is " + status);
        }
        MrdPushCore.getInstance().onMtuChanged(mtu, status);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                Log.e(TAG, "STATE_CONNECTED");
                putHandler(BleState.CONNECTED, gatt.getDevice());
                gatt.discoverServices();
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                Log.e(TAG, "STATE_DISCONNECTED");
                putHandler(BleState.DISCONNECTED, gatt.getDevice());
                break;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Log.e(TAG, "onServicesDiscovered");
        putHandler(BleState.SERVICES_DISCOVERED, gatt.getDevice());
        enableLostNoti();
    }

    /**
     * 打开读服务
     */
    @SuppressLint("MissingPermission")
    public Boolean enableLostNoti() {
        BluetoothGattService nableService = gatt.getService(SampleGattAttributes.NotifyServiceUUID);
        if (nableService == null) {
            return false;
        }
        BluetoothGattCharacteristic TxPowerLevel = nableService.getCharacteristic(SampleGattAttributes.NotifyCharacteristicUUID);
        if (TxPowerLevel == null) {
            return false;
        }
        Boolean isNotification = gatt.setCharacteristicNotification(TxPowerLevel, true);
        if (SampleGattAttributes.NotifyCharacteristicUUID.equals(TxPowerLevel.getUuid())) {
            BluetoothGattDescriptor descriptor = TxPowerLevel.getDescriptor(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
            if ((TxPowerLevel.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else if ((TxPowerLevel.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            }
            gatt.writeDescriptor(descriptor);
        }
        return isNotification;
    }

    @SuppressLint("MissingPermission")
    public boolean LostWriteData(byte[] pwm_data_buf) {
        try {
            if (pwm_data_buf == null) {
                return false;
            }
            if (gatt == null)
                return false;
            BluetoothGattService alertService = gatt.getService(SampleGattAttributes.WriteServiceUUID);
            if (alertService == null) {
                return false;
            }
            BluetoothGattCharacteristic alertLevel = alertService.getCharacteristic(SampleGattAttributes.WriteCharacteristicUUID);
            if (alertLevel == null) {
                return false;
            }
            boolean status = false;
            int storedLevel = alertLevel.getWriteType();
            alertLevel.setValue(pwm_data_buf);
            alertLevel.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            status = gatt.writeCharacteristic(alertLevel);
            Log.i(TAG, status + "-data=" + BleTool.ByteToString(pwm_data_buf));
            return status;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        byte[] value = characteristic.getValue();
        MrdPushCore.getInstance().readData(value);
        putHandler(value, gatt.getDevice());
        Log.i(TAG, "read=" + BleTool.ByteToString(value));
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        MrdPushCore.getInstance().onCharacteristicWrite(status, characteristic);
        Log.w(TAG, "write=" + BleTool.ByteToString(characteristic.getValue()));
    }

    private String hanKey_Device = "Device";
    private String hanKey_State = "State";
    private String hanKey_Datas = "Datas";

    @SuppressLint({"HandlerLeak", "MissingPermission"})
    Handler han = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1: {
                    Bundle bundle = msg.getData();
                    BleState newState = (BleState) bundle.getSerializable(hanKey_State);
                    BluetoothDevice device = bundle.getParcelable(hanKey_Device);
                    switch (newState) {
                        case CONNECTING:
                            break;
                        case CONNECTED:
                            break;
                        case DISCONNECTED:
                            if (gatt != null) {
                                gatt.disconnect();
                                gatt.close();
                            }
                            gatt = null;
                            break;
                        case SERVICES_DISCOVERED:
                            break;
                    }
                    if (listener != null) {
                        listener.onChange(device, newState);
                    }
                    state = newState;
                }
                break;
                case 2: {
                    Bundle bundle = msg.getData();
                    byte[] datas = bundle.getByteArray(hanKey_Datas);
                    BluetoothDevice device = bundle.getParcelable(hanKey_Device);
                    if (listener != null) {
                        listener.onReadChange(device, datas);
                    }
                }
                break;
            }
        }
    };

    private void putHandler(BleState bleState, BluetoothDevice device) {
        Bundle bundle = new Bundle();
        Message message = new Message();
        bundle.putSerializable(hanKey_State, bleState);
        bundle.putParcelable(hanKey_Device, device);
        message.setData(bundle);
        message.what = 1;
        han.sendMessage(message);
    }

    private void putHandler(byte[] datas, BluetoothDevice device) {
        Bundle bundle = new Bundle();
        Message message = new Message();
        bundle.putByteArray(hanKey_Datas, datas);
        bundle.putParcelable(hanKey_Device, device);
        message.setData(bundle);
        message.what = 2;
        han.sendMessage(message);
    }

    public BleState getState() {
        return state;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setListener(BluetoothStateListener listener) {
        this.listener = listener;
    }
}
