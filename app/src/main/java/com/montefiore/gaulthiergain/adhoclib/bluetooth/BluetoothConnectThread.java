package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclib.BluetoothConnect;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by gaulthiergain on 28/10/17.
 */

public class BluetoothConnectThread extends Thread {

    private String TAG = "[AdHoc]";
    private BluetoothNetwork bluetoothNetwork;
    private final BluetoothDevice device;

    private Handler mHandler;

    public BluetoothConnectThread(Handler mHandler, BluetoothDevice device, boolean secure, UUID uuid) {
        this.device = device;
        this.mHandler = mHandler;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            if (secure) {
                this.bluetoothNetwork = new BluetoothNetwork(this.device.createRfcommSocketToServiceRecord(uuid));
            } else {
                this.bluetoothNetwork = new BluetoothNetwork(this.device.createInsecureRfcommSocketToServiceRecord(uuid));
            }
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
    }

    public void run() {
        // Cancel discovery because it otherwise slows down the connection.
        // TODO

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            bluetoothNetwork.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                bluetoothNetwork.closeSocket();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            //connectionFailed();TODO
            return;
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.TODO
        while (true) {
            try {
                Log.d(TAG, "WAITING: ");

                //String receive = bluetoothNetwork.receiveString();
               // Log.d(TAG, "Message received: " + receive);

                byte[] buffer = new byte[256];
                int bytes;
                bytes = bluetoothNetwork.getInput().read(buffer);
                Log.d(TAG, "RCVD: " + new String(buffer, 0, bytes));

                // Send the obtained bytes to the UI Activity
                mHandler.obtainMessage(BluetoothConnect.MESSAGE_READ, bytes, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            bluetoothNetwork.closeSocket();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }
}
