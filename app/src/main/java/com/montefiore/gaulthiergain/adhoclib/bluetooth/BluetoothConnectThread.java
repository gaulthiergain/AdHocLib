package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothDevice;
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
    private BluetoothNetwork bluetoothNetwork;
    private final BluetoothDevice device;

    public BluetoothConnectThread(BluetoothDevice device, boolean secure) {
        this.device = device;
        //uuid = UUID.fromString("e0917680-d427-11e4-8830-" + device.getAddress().replace(":", ""));TODO
        this.uuid = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

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
        try {
            Log.d(TAG,"WAITING: ");
            String receive = bluetoothNetwork.receiveString();
            Log.d(TAG,"Message received: " + receive);
        } catch (IOException e) {
            e.printStackTrace();
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
