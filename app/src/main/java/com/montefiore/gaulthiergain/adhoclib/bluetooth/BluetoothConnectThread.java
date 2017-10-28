package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by gaulthiergain on 28/10/17.
 *
 */

public class BluetoothConnectThread extends Thread {

    private String TAG = "[AdHoc]";
    private final UUID uuid;
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private final boolean secure;

    public BluetoothConnectThread(BluetoothDevice device, boolean secure) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        mmDevice = device;
        this.secure = secure;
        //uuid = UUID.fromString("e0917680-d427-11e4-8830-" + mmDevice.getAddress().replace(":", ""));TODO
        uuid = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        // TODO

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            //connectionFailed();TODO
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        //manageMyConnectedSocket(mmSocket);TODO
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}
