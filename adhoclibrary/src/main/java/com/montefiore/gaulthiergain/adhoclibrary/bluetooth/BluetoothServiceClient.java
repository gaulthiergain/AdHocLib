package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.service.ServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by gaulthiergain on 28/10/17.
 */

public class BluetoothServiceClient extends ServiceClient {

    private boolean background;

    public BluetoothServiceClient(boolean verbose, Context context, MessageListener messageListener, boolean background) {
        super(verbose, context, messageListener, background);
        this.background = background;
    }

    public void connect(boolean secure, BluetoothAdHocDevice bluetoothAdHocDevice) throws NoConnectionException {
        if (v) Log.d(TAG, "connect to: " + bluetoothAdHocDevice.getDevice().getName());

        if (state == STATE_NONE) {

            // Get the UUID
            UUID uuid = UUID.fromString(bluetoothAdHocDevice.getUuid());
            // Change the state
            setState(STATE_CONNECTING);

            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            BluetoothSocket bluetoothSocket;
            try {
                if (secure) {
                    bluetoothSocket = bluetoothAdHocDevice.getDevice().createRfcommSocketToServiceRecord(uuid);
                } else {
                    bluetoothSocket = bluetoothAdHocDevice.getDevice().createInsecureRfcommSocketToServiceRecord(uuid);
                }

                // Connect to the remote host
                bluetoothSocket.connect();
                network = new NetworkObject(new AdHocSocketBluetooth(bluetoothSocket));

                // Notify handler TODO
                String messageHandle[] = new String[2];
                messageHandle[0] = bluetoothSocket.getRemoteDevice().getName();
                messageHandle[1] = bluetoothSocket.getRemoteDevice().getAddress();
                handler.obtainMessage(Service.CONNECTION_PERFORMED, messageHandle).sendToTarget();


                // Update state
                setState(STATE_CONNECTED);

                // Listen in Background
                if (background) {
                    listenInBackground();
                }
            } catch (IOException e) {
                setState(STATE_NONE);
                e.printStackTrace();
                throw new NoConnectionException("No remote connection");
            }
        }
    }

}
