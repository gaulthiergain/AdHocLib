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
        String msg;
        while (true) {
            // Read response
            try {
                Log.d(TAG, "Waiting response from server ...");
                msg = network.receive();

                Log.d(TAG, "---> Response: " + msg);
                handler.obtainMessage(BluetoothService.MESSAGE_READ, msg).sendToTarget();
            } catch (IOException e){
                if(!network.getSocket().isConnected()){
                    network.closeConnection();
                }
                e.printStackTrace();
                break;
            }
        }
    }

    public void cancel() {
        if(network != null){
            network.closeConnection();
        }
    }
}