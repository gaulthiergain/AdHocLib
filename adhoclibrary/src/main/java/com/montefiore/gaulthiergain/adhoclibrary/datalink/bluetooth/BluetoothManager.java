package com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by gaulthiergain on 25/10/17.
 * Manage the Bluetooth discovery and the peering with other bluetooth devices.
 */

public class BluetoothManager {

    private final boolean v;
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final String TAG = "[AdHoc][Blue.Manager]";
    private final HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice;

    private String initName;
    private boolean registered = false;
    private DiscoveryListener discoveryListener;

    /**
     * Constructor
     *
     * @param verbose a boolean value to set the debug/verbose mode.
     * @param context a Context object which gives global information about an application
     *                environment.
     * @throws DeviceException Signals that a Bluetooth Device Exception exception
     *                         has occurred.
     */
    public BluetoothManager(boolean verbose, Context context)
            throws DeviceException {

        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            throw new DeviceException("Error device does not support Bluetooth");
        } else {
            // Device supports Bluetooth
            this.v = verbose;
            this.context = context;
            this.hashMapBluetoothDevice = new HashMap<>();
        }
    }

    /**
     * Method allowing to check if the Bluetooth adapter is enabled.
     *
     * @return a boolean value which represents the status of the bluetooth adapter.
     */
    public boolean isEnabled() {
        return bluetoothAdapter.isEnabled();
    }

    /**
     * Method allowing to enable the Bluetooth adapter.
     *
     * @return a boolean value which represents the status of the operation.
     */
    public boolean enable() {
        return bluetoothAdapter.enable();
    }

    /**
     * Method allowing to disable the Bluetooth adapter.
     *
     * @return a boolean value which represents the status of the operation.
     */
    public boolean disable() {
        return bluetoothAdapter.disable();
    }

    /**
     * Method allowing to get all the paired Bluetooth devices.
     *
     * @return a HashMap<String, BluetoothAdHocDevice> that maps the device's name with
     * BluetoothAdHocDevice object.
     */
    public HashMap<String, BluetoothAdHocDevice> getPairedDevices() {
        if (v) Log.d(TAG, "getPairedDevices()");

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        HashMap<String, BluetoothAdHocDevice> hashMapBluetoothPairedDevice = new HashMap<>();

        if (pairedDevices.size() > 0) {
            // Add each paired device into a hashMap
            for (BluetoothDevice device : pairedDevices) {
                if (v) Log.d(TAG, "DeviceName: " + device.getName() +
                        " - DeviceHardwareAddress: " + device.getAddress());
                hashMapBluetoothPairedDevice.put(device.getAddress(),
                        new BluetoothAdHocDevice(device));
            }
        }
        return hashMapBluetoothPairedDevice;
    }

    public void updateDeviceName(String name) {

        if (initName == null) {
            initName = bluetoothAdapter.getName();
        }

        if(initName.contains("#e091#")){
            if (initName.split("#").length > 2) {
                initName = initName.split("#")[2];
            }
        }

        if (v) Log.i(TAG, "localdevicename : " + bluetoothAdapter.getName());
        bluetoothAdapter.setName(name + initName);
        if (v) Log.i(TAG, "localdevicename : " + name + initName);
    }

    public void resetDeviceName() throws DeviceException {
        if (initName != null && !initName.contains("#e091#")) {
            bluetoothAdapter.setName(initName);
        } else if (initName != null && initName.contains("#e091#")) {
            if (initName.split("#").length > 2) {
                bluetoothAdapter.setName(initName.split("#")[2]);
            } else {
                throw new DeviceException("No initial name found");
            }
        }
    }

    /**
     * Method allowing to discover other bluetooth devices.
     *
     * @param discoveryListener a discoveryListener object which serves as callback functions.
     */
    public void discovery(DiscoveryListener discoveryListener) {
        if (v) Log.d(TAG, "discovery()");

        // Check if the device is already "discovering". If it is, then cancel discovery.
        cancelDiscovery();

        this.discoveryListener = discoveryListener;

        // Start Discovery
        bluetoothAdapter.startDiscovery();

        // Set Register to true
        registered = true;

        // Register for broadcasts when a device is discovered.
        context.getApplicationContext().registerReceiver(mReceiver, new IntentFilter(
                BluetoothDevice.ACTION_FOUND));
        context.getApplicationContext().registerReceiver(mReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        context.getApplicationContext().registerReceiver(mReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        // Register for broadcasts when a device changes its mode
        context.getApplicationContext().registerReceiver(mReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
    }

    /**
     * Method allowing to unregister the discovery broadcast.
     *
     * @throws IllegalArgumentException Signals that a method has been passed an illegal or
     *                                  inappropriate argument.
     */
    public void unregisterDiscovery() throws IllegalArgumentException {
        if (v) Log.d(TAG, "unregisterDiscovery()");
        if (registered) {
            context.getApplicationContext().unregisterReceiver(mReceiver);
            registered = false;
        }
    }

    /**
     * Method allowing to cancel the discovery process.
     */
    public void cancelDiscovery() {

        // Check if the device is already "discovering". If it is, then cancel discovery.
        if (bluetoothAdapter.isDiscovering()) {
            if (v) Log.d(TAG, "cancelDiscovery()");
            bluetoothAdapter.cancelDiscovery();
        }
    }

    /**
     * Method allowing to set the device into a discovery mode.
     *
     * @param duration an integer value between 0 and 3600 which represents the time of
     *                 the discovery mode.
     * @throws BluetoothBadDuration Signals that a Bluetooth Bad Duration exception has occurred.
     */
    public void enableDiscovery(int duration) throws BluetoothBadDuration {
        if (duration < 0 || duration > 3600) {
            throw new BluetoothBadDuration("Duration must be between 0 and 3600 second(s)");
        }

        if (bluetoothAdapter != null) {

            Intent discoverableIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
            context.startActivity(discoverableIntent);
        }
    }

    /**
     * Method allowing to get the all the BluetoothAdHoc devices.
     *
     * @return a HashMap<String, BluetoothAdHocDevice> that maps the device's name with
     * BluetoothAdHocDevice object.
     */
    public HashMap<String, BluetoothAdHocDevice> getHashMapBluetoothDevice() {
        return hashMapBluetoothDevice;
    }

    /**
     * Base class for code that receives and handles broadcast intents sent by
     * {@link android.content.Context#sendBroadcast(Intent)}.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {


            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Get the BluetoothDevice object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                discoveryListener.onDeviceFound(device);

                if (v) Log.d(TAG, "DeviceName: " + device.getName());
                // Add devices into the hashMap
                if (!hashMapBluetoothDevice.containsKey(device.getAddress())) {
                    if (v) Log.d(TAG, "DeviceName: " + device.getName() +
                            " - DeviceHardwareAddress: " + device.getAddress());
                    hashMapBluetoothDevice.put(device.getAddress(), new BluetoothAdHocDevice(device,
                            intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)));
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                if (v) Log.d(TAG, "ACTION_DISCOVERY_STARTED");
                // Clear the hashMap
                hashMapBluetoothDevice.clear();
                // Listener onDiscoveryStarted
                discoveryListener.onDiscoveryStarted();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (v) Log.d(TAG, "ACTION_DISCOVERY_FINISHED");
                // Listener onDiscoveryCompleted
                discoveryListener.onDiscoveryCompleted(hashMapBluetoothDevice);
            } else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                if (v) Log.d(TAG, "ACTION_SCAN_MODE_CHANGED");
                // Get current and old mode
                int currentMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, 0);
                int oldMode = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_SCAN_MODE, 0);
                // Listener onScanModeChange
                discoveryListener.onScanModeChange(currentMode, oldMode);
            }
        }
    };

    /**
     * Method allowing to set the Bluetooth adapter name.
     *
     * @param name a String value which represents the name of the Bluetooth adapter.
     */
    public void setAdapterName(String name) {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.setName(name);
        }
    }

    /**
     * Method allowing to get the Bluetooth adapter name.
     *
     * @return a String value which represents the name of the Bluetooth adapter.
     */
    public String getAdapterName() {
        if (bluetoothAdapter != null) {
            return bluetoothAdapter.getName();
        }
        return null;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }
}
