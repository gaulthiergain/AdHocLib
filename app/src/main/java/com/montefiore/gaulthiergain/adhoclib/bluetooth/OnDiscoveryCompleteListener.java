package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothDevice;

import java.util.HashMap;

/**
 * Created by gaulthiergain on 26/10/17.
 *
 */

public interface OnDiscoveryCompleteListener {
    void OnDiscoveryComplete(HashMap<String, BluetoothDevice> hashMapBluetoothDevice);
}
