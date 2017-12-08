package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.bluetooth.BluetoothDevice;

import java.util.HashMap;

/**
 * <p>This interface allows to define callback functions for the bluetooth discovery process.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface DiscoveryListener {
    /**
     * Callback when the discovery is finished.
     *
     * @param hashMapBluetoothDevice a HashMap<String, BluetoothAdHocDevice> which contains a
     *                               mapping between the name of a device and a
     *                               {@link BluetoothAdHocDevice} object.
     */
    void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice);

    /**
     * Callback when the discovery started.
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
