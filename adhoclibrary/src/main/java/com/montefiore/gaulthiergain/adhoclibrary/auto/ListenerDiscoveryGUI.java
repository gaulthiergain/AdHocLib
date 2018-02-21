package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.bluetooth.BluetoothDevice;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;

import java.util.HashMap;


public interface ListenerDiscoveryGUI {

    /**
     * Callback when the discovery is finished.
     *
     * @param hashMapBluetoothDevice a HashMap<String, BluetoothAdHocDevice> which contains a
     *                               mapping between the name of a device and a
     *                               {@link BluetoothAdHocDevice} object.
     */
    void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice);

    /**
     * Callback when connection is performed.
     *
     * @param deviceName    a String value which represents the remote device's name.
     * @param deviceAddress a String value which represents the remote device's address.
     * @param localAddress  a String value which represents the local device's address.
     */
    void onConnection(String deviceName, String deviceAddress, String localAddress);

    /**
     * Callback when the discovery is started.
     */
    void onDiscoveryStarted();

    /**
     * Callback when a device is found.
     *
     * @param device a BluetoothDevice object which represents a new found device.
     */
    void onDeviceFound(BluetoothDevice device);

    /**
     * Callback when the mode is changed.
     *
     * @param currentMode an integer value which represents the current status of the mode.
     * @param oldMode     an integer value which represents the old status of the mode.
     */
    void onScanModeChange(int currentMode, int oldMode);
}
