package com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;

/**
 * <p>This class allows to get the current name and the current MAC address of the bluetoothAdapter,
 * and defines the common UUID.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class BluetoothUtil {

    public final static String UUID = "e0917680-d427-11e4-8830-";

    /**
     * Method allowing to get the current MAC address of the bluetoothAdapter.
     *
     * @param context a Context object which gives global information about an application environment.
     * @return a String value which represents the current MAC address of the bluetoothAdapter.
     */
    @SuppressLint("HardwareIds")
    public static String getCurrentMac(Context context) {

        String mac;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mac = android.provider.Settings.Secure.getString(context.getContentResolver(), "bluetooth_address");
        } else {
            mac = BluetoothAdapter.getDefaultAdapter().getAddress();
        }

        return mac;
    }

    /**
     * Method allowing to get the current name of the bluetoothAdapter.
     *
     * @return a String value which represents the current name of the bluetoothAdapter.
     */
    @Nullable
    public static String getCurrentName() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.getName();
        }
        return null;
    }

    /**
     * Method allowing to check if the Bluetooth adapter is enabled.
     *
     * @return a boolean value which represents the status of the bluetooth adapter.
     */
    public static boolean isEnabled() {
        return BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled();
    }
}
