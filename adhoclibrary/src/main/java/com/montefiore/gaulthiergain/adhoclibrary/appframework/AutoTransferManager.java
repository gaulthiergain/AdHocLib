package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
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
    private final ListenerApp listenerApp;
    private Timer timer;

    private int elapseTimeMax;
    private int elapseTimeMin;

    private ArrayList<AdHocDevice> arrayList;

    private short state;
    private AdHocDevice currentDevice = null;

    public AutoTransferManager(boolean verbose, ListenerApp listenerApp) {
        super(verbose);
        setListenerApp(initListener());

        this.listenerApp = listenerApp;
        this.connectedDevices = new HashSet<>();
        this.arrayList = new ArrayList<>();
        this.state = INIT_STATE;
    }

    public AutoTransferManager(boolean verbose, Config config, ListenerApp listenerApp) {
        super(verbose, config);
        setListenerApp(initListener());
        this.listenerApp = listenerApp;
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
                        discovery(initListenerDiscovery());
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

    private DiscoveryListener initListenerDiscovery() {
        return new DiscoveryListener() {
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
        };
    }

    private int waitRandomTime(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    private void updateAdapterName() throws DeviceException {
        String name = getBluetoothAdapterName();
        if (name != null) {
            if (!name.contains(PREFIX)) {
                updateBluetoothAdapterName(PREFIX + name);
            }
        }

        //update Adapter Name
        name = getWifiAdapterName();
        if (name != null && !name.contains(PREFIX)) {
            updateWifiAdapterName(PREFIX + name);
        }
    }

    private void connectPairedDevices() {
        Map<String, AdHocDevice> paired = getPairedDevices();
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

    private void _startDiscovery(int elapseTimeMin, int elapseTimeMax) throws DeviceException {

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

    public void startDiscovery(int elapseTimeMin, int elapseTimeMax) throws DeviceException {
        _startDiscovery(elapseTimeMin, elapseTimeMax);
    }

    public void startDiscovery() throws DeviceException {
        _startDiscovery(50000, 60000);
    }

    private ListenerApp initListener() {
        return new ListenerApp() {


            @Override
            public void onConnectionClosed(AdHocDevice adHocDevice) {
                if (v)
                    Log.d(TAG, "Remove " + adHocDevice.getMacAddress() + " from connectedDevices set");
                connectedDevices.remove(adHocDevice.getMacAddress());
                listenerApp.onConnectionClosed(adHocDevice);
            }

            @Override
            public void onConnectionClosedFailed(Exception e) {
                listenerApp.onConnectionClosedFailed(e);
            }

            @Override
            public void processMsgException(Exception e) {
                listenerApp.processMsgException(e);
            }

            @Override
            public void onReceivedData(AdHocDevice adHocDevice, Object pdu) {
                listenerApp.onReceivedData(adHocDevice, pdu);
            }

            @Override
            public void onConnection(AdHocDevice adHocDevice) {

                if (v)
                    Log.d(TAG, "Add " + adHocDevice.getMacAddress() + " into connectedDevices set");

                connectedDevices.add(adHocDevice.getMacAddress());

                listenerApp.onConnection(adHocDevice);

                arrayList.remove(adHocDevice);

                state = INIT_STATE;
            }

            @Override
            public void onConnectionFailed(Exception e) {
                listenerApp.onConnectionFailed(e);

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

    public void reset() throws DeviceException {
        String name = getBluetoothAdapterName();
        if (name != null && name.contains(PREFIX)) {
            resetBluetoothAdapterName();
        }

        //update Adapter Name
        name = getWifiAdapterName();
        if (name != null && name.contains(PREFIX)) {
            resetWifiAdapterName();
        }
    }
}
