package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclib.exceptions.BluetoothDeviceException;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by gaulthiergain on 25/10/17.
 */

public class BluetoothManager {



    private final boolean v;
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final String TAG = "[AdHoc][" + getClass().getName() + "]";

    private HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice;

    private OnDiscoveryCompleteListener listener;

    public BluetoothManager(Context context, OnDiscoveryCompleteListener listener)
            throws BluetoothDeviceException {
        this.context = context;
        this.v = true;
        this.listener = listener;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            throw new BluetoothDeviceException("Error device does not support Bluetooth");
        } else {
            // Device supports Bluetooth
            hashMapBluetoothDevice = new HashMap<>();
        }
    }

    public boolean isEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    public boolean enable() {
        return bluetoothAdapter.enable();
    }

    public boolean disable() {
        return bluetoothAdapter.disable();
    }

    public HashMap<String, BluetoothAdHocDevice> getPairedDevices() {
        if(v) Log.d(TAG, "getPairedDevices()");

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        HashMap<String, BluetoothAdHocDevice> hashMapBluetoothPairedDevice = new HashMap<>();

        if (pairedDevices.size() > 0) {
            // Add each paired device into a hashMap
            for (BluetoothDevice device : pairedDevices) {
                if(v) Log.d(TAG, "DeviceName: " + device.getName() +
                        " - DeviceHardwareAddress: " + device.getAddress());
                hashMapBluetoothPairedDevice.put(device.getAddress(),
                        new BluetoothAdHocDevice(device));
            }
        }
        return hashMapBluetoothPairedDevice;
    }

    public void discovery() {
        if(v) Log.d(TAG, "discovery()");

        // Check if the device is already "discovering". If it is, then cancel discovery.
        cancelDiscovery();

        // Start Discovery
        bluetoothAdapter.startDiscovery();

        // Register for broadcasts when a device is discovered.
        context.getApplicationContext().registerReceiver(mReceiver, new IntentFilter(
                BluetoothDevice.ACTION_FOUND));
        context.getApplicationContext().registerReceiver(mReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        context.getApplicationContext().registerReceiver(mReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    public void cancelDiscovery() {
        if(v) Log.d(TAG, "cancelDiscovery()");

        // Check if the device is already "discovering". If it is, then cancel discovery.
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //Log.d("[AdHoc]", "Action BroadcastReceiver: " + action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Get the BluetoothDevice object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Add into the hashMap
                if (!hashMapBluetoothDevice.containsKey(device.getAddress())) {
                    if(v) Log.d(TAG, "DeviceName: " + device.getName() +
                            " - DeviceHardwareAddress: " + device.getAddress());
                    hashMapBluetoothDevice.put(device.getAddress(), new BluetoothAdHocDevice(device,
                            intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)));
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                if(v) Log.d(TAG, "ACTION_DISCOVERY_STARTED");
                // Clear the hashMap
                hashMapBluetoothDevice.clear();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if(v) Log.d(TAG, "ACTION_DISCOVERY_FINISHED");
                // Listener
                listener.OnDiscoveryComplete(hashMapBluetoothDevice);
            }
        }
    };

    public void unregisterDiscovery() throws IllegalArgumentException {
        if(v) Log.d(TAG, "unregisterDiscovery()");
        context.getApplicationContext().unregisterReceiver(mReceiver);
    }

    public HashMap<String, BluetoothAdHocDevice> getHashMapBluetoothDevice() {
        return hashMapBluetoothDevice;
    }
}
