package com.montefiore.gaulthiergain.adhoclibrary.bluetoothLowEnergy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.DeviceException;

import java.util.HashMap;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLEManager {

    private boolean scanning;

    private final boolean v;
    private final Context context;
    private final Handler handler;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothManager bluetoothManager;
    private final HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice;
    private final String TAG = "[AdHoc][BlueLE.Manager]";

    /**
     * Constructor
     *
     * @param verbose a boolean value to set the debug/verbose mode.
     * @param context a Context object which gives global information about an application
     *                environment.
     * @throws DeviceException Signals that a Bluetooth Device Exception exception
     *                         has occurred.
     */

    public BluetoothLEManager(boolean verbose, Context context)
            throws DeviceException {

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            // Device does not support Bluetooth LE
            throw new DeviceException("Error device does not support Bluetooth Low Energy");
        }

        this.bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            // Device does not support Bluetooth
            throw new DeviceException("Error device does not support Bluetooth Low Energy");
        } else {
            this.bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null) {
                // Device does not support Bluetooth
                throw new DeviceException("Error device does not support Bluetooth Low Energy");
            } else {
                // Device supports Bluetooth
                this.v = verbose;
                this.context = context;
                this.handler = new Handler();
                this.hashMapBluetoothDevice = new HashMap<>();
            }
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (v) Log.d(TAG, "Device Found: " + device.getName() + " "
                            + device.getAddress() + " " + rssi);
                }
            };

    public void discovery(final boolean enable, int period) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void run() {
                    scanning = false;
                    bluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, period);

            scanning = true;
            bluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            scanning = false;
            bluetoothAdapter.stopLeScan(mLeScanCallback);
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
     * Method allowing to set the Bluetooth Low Energy adapter name.
     *
     * @param name a String value which represents the name of the bluetooth Low Energy adapter.
     */
    public void setAdapterName(String name) {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.setName(name);
        }
    }

    /**
     * Method allowing to get the Bluetooth Low Energy adapter name.
     *
     * @return a String value which represents the name of the bluetooth Low Energy adapter.
     */
    public String getAdapterName() {
        if (bluetoothAdapter != null) {
            return bluetoothAdapter.getName();
        }
        return null;
    }
}
