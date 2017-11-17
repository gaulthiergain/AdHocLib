package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.network.BluetoothNetwork;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

/**
 * Created by gaulthiergain on 15/11/17.
 */

public class BluetoothListenThread extends Thread {

    private static final String TAG = "[AdHoc]";
    private final BluetoothNetwork network;
    private final Handler handler;

    public BluetoothListenThread(BluetoothNetwork network, Handler handler) {
        this.network = network;
        this.handler = handler;
    }

    @Override
    public void run() {
        Log.d(TAG, "start Listening ...");
        MessageAdHoc message;
        while (true) {
            // Read response
            try {

                Log.d(TAG, "Waiting response from server ...");

                // Get response MessageAdHoc
                message = (MessageAdHoc) network.receiveObjectStream();

                Log.d(TAG, "---> Response: " + message);
                handler.obtainMessage(BluetoothService.MESSAGE_READ, message).sendToTarget();
            } catch (IOException e) {
                if (!network.getSocket().isConnected()) {
                    network.closeConnection();
                }
                String handleConnectionAborted[] = new String[2];
                // Get remote device name
                handleConnectionAborted[0] = network.getSocket().getRemoteDevice().getName();
                // Get remote device address
                handleConnectionAborted[1] = network.getSocket().getRemoteDevice().getAddress();
                // Notify handler
                handler.obtainMessage(BluetoothService.CONNECTION_ABORTED, handleConnectionAborted).sendToTarget();
                break;
            } catch (ClassNotFoundException e) {
                e.printStackTrace(); //TODO update
            }
        }
    }

    public void cancel() {
        if (network != null) { //TODO !network.getSocket().isConnected()
            network.closeConnection();
        }
    }
}