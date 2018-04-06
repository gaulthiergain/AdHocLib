package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class AutoTransferManager {

    private static final String TAG = "[AdHoc][AutoTransfer]";
    private static final String PREFIX = "[PEER]";
    private TransferManager transferManager;

    public AutoTransferManager(boolean verbose, Context context) {
        this.transferManager = new TransferManager(verbose, context, initListener());
    }

    public void stop() throws IOException, DeviceException {
        if (transferManager.isWifiEnable()) {
            transferManager.resetWifiAdapterName();
        }

        if (transferManager.isBluetoothEnable()) {
            transferManager.resetBluetoothAdapterName();
        }
        transferManager.stopListening();
    }

    public void start() throws IOException, DeviceException {
        transferManager.start();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                String name = transferManager.getBluetoothAdapterName();
                if (name!= null && !name.contains(PREFIX)) {
                    try {
                        transferManager.updateBluetoothAdapterName(PREFIX + name);
                    } catch (DeviceException e) {
                        e.printStackTrace();
                    }
                }

                //update Adapter Name
                name = transferManager.getWifiAdapterName();
                if (name!= null && !name.contains(PREFIX)) {
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
        }, 2000, 30000);
    }

    public Config getConfig() {
        return transferManager.getConfig();
    }

    private ListenerApp initListener() {
        return new ListenerApp() {
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

            }

            @Override
            public void traceException(Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onConnectionClosed(String remoteAddress, String remoteName) {
                Log.d(TAG, "Connection closed with ");
            }

            @Override
            public void onConnection(String remoteAddress, String remoteName, int hops) {
                Log.d(TAG, "Connection with " + remoteAddress + "(" + remoteName + ")");
            }

            @Override
            public void onConnectionFailed(String remoteName) {
                Log.d(TAG, "Connection failed " + remoteName + ")");
            }
        };
    }

    public void sendMessageTo(Object msg, String remoteDest) throws IOException {
        transferManager.sendMessageTo(msg, remoteDest);
    }
}
