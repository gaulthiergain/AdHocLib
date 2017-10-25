package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import java.util.Set;

/**
 * Created by gaulthiergain on 25/10/17.
 *
 */

public class BluetoothManager {

    private BluetoothAdapter bluetoothAdapter;
    private Context context;

    public BluetoothManager(Context context) {
        this.context = context;
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

    public void getPairedDevices(){
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Log.d("[AdHoc]", "Device Paired: ");
        if (pairedDevices.size() > 0) {
            // Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d("[AdHoc]", "DeviceName: " + deviceName);
                Log.d("[AdHoc]", "DeviceHardwareAddress: " + deviceHardwareAddress);
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Toast.makeText(context, deviceName + "-" + deviceHardwareAddress, Toast.LENGTH_LONG).show();
                Log.d("[AdHoc]", "DeviceName: " + deviceName);
                Log.d("[AdHoc]", "DeviceHardwareAddress: " + deviceHardwareAddress);
            }
        }

    };

    public void discovery(){
        // Check if the device is already "discovering". If it is, then cancel discovery.
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Start Discovery
        bluetoothAdapter.startDiscovery();

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.getApplicationContext().registerReceiver(mReceiver, filter);

    }

    public void unregisterDiscovery(){
        context.getApplicationContext().unregisterReceiver(mReceiver);
    }

}
