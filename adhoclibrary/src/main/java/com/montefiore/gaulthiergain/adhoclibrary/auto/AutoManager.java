package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.util.UtilBattery;
import com.montefiore.gaulthiergain.adhoclibrary.wifi.WifiManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutoManager {

    private final boolean v;
    private final Context context;
    private final String TAG = "[AdHoc][AutoManager]";
    private final ConcurrentHashMap<String, SmartDevice> smartDeviceHashMap;

    private BluetoothManager bluetoothManager;
    private WifiManager wifiManager;

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
        btDiscovery(duration);
        wifiDiscovery();
    }


    private void btDiscovery(int duration) {
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                bluetoothManager.discovery(new DiscoveryListener() {
                    @Override
                    public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                        Log.d(TAG, "onDiscoveryCompleted()");
                        for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
                            BluetoothAdHocDevice device = entry.getValue();
                            smartDeviceHashMap.put(device.getDevice().getAddress(),
                                    new SmartBluetoothDevice(device.getDevice(), device.getRssi()));
                            Log.d(TAG, "BLUETOOTH_DISCOVERY : " + entry.getValue().getDevice().getAddress());
                        }
                        bluetoothManager.unregisterDiscovery();
                    }

                    @Override
                    public void onDiscoveryStarted() {

                    }

                    @Override
                    public void onDeviceFound(BluetoothDevice device) {
                    }

                    @Override
                    public void onScanModeChange(int currentMode, int oldMode) {
                    }
                });
            }
        }).start();
    }

    private void wifiDiscovery() {
        try {
            wifiManager = new WifiManager(true, context);
            if (!wifiManager.isEnabled()) {
                wifiManager.enable();
            }

        } catch (DeviceException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                wifiManager.discover(new com.montefiore.gaulthiergain.adhoclibrary.wifi.DiscoveryListener() {
                    @Override
                    public void onDiscoveryStarted() {
                        Log.d(TAG, "WIFI START");
                    }

                    @Override
                    public void onDiscoveryFailed(int reasonCode) {
                        Log.d(TAG, "WIFI FAIL" + reasonCode);
                    }

                    @Override
                    public void onDiscoveryCompleted(HashMap<String, WifiP2pDevice> peers) {
                        for (Map.Entry<String, WifiP2pDevice> entry : peers.entrySet()) {
                            WifiP2pDevice device = entry.getValue();
                            smartDeviceHashMap.put(device.deviceAddress,
                                    new SmartWifiDevice(device, -1));
                            Log.d(TAG, "WIFI_DISCOVERY : " + entry.getValue().deviceAddress);
                        }
                    }
                });
            }
        }).start();
    }
}
