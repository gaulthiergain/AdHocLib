package com.montefiore.gaulthiergain.adhoclibrary.auto;


import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.DiscoveryListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadDiscoveryBluetooth extends Thread {

    private static final String TAG = "[AdHoc]";

    private final BluetoothManager bluetoothManager;
    private final DiscoveryListener discoveryListener;

    public ThreadDiscoveryBluetooth(BluetoothManager bluetoothManager,
                                   DiscoveryListener discoveryListener) {
        this.bluetoothManager = bluetoothManager;
        this.discoveryListener = discoveryListener;
    }

    @Override
    public void run() {
        bluetoothManager.discovery(discoveryListener);
    }
}
