package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.content.Context;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.DeviceAlreadyConnectedException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class AutoTransferManager {

    private static final String TAG = "[AdHoc][AutoTransfer]";
    public static final String PREFIX = "[PEER]";

    private final Set<String> connectedDevices;
    private final TransferManager transferManager;
    private final ListenerAutoApp listenerAutoApp;
    private Timer timer;

    //private WifiAdHocManager wifiAdHocManager;

    public AutoTransferManager(boolean verbose, Context context, ListenerAutoApp listenerAutoApp) {
        this.listenerAutoApp = listenerAutoApp;
        this.transferManager = new TransferManager(verbose, context, initListener());
        this.connectedDevices = new HashSet<>();

        /*try {
            wifiAdHocManager = new WifiAdHocManager(true, context, null);
        } catch (DeviceException e) {
            e.printStackTrace();
        }*/
    }

    public void cancel() throws IOException {
        transferManager.stopListening();
    }

    public void stopDiscovery() throws IOException {

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void runDiscovery(int time) {
        timer = new Timer();
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

                    runDiscovery(waitRandomTime(20000, 30000));
                } catch (DeviceException e) {
                    e.printStackTrace();
                }
            }
        }, time);
    }

    private int waitRandomTime(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }


    public void start() throws IOException {
        transferManager.start();


        /*timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                wifiAdHocManager.discoverService();
            }
        }, 2000, 10000);*/


        runDiscovery(waitRandomTime(2000, 5000));


    }

    public Config getConfig() {
        return transferManager.getConfig();
    }

    private ListenerApp initListener() {
        return new ListenerApp() {
            @Override
            public void onDeviceDiscovered(AdHocDevice device) {
                //ignored
            }

            @Override
            public void onDiscoveryStarted() {
                Log.d(TAG, "onDiscoveryStarted");
            }

            @Override
            public void onDiscoveryFailed(Exception e) {
                Log.d(TAG, "onDiscoveryFailed" + e.getMessage());
            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice) {
                for (final Map.Entry<String, AdHocDevice> entry : mapAddressDevice.entrySet()) {
                    if (entry.getValue().getDeviceName().contains(PREFIX)) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                connect(entry.getValue());
                                try {
                                    Thread.sleep(waitRandomTime(2000, 5000));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }
            }

            @Override
            public void onConnectionClosed(AdHocDevice adHocDevice) {
                connectedDevices.remove(adHocDevice.getDeviceName());
                listenerAutoApp.onConnectionClosed(adHocDevice);
            }

            @Override
            public void onConnectionClosedFailed(Exception e) {
                listenerAutoApp.onConnectionClosedFailed(e);
            }

            @Override
            public void processMsgException(Exception e) {
                listenerAutoApp.processMsgException(e);
            }

            @Override
            public void onReceivedData(AdHocDevice adHocDevice, Object pdu) {
                listenerAutoApp.onReceivedData(adHocDevice, pdu);
            }

            @Override
            public void onConnection(AdHocDevice remoteDevice) {
                Log.d(TAG, "ADD " + remoteDevice.getDeviceName() + "into set");
                connectedDevices.add(remoteDevice.getDeviceName());
                listenerAutoApp.onConnection(remoteDevice);
            }

            @Override
            public void onConnectionFailed(Exception e) {
                listenerAutoApp.onConnectionFailed(e);
            }
        };
    }

    private void connect(final AdHocDevice device) {


        if (!connectedDevices.contains(device.getDeviceName())) {
            try {
                Log.d(TAG, "try to connect to " + device);
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

    public void sendMessageTo(Object msg, AdHocDevice adHocDevice) throws IOException {
        transferManager.sendMessageTo(msg, adHocDevice);
    }

    public void broadcast(Object msg) throws IOException {
        //wifiAdHocManager.startRegistration();

        transferManager.broadcast(msg);
    }

    public void broadcastExcept(Object msg, AdHocDevice adHocDevice) throws IOException {
        transferManager.broadcastExcept(msg, adHocDevice);
    }


    public String getName() {
        return transferManager.getOwnName();
    }
}
