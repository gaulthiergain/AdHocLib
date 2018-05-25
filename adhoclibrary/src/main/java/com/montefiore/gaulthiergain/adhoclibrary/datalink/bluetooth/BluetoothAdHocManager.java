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
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

/**
 * <p>This class manages the Bluetooth discovery and the pairing with other bluetooth devices.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class BluetoothAdHocManager {

    private final boolean v;
    private final BluetoothAdapter bluetoothAdapter;
    private final String TAG = "[AdHoc][Blue.Manager]";
    private final HashMap<String, AdHocDevice> hashMapBluetoothDevice;

    private Context context;
    private String initialName;
    private boolean registeredAdapter;
    private boolean registeredDiscovery;
    private BroadcastReceiver mReceiverAdapter;
    private DiscoveryListener discoveryListener;

    /**
     * Constructor
     *
     * @param verbose a boolean value to set the debug/verbose mode.
     * @param context a Context object which gives global information about an application
     *                environment.
     */
    public BluetoothAdHocManager(boolean verbose, Context context) {
        this.v = verbose;
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.initialName = bluetoothAdapter.getName();
        this.hashMapBluetoothDevice = new HashMap<>();
        this.registeredAdapter = false;
        this.registeredDiscovery = false;
    }

    /**
     * Method allowing to disable the Bluetooth adapter.
     */
    public void disable() {
        bluetoothAdapter.disable();
    }

    /**
     * Method allowing to enable the Bluetooth adapter.
     */
    public void enable() {
        bluetoothAdapter.enable();
    }

    /**
     * Method allowing to set the device into a discovery mode.
     *
     * @param duration an integer value between 0 and 3600 which represents the time of
     *                 the discovery mode.
     * @throws BluetoothBadDuration signals that a Bluetooth Bad Duration exception has occurred.
     */
    public void enableDiscovery(Context context, int duration) throws BluetoothBadDuration {

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
     * Method allowing to get all the paired Bluetooth devices.
     *
     * @return a HashMap<String, BluetoothAdHocDevice> that maps the device's name with a
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

    /**
     * Method allowing to update the device local adapter name.
     *
     * @param name a String value which represents the new name of the  device adapter.
     * @return true if the name was set, false otherwise.
     */
    public boolean updateDeviceName(String name) {
        return bluetoothAdapter.setName(name);
    }

    /**
     * Method allowing to reset the device local adapter name.
     */
    public void resetDeviceName() {
        if (initialName != null) {
            bluetoothAdapter.setName(initialName);
        }
    }

    /**
     * Method allowing to discovery other bluetooth devices.
     *
     * @param discoveryListener a discoveryListener object which contains callback functions.
     */
    public void discovery(DiscoveryListener discoveryListener) {

        if (v) Log.d(TAG, "discovery()");

        // Check if the device is already "discovering". If it is, then cancel discovery.
        cancelDiscovery();

        // Update Listener
        this.discoveryListener = discoveryListener;

        // Start Discovery
        bluetoothAdapter.startDiscovery();

        // Set Register to true
        registeredDiscovery = true;

        // Register for broadcasts when a device is discovered.
        context.registerReceiver(mReceiver, new IntentFilter(
                BluetoothDevice.ACTION_FOUND));
        context.registerReceiver(mReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        context.registerReceiver(mReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
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
     * Method allowing to unregister the discovery broadcast.
     *
     * @throws IllegalArgumentException signals that a method has been passed an illegal or
     *                                  inappropriate argument.
     */
    public void unregisterDiscovery() throws IllegalArgumentException {

        if (registeredDiscovery) {
            if (v) Log.d(TAG, "unregisterDiscovery()");
            context.unregisterReceiver(mReceiver);
            registeredDiscovery = false;
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

        // Unregister discovery if necessary (avoid memory leaks)
        unregisterDiscovery();
    }

    /**
     * Method allowing to get the Bluetooth adapter name.
     *
     * @return a String value which represents the name of the Bluetooth adapter.
     */
    public String getAdapterName() {

        if (v) Log.d(TAG, "getAdapterName()");

        if (bluetoothAdapter != null) {
            return bluetoothAdapter.getName();
        }
        return null;
    }

    /**
     * Method allowing to unpair a previously paired bluetooth device.
     *
     * @param device a BluetoothAdHocDevice object which represents a remote Bluetooth device.
     * @throws InvocationTargetException signals that a method does not exist.
     * @throws IllegalAccessException    signals that an application tries to reflectively create
     *                                   an instance which has no access to the definition of
     *                                   the specified class
     * @throws NoSuchMethodException     signals that a method does not exist.
     */
    public void unpairDevice(BluetoothAdHocDevice device) throws InvocationTargetException,
            IllegalAccessException, NoSuchMethodException {

        if (v) Log.d(TAG, "unpairDevice()");

        Method m = device.getDevice().getClass().getMethod("removeBond", (Class[]) null);
        m.invoke(device.getDevice(), (Object[]) null);
    }

    /**
     * Method allowing to notify if the Bluetooth adapter has been enabled.
     *
     * @param listenerAdapter a listenerAdapter object which contains callback functions.
     */
    public void onEnableBluetooth(final ListenerAdapter listenerAdapter) {

        if (v) Log.d(TAG, "onEnableBluetooth()");

        // unregister BroadcastReceiver event
        unregisterAdapter();

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

        // Register again to BroadcastReceiver event
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(mReceiverAdapter, filter);
        registeredAdapter = true;
    }

    /**
     * Method allowing to unregister a previously registered BroadcastReceiver related
     * to adapter event.
     */
    public void unregisterAdapter() {

        if (registeredAdapter) {
            if (v) Log.d(TAG, "unregisterAdapter()");
            context.unregisterReceiver(mReceiverAdapter);
            registeredAdapter = false;
        }
    }

    /**
     * Method allowing to update the context of the current class.
     *
     * @param context a Context object which gives global information about an application
     *                environment.
     */
    public void updateContext(Context context) {

        if (v) Log.d(TAG, "updateContext()");

        // Unregister previous context to avoid memory leak
        unregisterDiscovery();
        unregisterAdapter();

        // Update new context
        this.context = context;
    }
}
