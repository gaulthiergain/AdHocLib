package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.util.UtilBattery;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutoManager {

    private final boolean v;
    private final Context context;
    private final String TAG = "[AdHoc][AutoManager]";
    private final ConcurrentHashMap<String, SmartDevice> smartDeviceHashMap;

    private BluetoothManager bluetoothManager;
    private boolean bluetooth_support;

    public AutoManager(boolean verbose, Context context) {
        this.v = verbose;
        this.context = context;
        this.smartDeviceHashMap = new ConcurrentHashMap<>();
    }

    public int computeMagicNumber() {
        // 1. Get Battery Level
        int batteryLevel = UtilBattery.getBatteryPercentage(context);
        if (v) Log.d(TAG, "Battery Level" + batteryLevel);
        // 2. Get Spec
        Log.d(TAG, "ManuFacturer :" + Build.MANUFACTURER);
        Log.d(TAG, "Board : " + Build.BOARD);
        Log.d(TAG, "Display : " + Build.DISPLAY);
        // 3. Get number of discoverable devices


        return 1;
    }

    public void discovery(int duration) {

        try {
            bluetoothManager = new BluetoothManager(true, context);
            if (!bluetoothManager.isEnabled()) {
                // Enable Bluetooth and enable the discovery
                bluetoothManager.enable();
                bluetoothManager.enableDiscovery(duration);

            }
            // Bluetooth is supported on this device
            bluetooth_support = true;
        } catch (DeviceException e1) {
            e1.printStackTrace();
            bluetooth_support = false;
        } catch (BluetoothBadDuration e2) {
            e2.printStackTrace();
        }

        ThreadDiscoveryBluetooth threadDiscoveryBluetooth =
                new ThreadDiscoveryBluetooth(bluetoothManager, new DiscoveryListener() {
                    @Override
                    public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                        Log.d(TAG, "onDiscoveryCompleted()");
                        for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
                            BluetoothAdHocDevice device = entry.getValue();
                            smartDeviceHashMap.put(device.getDevice().getAddress(),
                                    new SmartBluetoothDevice(device.getDevice(), device.getRssi()));
                            Log.d(TAG, "DISCOVERY : " + entry.getValue().getDevice().getAddress());
                        }
                        bluetoothManager.unregisterDiscovery();
                    }

                    @Override
                    public void onDiscoveryStarted() {
                        Log.d(TAG, "onDiscoveryStarted()");
                    }

                    @Override
                    public void onDeviceFound(BluetoothDevice device) {
                        Log.d(TAG, "onDeviceFound()");
                    }

                    @Override
                    public void onScanModeChange(int currentMode, int oldMode) {
                        Log.d(TAG, "onScanModeChange()");
                    }
                });
        threadDiscoveryBluetooth.start();
        

    }


}
