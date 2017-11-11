package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by gaulthiergain on 28/10/17.
 */


public class BluetoothServiceClient extends BluetoothService {

    private ArrayList<BluetoothAdHocDevice> arrayConnectedDevice;

    private BluetoothConnectThread threadConnect;

    public BluetoothServiceClient(Context context, boolean verbose) {
        super(context, verbose);
        this.arrayConnectedDevice = new ArrayList<>();
    }

    public void connect(boolean secure, BluetoothAdHocDevice device) {
        if (v) Log.d(TAG, "connect to: " + device.getDevice().getName());

        // Cancel any thread currently running a connection
        if (threadConnect != null) {
            threadConnect.cancel();
            threadConnect = null;
        }

        setState(STATE_CONNECTING);

        // Add device to the list of connected devices
        arrayConnectedDevice.add(device);

        // Start the thread to connect with the given device
        threadConnect = new BluetoothConnectThread(handler, device.getDevice(), secure,
                UUID.fromString(device.getUuid()));
        threadConnect.start();

        //setState(STATE_CONNECTED);TODO
    }

    public void cancel(){
        threadConnect.cancel();
    }


}
