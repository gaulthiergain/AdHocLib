package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.content.Context;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
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

public class AutoTransferManager extends TransferManager {

    private static final String TAG = "[AdHoc][AutoTransfer]";
    public static final String PREFIX = "[PEER]";

    private static final int MIN_DELAY_TIME = 2000;
    private static final int MAX_DELAY_TIME = 5000;

    private final Set<String> connectedDevices;
    private final ListenerAutoApp listenerAutoApp;
    private Timer timer;

    private int elapseTimeMax;
    private int elapseTimeMin;

    public AutoTransferManager(boolean verbose, Context context, ListenerAutoApp listenerAutoApp) {
        super(verbose, context);
        setListenerApp(initListener());

        this.listenerAutoApp = listenerAutoApp;
        this.connectedDevices = new HashSet<>();
    }

    public AutoTransferManager(boolean verbose, Context context, Config config, ListenerAutoApp listenerAutoApp) {
        super(verbose, context, config);
        setListenerApp(initListener());

        this.listenerAutoApp = listenerAutoApp;
        this.connectedDevices = new HashSet<>();
    }

    public void stopDiscovery() {

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

                String name = getBluetoothAdapterName();
                if (name != null) {

                    for (AdHocDevice adHocDevice : getPairedDevices().values()) {
                        Log.d(TAG, "PAIRED: " + String.valueOf(adHocDevice));
                        if (!connectedDevices.contains(adHocDevice.getMacAddress())
                                && adHocDevice.getDeviceName().contains(PREFIX)) {
                            try {
                                connect(adHocDevice);
                            } catch (DeviceException e) {
                                e.printStackTrace();
                            } catch (DeviceAlreadyConnectedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (!name.contains(PREFIX)) {
                        try {
                            updateBluetoothAdapterName(PREFIX + name);
                        } catch (DeviceException e) {
                            e.printStackTrace();
                        }
                    }
                }

                //update Adapter Name
                name = getWifiAdapterName();
                if (name != null && !name.contains(PREFIX)) {
                    try {
                        updateWifiAdapterName(PREFIX + name);
                    } catch (DeviceException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    if (v) Log.d(TAG, "START new discovery");
                    discovery();

                    runDiscovery(waitRandomTime(elapseTimeMin, elapseTimeMax));
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

    public void startDiscovery(int elapseTimeMin, int elapseTimeMax) throws IOException {
        super.start();

        this.elapseTimeMin = elapseTimeMin;
        this.elapseTimeMax = elapseTimeMax;

        runDiscovery(waitRandomTime(MIN_DELAY_TIME, MAX_DELAY_TIME));
    }

    public void startDiscovery() throws IOException {
        super.start();

        this.elapseTimeMin = 20000;
        this.elapseTimeMax = 30000;

        runDiscovery(waitRandomTime(MIN_DELAY_TIME, MAX_DELAY_TIME));
    }


    private ListenerApp initListener() {
        return new ListenerApp() {
            @Override
            public void onDeviceDiscovered(AdHocDevice adHocDevice) {
                if (v) Log.d(TAG, "AdHoc device found " + adHocDevice.toString());
                if (adHocDevice.getType() == Service.BLUETOOTH &&
                        !connectedDevices.contains(adHocDevice.getMacAddress())
                        && adHocDevice.getDeviceName().contains(PREFIX)) {
                    try {
                        if (v) Log.d(TAG, "try to connect to " + adHocDevice);
                        connect(adHocDevice);
                    } catch (DeviceException e) {
                        e.printStackTrace();
                    } catch (DeviceAlreadyConnectedException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onDiscoveryStarted() {
                if (v) Log.d(TAG, "onDiscoveryStarted");
            }

            @Override
            public void onDiscoveryFailed(Exception e) {
                if (v) Log.d(TAG, "onDiscoveryFailed" + e.getMessage());
            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice) {
                for (final Map.Entry<String, AdHocDevice> entry : mapAddressDevice.entrySet()) {
                    AdHocDevice device = entry.getValue();
                    if (device.getType() == Service.WIFI &&
                            !connectedDevices.contains(device.getMacAddress())
                            && device.getDeviceName().contains(PREFIX)) {

                        if (v) Log.d(TAG, "Try to connect to " + device);
                        try {
                            Thread.sleep(waitRandomTime(MIN_DELAY_TIME, MAX_DELAY_TIME));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    connect(entry.getValue());
                                } catch (DeviceAlreadyConnectedException e) {
                                    e.printStackTrace();
                                } catch (DeviceException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }
            }

            @Override
            public void onConnectionClosed(AdHocDevice adHocDevice) {
                if (v)
                    Log.d(TAG, "Remove " + adHocDevice.getMacAddress() + " from connectedDevices set");
                connectedDevices.remove(adHocDevice.getMacAddress());
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
            public void onConnection(AdHocDevice adHocDevice) {
                if (v)
                    Log.d(TAG, "Add " + adHocDevice.getMacAddress() + " into connectedDevices set");
                connectedDevices.add(adHocDevice.getMacAddress());

                listenerAutoApp.onConnection(adHocDevice);
            }

            @Override
            public void onConnectionFailed(Exception e) {
                listenerAutoApp.onConnectionFailed(e);
            }
        };
    }

    public String getOwnName() {

        if (isWifiEnable()) {
            return getWifiAdapterName();
        }

        if (isBluetoothEnable()) {
            return getWifiAdapterName();
        }

        return null;
    }
}
