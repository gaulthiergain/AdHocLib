package com.montefiore.gaulthiergain.adhoclibrary.auto;


import android.bluetooth.BluetoothDevice;

public class SmartBluetoothDevice extends SmartDevice {
    private BluetoothDevice bluetoothDevice;
    private int rssi;

    public SmartBluetoothDevice(BluetoothDevice bluetoothDevice, int rssi) {
        super(bluetoothDevice.getName(), bluetoothDevice.getAddress());
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = rssi;
    }
}
