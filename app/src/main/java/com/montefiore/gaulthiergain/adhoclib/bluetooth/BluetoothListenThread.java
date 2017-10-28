package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by gaulthiergain on 28/10/17.
 *
 */

public class BluetoothListenThread extends Thread {

    private String TAG = "[AdHoc]";
    private final BluetoothServerSocket mmServerSocket;
    private String mSocketType;


    public BluetoothListenThread(boolean secure, String name, BluetoothAdapter mAdapter, UUID uuid) {
        BluetoothServerSocket tmp = null;
        mSocketType = secure ? "Secure" : "Insecure";
        //uuid = UUID.fromString("e0917680-d427-11e4-8830-" + mmDevice.getAddress().replace(":", "")); TODO


        // Create a new listening server socket
        try {
            if (secure) {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(name,
                        uuid);
            } else {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        name, uuid);
            }
        } catch (IOException e) {
            Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
        }
        mmServerSocket = tmp;
    }

    public void run() {
        Log.d(TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this);
        setName("AcceptThread" + mSocketType);

        BluetoothSocket socket = null;

        try {
            // This is a blocking call and will only return on a
            // successful connection or an exception
            socket = mmServerSocket.accept();
        } catch (IOException e) {
            Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
        }

        // If a connection was accepted
        if (socket != null) {
            /*connected(socket, socket.getRemoteDevice(),
                    mSocketType);*/
            //
            Log.d(TAG, "SUCCEDEED: " + mSocketType);
        }


        Log.d(TAG, "END mAcceptThread, socket Type: " + mSocketType);

    }

    public void cancel() {
        Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
        }
    }
}
