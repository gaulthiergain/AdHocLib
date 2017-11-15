package com.montefiore.gaulthiergain.adhoclib.threadPool;


import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothService;
import com.montefiore.gaulthiergain.adhoclib.network.BluetoothNetwork;

import java.io.EOFException;
import java.io.IOException;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

/**
 * Created by gaulthiergain on 10/11/17.
 */
public class ThreadClient extends Thread {

    private static final String TAG = "[AdHoc]";

    private final ListSocketDevice listSocketDevice;
    private final String name;
    private final Handler handler;
    private BluetoothNetwork network = null;

    ThreadClient(ListSocketDevice listSocketDevice, String name, Handler handler) {
        this.listSocketDevice = listSocketDevice;
        this.name = name;
        this.handler = handler;
    }

    public void run() {
        while (!isInterrupted()) {
            try {
                BluetoothSocket socketDevice = listSocketDevice.getSocketDevice();
                network = new BluetoothNetwork(socketDevice, false);
                while (true) {
                    processRequest(network.receive());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.d(TAG, "Error InterruptedException: " + e.getMessage());
            } catch (EOFException e) {
                Log.d(TAG, "Error EOFException: " + e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                Log.d(TAG, "Error IOException: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (network != null)
                    network.closeConnection();
            }
        }
    }

    private void processRequest(String request) throws IOException {
        Log.d(TAG, "Processing request " + request);
        handler.obtainMessage(BluetoothService.MESSAGE_READ, request).sendToTarget();
    }

    public String getNameThread() {
        return name;
    }
}
