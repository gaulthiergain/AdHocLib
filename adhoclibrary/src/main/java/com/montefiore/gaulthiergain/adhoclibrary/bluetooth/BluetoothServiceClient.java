package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetoothListener.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by gaulthiergain on 28/10/17.
 */


public class BluetoothServiceClient extends BluetoothService {

    protected NetworkObject bluetoothNetwork;
    protected boolean background;
    protected BluetoothListenThread threadListening;

    public BluetoothServiceClient(Context context, boolean verbose, boolean background, MessageListener messageListener) {
        super(context, verbose, messageListener);
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
                bluetoothNetwork = new NetworkObject(new AdHocSocketBluetooth(bluetoothSocket));


                // Notify handler
                String messageHandle[] = new String[2];
                messageHandle[0] = bluetoothSocket.getRemoteDevice().getName();
                messageHandle[1] = bluetoothSocket.getRemoteDevice().getAddress();
                handler.obtainMessage(BluetoothService.CONNECTION_PERFORMED, messageHandle).sendToTarget();


                // Update state
                setState(STATE_CONNECTED);

                // Listen in Background
                if (background) {
                    listenInBackground();
                }
            } catch (IOException e) {
                setState(STATE_NONE);
                throw new NoConnectionException("No remote connection");
            }
        }
    }

    private void listenInBackground() throws NoConnectionException, IOException {
        if (v) Log.d(TAG, "listenInBackground()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        } else {
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
    }

    protected void stopListeningInBackground() {
        if (v) Log.d(TAG, "stopListeningInBackground()");

        if (state == STATE_LISTENING_CONNECTED) {
            // Cancel any thread currently running a connection
            if (threadListening != null) {
                threadListening.cancel();
                threadListening = null;
            }
            // Update the state of the connection
            setState(STATE_CONNECTED);
        }
    }

    public void send(MessageAdHoc msg) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "send()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        } else {
            // Send message to remote device
            bluetoothNetwork.sendObjectStream(msg);

            // Notify handler
            handler.obtainMessage(BluetoothService.MESSAGE_WRITE, msg).sendToTarget();
        }
    }

    public void disconnect() throws NoConnectionException {
        if (v) Log.d(TAG, "disconnect()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        } else {
            if (state == STATE_CONNECTED) {
                bluetoothNetwork.closeConnection();
            } else if (state == STATE_LISTENING_CONNECTED) {
                stopListeningInBackground();
            }

            // Update the state of the connection
            setState(STATE_NONE);
        }
    }

}