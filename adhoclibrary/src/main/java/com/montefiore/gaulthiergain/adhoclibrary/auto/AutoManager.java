package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.DeviceException;

public class AutoManager {

    private BluetoothManager bluetoothManager;
    private final static int DURATION = 10;
    private Context context;

    public AutoManager(Context context) {
        this.context = context;
    }

    public void discovery() {
        try {
            bluetoothManager = new BluetoothManager(true, context);
        } catch (DeviceException e) {
            e.printStackTrace();
        }

        if (!bluetoothManager.isEnabled()) {
            // Enable bluetooth and enable the discovery
            try {
                bluetoothManager.enable();
                bluetoothManager.enableDiscovery(DURATION);
            } catch (BluetoothBadDuration bluetoothBadDuration) {
                bluetoothBadDuration.printStackTrace();
            }
        }
    }


}
