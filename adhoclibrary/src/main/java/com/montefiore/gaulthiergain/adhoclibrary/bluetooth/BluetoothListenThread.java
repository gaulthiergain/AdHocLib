package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;
import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.network.BluetoothNetwork;

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
        String handleMessage[] = new String[3];
        while (true) {
            // Read response
            try {

                Log.d(TAG, "Waiting response from server ...");

                // Get response
                handleMessage[0] = network.receive();
                // Get remote device name
                handleMessage[1] = network.getSocket().getRemoteDevice().getName();
                // Get remote device address
                handleMessage[2] = network.getSocket().getRemoteDevice().getAddress();

                Log.d(TAG, "---> Response: " + handleMessage[0]);
                handler.obtainMessage(BluetoothService.MESSAGE_READ, handleMessage).sendToTarget();
            } catch (IOException e){
                if(!network.getSocket().isConnected()){
                    network.closeConnection();
                }
                handler.obtainMessage(BluetoothService.CONNECTION_ABORTED, -1).sendToTarget();
                break;
            }
        }
    }

    public void cancel() {
        if(network != null){ //TODO !network.getSocket().isConnected()
            network.closeConnection();
        }
    }
}