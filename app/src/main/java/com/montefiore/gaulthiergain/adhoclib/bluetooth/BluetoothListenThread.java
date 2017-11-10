package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclib.network.BAKBluetoothNetwork;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by gaulthiergain on 28/10/17.
 */

public class BluetoothListenThread extends Thread {

    private String TAG = "[AdHoc]";
    private BAKBluetoothNetwork bluetoothSocketNetwork;
    private BluetoothServerSocket bluetoothServerNetwork;
    private String socketType;

    private Handler mHandler;

    public BluetoothListenThread(Handler mHandler, boolean secure, String name, BluetoothAdapter mAdapter, UUID uuid) {
        this.socketType = secure ? "Secure" : "Insecure";
        this.mHandler = mHandler;
        // Create a new listening server socket
        try {
            if (secure) {
                this.bluetoothServerNetwork = mAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
            } else {
                this.bluetoothServerNetwork = mAdapter.listenUsingInsecureRfcommWithServiceRecord(name, uuid);
            }
        } catch (IOException e) {
            Log.e(TAG, "Socket Type: " + socketType + "listen() failed", e);
        }
    }

    public void sendMessage(String msg) throws IOException {
        bluetoothSocketNetwork.sendString(msg);
    }

    public void run() {

        Log.d(TAG, "Socket Type: " + socketType + "BEGIN mAcceptThread" + this);
        setName("BluetoothListenThread" + socketType);


        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
            bluetoothSocketNetwork = new BAKBluetoothNetwork(bluetoothServerNetwork.accept());

            // If a connection was accepted
            if (bluetoothSocketNetwork.getSocket() != null) {
                //TODO State here
                Log.d(TAG, "CONNECTED: " + socketType);
            }


            Log.d(TAG, "END BluetoothListenThread, socket Type: " + socketType);

        } catch (IOException e) {
            Log.e(TAG, "Socket Type: " + socketType + "accept() failed", e);
        }

    }

    public void cancel() {
        Log.d(TAG, "Socket Type" + socketType + "cancel " + this);
        try {
            bluetoothServerNetwork.close();
        } catch (IOException e) {
            Log.e(TAG, "Socket Type" + socketType + "close() of server failed", e);
        }
    }
}
