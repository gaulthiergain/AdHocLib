package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.bluetooth.BluetoothDevice;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;

import java.util.HashMap;


public interface ListenerGUI {

    void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice);

    void onConnection(String deviceName, String deviceAddress, String localAddress);

    void onDiscoveryStarted();

    void onDeviceFound(BluetoothDevice device);

    void onScanModeChange(int currentMode, int oldMode);
}
