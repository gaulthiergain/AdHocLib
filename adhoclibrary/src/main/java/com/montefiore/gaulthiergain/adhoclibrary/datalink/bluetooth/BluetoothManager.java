package com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;

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
    private final HashMap<String, AdHocDevice> hashMapBluetoothDevice;

    private String initialName;
    private boolean registered = false;
    private BroadcastReceiver mReceiverAdapter;
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
            this.initialName = bluetoothAdapter.getName();
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
     * Method allowing to disable the Bluetooth adapter.
     */
    public void disable() {
        bluetoothAdapter.disable();
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
                        new BluetoothAdHocDevice(device, Service.BLUETOOTH));
            }
        }
        return hashMapBluetoothPairedDevice;
    }

    public boolean updateDeviceName(String name) {
        return bluetoothAdapter.setName(name);
    }

    public void resetDeviceName() {
        if (initialName != null) {
            bluetoothAdapter.setName(initialName);
        }
    }

    /**
     * Method allowing to discovery other bluetooth devices.
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
    private void cancelDiscovery() {

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
            discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
            context.startActivity(discoverableIntent);
        }
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
                BluetoothAdHocDevice btDevice = new BluetoothAdHocDevice(
                        (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE),
                        intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE));

                // Add devices into the hashMap
                if (!hashMapBluetoothDevice.containsKey(btDevice.getMacAddress())) {
                    if (v) Log.d(TAG, "DeviceName: " + btDevice.getDeviceName() +
                            " - DeviceHardwareAddress: " + btDevice.getMacAddress());

                    hashMapBluetoothDevice.put(btDevice.getMacAddress(), btDevice);

                    // Listener onDeviceDiscovered
                    discoveryListener.onDeviceDiscovered(btDevice);
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
            }
        }
    };

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

    public void onEnableBluetooth(final ListenerAdapter listenerAdapter) {

        unregisterEnableAdapter();

        mReceiverAdapter = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (bluetoothState) {
                        case BluetoothAdapter.STATE_ON:
                            listenerAdapter.onEnableBluetooth(true);
                            break;
                        case BluetoothAdapter.ERROR:
                            listenerAdapter.onEnableBluetooth(false);
                            break;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(mReceiverAdapter, filter);
    }

    public void unregisterEnableAdapter() {
        if (mReceiverAdapter != null) {
            context.unregisterReceiver(mReceiverAdapter);
            mReceiverAdapter = null;
        }
    }
}
