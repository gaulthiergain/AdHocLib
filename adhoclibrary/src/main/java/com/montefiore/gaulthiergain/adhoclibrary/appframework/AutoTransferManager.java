package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.DeviceAlreadyConnectedException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class AutoTransferManager {

    private static final String TAG = "[AdHoc][AutoTransfer]";
    private static final String PREFIX = "[PEER]";

    private final Set<String> connectedDevices;
    private final TransferManager transferManager;
    private final ListenerAutoApp listenerAutoApp;
    private Timer timer;

    public AutoTransferManager(boolean verbose, Context context, ListenerAutoApp listenerAutoApp) {
        this.listenerAutoApp = listenerAutoApp;
        this.transferManager = new TransferManager(verbose, context, initListener());
        this.connectedDevices = new HashSet<>();
    }

    public void stop() throws IOException, DeviceException {

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (transferManager.isWifiEnable()) {
            transferManager.resetWifiAdapterName();
        }

        if (transferManager.isBluetoothEnable()) {
            transferManager.resetBluetoothAdapterName();
        }
        transferManager.stopListening();
    }

    public void start() throws IOException {
        transferManager.start();

        for (AdHocDevice device : transferManager.getPairedDevices().values()) {
            Log.d(TAG, "PAIRED: " + String.valueOf(device));
            connect(device);
        }

        /*timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                String name = transferManager.getBluetoothAdapterName();
                if (name != null && !name.contains(PREFIX)) {
                    try {
                        transferManager.updateBluetoothAdapterName(PREFIX + name);
                        for (AdHocDevice device : transferManager.getPairedDevices().values()) {
                            Log.d(TAG, "PAIRED: " + String.valueOf(device));
                            connect(device);
                        }
                    } catch (DeviceException e) {
                        e.printStackTrace();
                    }
                }

                //update Adapter Name
                name = transferManager.getWifiAdapterName();
                if (name != null && !name.contains(PREFIX)) {
                    try {
                        transferManager.updateWifiAdapterName(PREFIX + name);
                    } catch (DeviceException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Log.d(TAG, "START NEW DISCOVERY");
                    transferManager.discovery();
                } catch (DeviceException e) {
                    e.printStackTrace();
                }
            }
        }, 2000, 30000);*/
    }

    public Config getConfig() {
        return transferManager.getConfig();
    }

    private ListenerApp initListener() {
        return new ListenerApp() {
            @Override
            public void onDeviceDiscovered(AdHocDevice device) {
                connect(device);
            }

            @Override
            public void onDiscoveryStarted() {
                Log.d(TAG, "onDiscoveryStarted");
            }

            @Override
            public void onDiscoveryFailed(String s) {
                Log.d(TAG, "onDiscoveryFailed");
            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice) {
                for (Map.Entry<String, AdHocDevice> entry : mapAddressDevice.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    Log.d(TAG, "Discovered Completed " + key + " - " + value.toString());
                }
            }

            @Override
            public void onReceivedData(String senderName, String senderAddress, Object pdu) {
                listenerAutoApp.onReceivedData(senderName, senderAddress, pdu);
            }

            @Override
            public void traceException(Exception e) {
                listenerAutoApp.traceException(e);
            }

            @Override
            public void onConnectionClosed(String remoteAddress, String remoteName) {
                connectedDevices.remove(remoteName);
                listenerAutoApp.onConnectionClosed(remoteAddress, remoteName);
            }

            @Override
            public void onConnection(String remoteAddress, String remoteName, int hops) {
                connectedDevices.add(remoteName);
                listenerAutoApp.onConnection(remoteAddress, remoteName, hops);
            }

            @Override
            public void onConnectionFailed(String remoteName) {
                listenerAutoApp.onConnectionFailed(remoteName);
            }
        };
    }

    private void connect(AdHocDevice device) {
        Log.d(TAG, "onDeviceDiscovered " + device);
        if (device.getDeviceName().contains(PREFIX) && !connectedDevices.contains(device.getDeviceName())) {
            try {
                transferManager.connect(device);
            } catch (DeviceException e) {
                e.printStackTrace();
            } catch (DeviceAlreadyConnectedException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "Already connected to " + device.getDeviceName());
        }
    }

    public void sendMessageTo(Object msg, String remoteDest) throws IOException {
        transferManager.sendMessageTo(msg, remoteDest);
    }

    public void broadcast(Object msg) throws IOException {
        transferManager.broadcast(msg);
    }

    public void broadcastExcept(Object msg, String excludedAddress) throws IOException {
        transferManager.broadcastExcept(msg, excludedAddress);
    }
}
