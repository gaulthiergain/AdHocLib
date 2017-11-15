package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclib.bluetoothListener.MessageListener;
import com.montefiore.gaulthiergain.adhoclib.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclib.network.BluetoothNetwork;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by gaulthiergain on 28/10/17.
 */


public class BluetoothServiceClient extends BluetoothService {

    private BluetoothNetwork bluetoothNetwork;
    private BluetoothListenThread threadListening;

    public BluetoothServiceClient(Context context, boolean verbose, MessageListener messageListener) {
        super(context, verbose, messageListener);
    }


    public void connect(boolean secure, BluetoothAdHocDevice bluetoothAdHocDevice) throws NoConnectionException {
        if (v) Log.d(TAG, "connect to: " + bluetoothAdHocDevice.getDevice().getName());

        if (state == STATE_NONE) {

            // Get the UUID
            UUID uuid = UUID.fromString(bluetoothAdHocDevice.getUuid());
            // Change the state
            setState(STATE_CONNECTING);

            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            BluetoothSocket bluetoothSocket = null;
            try {
                if (secure) {
                    bluetoothSocket = bluetoothAdHocDevice.getDevice().createRfcommSocketToServiceRecord(uuid);
                } else {
                    bluetoothSocket = bluetoothAdHocDevice.getDevice().createInsecureRfcommSocketToServiceRecord(uuid);
                }

            } catch (IOException e) {
                setState(STATE_NONE);
                e.printStackTrace();
            }

            if (bluetoothSocket == null) {
                setState(STATE_NONE);
                throw new NoConnectionException("No remote connection");
                //TODO debug here to check the statements
            }

            // Connect to the remote host
            try {
                bluetoothSocket.connect();
            } catch (IOException e) {
                //TODO close connection corretly
                e.printStackTrace();
            }

            bluetoothNetwork = new BluetoothNetwork(bluetoothSocket, false);

            setState(STATE_CONNECTED);
        }

    }

    public void listenInBackground() throws NoConnectionException, IOException {
        if (v) Log.d(TAG, "listenInBackground()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        }

        // Cancel any thread currently running a connection
        if (threadListening != null) {
            threadListening.cancel();
            threadListening = null;
        }

        setState(STATE_LISTENING_CONNECTED);

        // Start the thread to connect with the given device
        threadListening = new BluetoothListenThread(bluetoothNetwork, handler);
        threadListening.start();
    }

    public void stopListeningInBackground() {
        if (v) Log.d(TAG, "stopListeningInBackground()");

        if (state == STATE_LISTENING_CONNECTED) {
            // Cancel any thread currently running a connection
            if (threadListening != null) {
                threadListening.cancel();
                threadListening = null;
            }

            setState(STATE_CONNECTED);
        }
    }

    public void send(String msg) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "send()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
            //TODO debug here
        }

        bluetoothNetwork.send(msg);

    }

    public void disconnect() throws NoConnectionException {
        if (v) Log.d(TAG, "disconnect()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");

        }

        if (state == STATE_CONNECTED) {
            bluetoothNetwork.closeConnection();
        } else if (state == STATE_LISTENING_CONNECTED) {
            stopListeningInBackground();
        }

        setState(STATE_NONE);
    }

}
