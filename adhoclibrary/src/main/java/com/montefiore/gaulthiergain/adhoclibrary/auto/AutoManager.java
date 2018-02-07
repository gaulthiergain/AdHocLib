package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.util.Log;
import android.widget.ActionMenuView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothPeer;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.util.UtilBattery;
import com.montefiore.gaulthiergain.adhoclibrary.wifi.WifiManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoManager {

    private final boolean v;
    private final Context context;
    private final String TAG = "[AdHoc][AutoManager]";
    private final ConcurrentHashMap<String, SmartBluetoothDevice> smartDeviceHashMap;

    private BluetoothManager bluetoothManager;
    private WifiManager wifiManager;

    private boolean bluetooth_support;


    //todo remove
    private int type;
    public final static int TYPE_FULL_MESH = 0;
    public final static int TYPE_RANDOM_GROUP = 1;
    public final static int TYPE_COVER_CHANNEL_GROUP = 2;

    public ListenerGUI listenerGUI;

    public AutoManager(boolean verbose, Context context) throws DeviceException {
        this.v = verbose;
        this.context = context;
        this.smartDeviceHashMap = new ConcurrentHashMap<>();
        this.bluetoothManager = new BluetoothManager(true, context);
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setListenerGUI(ListenerGUI listenerGUI) {
        this.listenerGUI = listenerGUI;
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
        //wifiDiscovery();
    }


    private void discoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
        boolean conn = false;

        switch (type) {
            case TYPE_FULL_MESH:
                conn = true;
                break;
            case TYPE_RANDOM_GROUP:
                break;
            case TYPE_COVER_CHANNEL_GROUP:
                break;
        }

        Log.d(TAG, "onDiscoveryCompleted()");
        for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
            BluetoothAdHocDevice device = entry.getValue();
            Log.d(TAG, "BLUETOOTH_DISCOVERY: " + device.getDevice().getName());

            if (device.getDevice().getName().contains(Code.ID_APP)) {
                if (conn) {
                    connect(false, device);
                }
            }
        }
        bluetoothManager.unregisterDiscovery();
    }

    private void btDiscovery(int duration) {
        try {
            if (!bluetoothManager.isEnabled()) {
                // Enable Bluetooth and enable the discovery
                bluetoothManager.enable();
                bluetoothManager.enableDiscovery(duration);

            }
            // Bluetooth is supported on this device
            bluetooth_support = true;
        } catch (BluetoothBadDuration e2) {
            e2.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                bluetoothManager.discovery(new DiscoveryListener() {
                    @Override
                    public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                        discoveryCompleted(hashMapBluetoothDevice);
                    }

                    @Override
                    public void onDiscoveryStarted() {
                        switch (type) {
                            case TYPE_FULL_MESH:
                                listen();
                                break;
                            case TYPE_RANDOM_GROUP:
                                break;
                            case TYPE_COVER_CHANNEL_GROUP:
                                break;
                        }
                    }

                    @Override
                    public void onDeviceFound(BluetoothDevice device) {
                        listenerGUI.deviceDiscover(device.getName(), device.getAddress());
                    }

                    @Override
                    public void onScanModeChange(int currentMode, int oldMode) {
                    }
                });
            }
        }).start();
    }

    private void listen() {
        BluetoothServiceServer bluetoothServiceServer = new BluetoothServiceServer(true, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {

            }

            @Override
            public void onMessageSent(MessageAdHoc message) {

            }

            @Override
            public void onForward(MessageAdHoc message) {

            }

            @Override
            public void onConnectionClosed(String deviceName, String deviceAddress) {

            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                //Toast.makeText(context, "Connected with " + deviceName, Toast.LENGTH_LONG).show();
                listenerGUI.deviceConnection(deviceName, deviceAddress);
            }
        });
        try {
            bluetoothServiceServer.listen(8, false, "test", bluetoothManager.getBluetoothAdapter(),
                    UUID.fromString(BluetoothUtil.UUID + BluetoothUtil.getCurrentMac(context).replace(":", "")));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                            /*smartDeviceHashMap.put(device.deviceAddress,
                                    new SmartWifiDevice(device, -1));
                            Log.d(TAG, "WIFI_DISCOVERY : " + entry.getValue().deviceAddress);*/
                        }
                    }
                });
            }
        }).start();
    }


    public void connect(final boolean secure, final BluetoothAdHocDevice bluetoothAdHocDevice) {

        BluetoothServiceClient bluetoothServiceClient = new BluetoothServiceClient(true, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {

            }

            @Override
            public void onMessageSent(MessageAdHoc message) {

            }

            @Override
            public void onForward(MessageAdHoc message) {

            }

            @Override
            public void onConnectionClosed(String deviceName, String deviceAddress) {

            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                listenerGUI.deviceConnection(deviceName, deviceAddress);
            }
        }, false, secure, 3, bluetoothAdHocDevice);
        new Thread(bluetoothServiceClient).start();
    }

    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public SmartBluetoothDevice getSmartDeviceByAddr(String addr) {

        if (smartDeviceHashMap.containsKey(addr)) {
            return smartDeviceHashMap.get(addr);
        }

        return null;
    }
}
