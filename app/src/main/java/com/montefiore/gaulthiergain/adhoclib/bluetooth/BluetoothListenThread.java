package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by gaulthiergain on 28/10/17.
 *
 */

public class BluetoothListenThread extends Thread {

    private String TAG = "[AdHoc]";
    private BluetoothServerNetwork bluetoothNetwork;
    private String socketType;

    public BluetoothListenThread(boolean secure, String name, BluetoothAdapter mAdapter, UUID uuid) {
        this.socketType = secure ? "Secure" : "Insecure";
        //uuid = UUID.fromString("e0917680-d427-11e4-8830-" + mmDevice.getAddress().replace(":", "")); TODO

        // Create a new listening server socket
        try {
            if (secure) {
                this.bluetoothNetwork = new BluetoothServerNetwork(mAdapter.listenUsingRfcommWithServiceRecord(name, uuid));
            } else {
                this.bluetoothNetwork = new BluetoothServerNetwork(mAdapter.listenUsingInsecureRfcommWithServiceRecord(name, uuid));
            }
        } catch (IOException e) {
            Log.e(TAG, "Socket Type: " + socketType + "listen() failed", e);
        }
    }

    public void run() {

        Log.d(TAG, "Socket Type: " + socketType + "BEGIN mAcceptThread" + this);
        setName("BluetoothListenThread" + socketType);

        BluetoothNetwork bluetoothSocketNetwork;

        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
            bluetoothSocketNetwork = new BluetoothNetwork(bluetoothNetwork.accept());

            // If a connection was accepted
            if (bluetoothSocketNetwork.getSocket() != null) {
            /*connected(socket, socket.getRemoteDevice(),
                    socketType);*/
                //
                Log.d(TAG, "CONNECTED: " + socketType);
                bluetoothSocketNetwork.sendString("test");
            }


            Log.d(TAG, "END BluetoothListenThread, socket Type: " + socketType);

        } catch (IOException e) {
            Log.e(TAG, "Socket Type: " + socketType + "accept() failed", e);
        }

    }

    public void cancel() {
        Log.d(TAG, "Socket Type" + socketType + "cancel " + this);
        try {
            bluetoothNetwork.closeSocket();
        } catch (IOException e) {
            Log.e(TAG, "Socket Type" + socketType + "close() of server failed", e);
        }
    }
}
