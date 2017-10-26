package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by gaulthiergain on 25/10/17.
 *
 */

public class BluetoothManager {

    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;

    private HashMap<String, BluetoothDevice> hashMapBluetoothDevice;
    private OnDiscoveryCompleteListener listener;


    public BluetoothManager(Context context, OnDiscoveryCompleteListener listener) {
        this.context = context;
        this.listener = listener;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.d("[AdHoc]", "Error device does not support Bluetooth");
        }else{
            Log.d("[AdHoc]", "Device supports Bluetooth");
            hashMapBluetoothDevice = new HashMap<String, BluetoothDevice>();
        }
    }

    public boolean isEnabled(){
        return bluetoothAdapter.isEnabled();
    }

    public boolean enable(){
        return bluetoothAdapter.enable();
    }

    public boolean disable(){
        return bluetoothAdapter.disable();
    }

    public void getPairedDevices(){
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Log.d("[AdHoc]", "Device Paired: ");
        if (pairedDevices.size() > 0) {
            // Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d("[AdHoc]", "DeviceName: " + deviceName + " - DeviceHardwareAddress: " + deviceHardwareAddress);
            }
        }
    }


    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Get the BluetoothDevice object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Add into the hashMap
                if(!hashMapBluetoothDevice.containsKey(device.getAddress())){
                    hashMapBluetoothDevice.put(device.getAddress(), device);

                    listener.OnDiscoveryComplete(device.getAddress());

                    // Debug
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress();
                    Log.d("[AdHoc]", "DeviceName: " + deviceName + " - DeviceHardwareAddress: " + deviceHardwareAddress);
                }
            }
        }
    };

    public void cancelDiscovery(){
        // Check if the device is already "discovering". If it is, then cancel discovery.
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

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

    public void unregisterDiscovery() throws IllegalArgumentException{
        Log.d("[AdHoc]", "unregisterDiscovery()");
        context.getApplicationContext().unregisterReceiver(mReceiver);
    }

    public HashMap<String, BluetoothDevice> getHashMapBluetoothDevice() {
        return hashMapBluetoothDevice;
    }
}
