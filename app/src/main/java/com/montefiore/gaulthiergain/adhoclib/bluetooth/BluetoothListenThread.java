package com.montefiore.gaulthiergain.adhoclib.bluetooth;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclib.network.BluetoothNetwork;

import java.io.IOException;

/**
 * Created by gaulthiergain on 15/11/17.
 */

public class BluetoothListenThread extends Thread {

    private static final String TAG = "[AdHoc]";
    private final BluetoothNetwork network;

    public BluetoothListenThread(BluetoothNetwork network) {
        this.network = network;
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