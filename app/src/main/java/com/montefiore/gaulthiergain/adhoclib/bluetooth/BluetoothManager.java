package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

/**
 * Created by gaulthiergain on 25/10/17.
 *
 */

public class BluetoothManager {

    // Intent Request Code
    private static final int REQUEST_ENABLE_BT = 3;

    private final BluetoothAdapter bluetoothAdapter;

    public BluetoothManager() {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.d("[AdHoc]", "Error device does not support Bluetooth");
        }else{
            Log.d("[AdHoc]", "Device supports Bluetooth");
        }
    }

    public boolean activeBluetooth(){
        return bluetoothAdapter.isEnabled();
    }


}
