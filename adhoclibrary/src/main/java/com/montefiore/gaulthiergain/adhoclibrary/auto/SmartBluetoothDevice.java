package com.montefiore.gaulthiergain.adhoclibrary.auto;


import android.bluetooth.BluetoothDevice;

public class SmartBluetoothDevice extends SmartDevice {
    private BluetoothDevice bluetoothDevice;
    private int rssi;
    private String uuid;

    public SmartBluetoothDevice(BluetoothDevice bluetoothDevice, int rssi, String uuid) {
        super(bluetoothDevice.getName(), bluetoothDevice.getAddress());
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = rssi;
        this.uuid = uuid;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public int getRssi() {
        return rssi;
    }

    public String getUuid() {
        return uuid;
    }
}
