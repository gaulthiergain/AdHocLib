package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Build;

/**
 * Created by gaulthiergain on 8/12/17.
 */

public class BluetoothUtil {

    public static String UUID = "e0917680-d427-11e4-8830-";


    public static String getCurrentMac(Context context) {

        String mac;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mac = android.provider.Settings.Secure.getString(context.getContentResolver(), "bluetooth_address");
        } else {
            mac = BluetoothAdapter.getDefaultAdapter().getAddress();
        }

        return mac;
    }

    public static String getCurrentName() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.getName();
        }
        return null;
    }
}
