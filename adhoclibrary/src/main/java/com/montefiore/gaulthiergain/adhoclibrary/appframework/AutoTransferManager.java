package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.DeviceAlreadyConnectedException;

import java.util.ArrayList;
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

    private static final short INIT_STATE = 0;
    private static final short DISCOVERY_STATE = 1;
    private static final short CONNECT_STATE = 2;

    private final Set<String> connectedDevices;
    private final ListenerAutoApp listenerAutoApp;
    private Timer timer;

    private int elapseTimeMax;
    private int elapseTimeMin;

    private ArrayList<AdHocDevice> arrayList;

    private short state;
    private AdHocDevice currentDevice = null;

    public AutoTransferManager(boolean verbose, ListenerAutoApp listenerAutoApp) {
        super(verbose);
        this.config.setAttemps(1);
        setListenerApp(initListener());

        this.listenerAutoApp = listenerAutoApp;
        this.connectedDevices = new HashSet<>();
        this.arrayList = new ArrayList<>();
        this.state = INIT_STATE;
    }

    public AutoTransferManager(boolean verbose, Config config, ListenerAutoApp listenerAutoApp) {
        super(verbose, config);
        this.config.setAttemps(1);
        setListenerApp(initListener());

        this.listenerAutoApp = listenerAutoApp;
        this.connectedDevices = new HashSet<>();
        this.arrayList = new ArrayList<>();
        this.state = INIT_STATE;
    }

    public void stopDiscovery() {

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void timerDiscovery(int time) {

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                if (state == INIT_STATE) {

                    try {
                        if (v) Log.d(TAG, "START new discovery");
                        discovery();
                        timerDiscovery(waitRandomTime(elapseTimeMin, elapseTimeMax));
                    } catch (DeviceException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(TAG, "Unable to discovery " + getStateString());
                    timerDiscovery(waitRandomTime(elapseTimeMin, elapseTimeMax));
                }
            }
        }, time);
    }

    private int waitRandomTime(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    private void updateAdapterName() {
        String name = getBluetoothAdapterName();
        if (name != null) {
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
    }

    private void connectPairedDevices() {
        HashMap<String, AdHocDevice> paired = getPairedDevices();
        if (paired != null) {
            for (AdHocDevice adHocDevice : getPairedDevices().values()) {
                if (v) Log.d(TAG, "Paired devices: " + String.valueOf(adHocDevice));
                if (!connectedDevices.contains(adHocDevice.getMacAddress())
                        && adHocDevice.getDeviceName().contains(PREFIX)) {
                    arrayList.add(adHocDevice);
                }
            }
        }
    }

    private void _startDiscovery(int elapseTimeMin, int elapseTimeMax) {

        this.elapseTimeMin = elapseTimeMin;
        this.elapseTimeMax = elapseTimeMax;

        // Update adapter name if necessary
        updateAdapterName();

        // Get all paired devices (bluetooth only)
        connectPairedDevices();

        // Run discovery process
        timerDiscovery(waitRandomTime(MIN_DELAY_TIME, MAX_DELAY_TIME));

        // Run connection process
        timerConnect();
    }

    public void startDiscovery(int elapseTimeMin, int elapseTimeMax) {
        _startDiscovery(elapseTimeMin, elapseTimeMax);
    }

    public void startDiscovery() {
        _startDiscovery(50000, 60000);
    }

    private ListenerApp initListener() {
        return new ListenerApp() {
            @Override
            public void onDeviceDiscovered(AdHocDevice adHocDevice) {
                //Ignored
            }

            @Override
            public void onDiscoveryStarted() {
                if (v) Log.d(TAG, "onDiscoveryStarted");
                state = DISCOVERY_STATE;
            }

            @Override
            public void onDiscoveryFailed(Exception e) {
                if (v) Log.d(TAG, "onDiscoveryFailed" + e.getMessage());
                state = INIT_STATE;
            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice) {

                for (final Map.Entry<String, AdHocDevice> entry : mapAddressDevice.entrySet()) {
                    AdHocDevice adHocDevice = entry.getValue();

                    if (adHocDevice.getDeviceName().contains(PREFIX)
                            && !connectedDevices.contains(adHocDevice.getMacAddress())) {

                        if (v) Log.d(TAG, "Add device " + adHocDevice.toString());
                        arrayList.add(adHocDevice);
                    } else {
                        if (v) Log.d(TAG, "Found device " + adHocDevice.toString());
                    }
                }
                state = INIT_STATE;
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

                arrayList.remove(adHocDevice);

                state = INIT_STATE;
            }

            @Override
            public void onConnectionFailed(Exception e) {
                listenerAutoApp.onConnectionFailed(e);

                arrayList.remove(currentDevice);

                state = INIT_STATE;
            }
        };
    }

    private void timerConnect() {
        Timer timerConnect = new Timer();
        timerConnect.schedule(new TimerTask() {
            @Override
            public void run() {
                for (AdHocDevice device : arrayList) {
                    if (state == INIT_STATE) {
                        try {
                            if (v) Log.d(TAG, "Try to connect to " + device.toString());
                            currentDevice = device;
                            state = CONNECT_STATE;
                            connect(device);
                        } catch (DeviceException e) {
                            e.printStackTrace();
                            state = INIT_STATE;
                            arrayList.remove(device);
                        } catch (DeviceAlreadyConnectedException e) {
                            e.printStackTrace();
                            state = INIT_STATE;
                            arrayList.remove(device);
                        }
                    } else {
                        if (v) Log.d(TAG, "Unable to connect " + getStateString());
                    }
                }
            }
        }, MIN_DELAY_TIME, MAX_DELAY_TIME);
    }

    private String getStateString() {
        switch (state) {
            case DISCOVERY_STATE:
                return "DISCOVERY";
            case CONNECT_STATE:
                return "CONNECTING";
            case INIT_STATE:
            default:
                return "INIT";

        }
    }
}
