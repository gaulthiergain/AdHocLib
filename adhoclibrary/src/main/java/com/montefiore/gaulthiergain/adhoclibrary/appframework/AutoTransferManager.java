package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>This class provide high-Level APIs to manage automatically ad hoc networks and communications.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class AutoTransferManager extends TransferManager {

    private static final String TAG = "[AdHoc][AutoTransfer]";
    public static final String PREFIX = "[PEER]";

    private static final int MIN_DELAY_TIME = 2000;
    private static final int MAX_DELAY_TIME = 5000;

    private static final short INIT_STATE = 0;
    private static final short DISCOVERY_STATE = 1;
    private static final short CONNECT_STATE = 2;

    private final ListenerApp listenerApp;
    private final Set<String> connectedDevices;
    private final ArrayList<AdHocDevice> discoveredDevices;

    private short state;
    private Timer timer;
    private int elapseTimeMax;
    private int elapseTimeMin;
    private AdHocDevice currentDevice = null;

    /**
     * Constructor
     *
     * @param verbose     a boolean value to set the debug/verbose mode.
     * @param listenerApp a ListenerApp object which contains callback functions.
     */
    public AutoTransferManager(boolean verbose, ListenerApp listenerApp) {
        super(verbose);
        setListenerApp(initListener());

        this.listenerApp = listenerApp;
        this.connectedDevices = new HashSet<>();
        this.discoveredDevices = new ArrayList<>();
        this.state = INIT_STATE;
    }

    /**
     * Constructor
     *
     * @param verbose     a boolean value to set the debug/verbose mode.
     * @param config      a Config object which contains specific configurations.
     * @param listenerApp a ListenerApp object which contains callback functions.
     */
    public AutoTransferManager(boolean verbose, Config config, ListenerApp listenerApp) {
        super(verbose, config);
        setListenerApp(initListener());
        this.listenerApp = listenerApp;
        this.connectedDevices = new HashSet<>();
        this.discoveredDevices = new ArrayList<>();
        this.state = INIT_STATE;
    }

    /**
     * Method allowing to stop the discovery process.
     */
    public void stopDiscovery() {

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * Method allowing to launch a timer to execute the discovery mode.
     *
     * @param time an integer which represents the expiration of the timer.
     */
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

    /**
     * Method allowing to initialize the discovery listener.
     *
     * @return A DiscoveryListener object which contains callback methods.
     */
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
                        discoveredDevices.add(adHocDevice);
                    } else {
                        if (v) Log.d(TAG, "Found device " + adHocDevice.toString());
                    }
                }
                state = INIT_STATE;
            }
        };
    }

    /**
     * Method allowing to wait a random time.
     *
     * @param min an integer value which represents the lower bound (exclusive).  It must be positive.
     * @param max an integer value which represents the upper bound (exclusive).  It must be positive.
     * @return an integer value which represents the random time between min and max parameter.
     */
    private int waitRandomTime(int min, int max) {
        Random random = new Random();
        return random.nextInt(max - min + 1) + min;
    }

    /**
     * Method allowing to update the adapter name with a specific prefix.
     *
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
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

    /**
     * Method allowing to connect to a paired device (Bluetooth).
     */
    private void connectPairedDevices() {
        Map<String, AdHocDevice> paired = getPairedBluetoothDevices();
        if (paired != null) {
            for (AdHocDevice adHocDevice : getPairedBluetoothDevices().values()) {
                if (v) Log.d(TAG, "Paired devices: " + String.valueOf(adHocDevice));
                if (!connectedDevices.contains(adHocDevice.getMacAddress())
                        && adHocDevice.getDeviceName().contains(PREFIX)) {
                    discoveredDevices.add(adHocDevice);
                }
            }
        }
    }

    /**
     * Method allowing to start the discovery process every x seconds where x is a random value
     * between elapseTimeMin and elapseTimeMax in milliseconds.
     *
     * @param elapseTimeMin an integer value which represents the lower bound (exclusive).  It must be positive.
     * @param elapseTimeMax an integer value which represents the upper bound (exclusive).  It must be positive.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
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

    /**
     * Method allowing to start the discovery process every x seconds where x is a random value
     * between elapseTimeMin and elapseTimeMax in milliseconds.
     *
     * @param elapseTimeMin an integer value which represents the lower bound (exclusive).  It must be positive.
     * @param elapseTimeMax an integer value which represents the upper bound (exclusive).  It must be positive.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void startDiscovery(int elapseTimeMin, int elapseTimeMax) throws DeviceException {
        _startDiscovery(elapseTimeMin, elapseTimeMax);
    }

    /**
     * Method allowing to start the discovery process every x seconds where x is a random value
     * between 50 and 60.
     *
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void startDiscovery() throws DeviceException {
        _startDiscovery(50000, 60000);
    }

    /**
     * Method allowing to initialize the ListenerApp listener.
     *
     * @return a ListenerApp object which contains callback functions.
     */
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
            public void onForwardData(AdHocDevice adHocDevice, Object pdu) {
                listenerApp.onForwardData(adHocDevice, pdu);
            }

            @Override
            public void onConnection(AdHocDevice adHocDevice) {

                if (v)
                    Log.d(TAG, "Add " + adHocDevice.getMacAddress() + " into connectedDevices set");

                // Add the device into the connected devices list
                connectedDevices.add(adHocDevice.getMacAddress());

                listenerApp.onConnection(adHocDevice);

                // Remove device from the discovered devices list
                discoveredDevices.remove(adHocDevice);

                // Update state
                state = INIT_STATE;
            }

            @Override
            public void onConnectionFailed(Exception e) {

                listenerApp.onConnectionFailed(e);

                // Remove device from the discovered devices list
                discoveredDevices.remove(currentDevice);

                // Update state
                state = INIT_STATE;
            }
        };
    }

    /**
     * Method allowing to launch a timer for outgoing connection(s).
     */
    private void timerConnect() {
        Timer timerConnect = new Timer();
        timerConnect.schedule(new TimerTask() {
            @Override
            public void run() {
                for (AdHocDevice device : discoveredDevices) {
                    if (state == INIT_STATE) {
                        try {
                            if (v) Log.d(TAG, "Try to connect to " + device.toString());
                            currentDevice = device;
                            state = CONNECT_STATE;
                            connect(device);
                        } catch (DeviceException e) {
                            e.printStackTrace();
                            state = INIT_STATE;
                            discoveredDevices.remove(device);
                        }
                    } else {
                        if (v) Log.d(TAG, "Unable to connect " + getStateString());
                    }
                }
            }
        }, MIN_DELAY_TIME, MAX_DELAY_TIME);
    }

    /**
     * Method allowing to get the current state as a String value.
     *
     * @return a String value which represents the current state.
     */
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

    /**
     * Method allowing to reset the adapter name (Wi-Fi and Bluetooth) of the current device.
     *
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
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
