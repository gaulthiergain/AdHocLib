package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.util.Log;

/**
 * Created by gaulthiergain on 25/10/17.
 *
 */

public class BluetoothManager extends Activity {

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

    public void activeBluetooth(){
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.

            }
        }
    }
}
