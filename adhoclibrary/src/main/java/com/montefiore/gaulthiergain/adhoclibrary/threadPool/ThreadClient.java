package com.montefiore.gaulthiergain.adhoclibrary.threadPool;


import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothService;
import com.montefiore.gaulthiergain.adhoclibrary.network.BluetoothNetwork;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.EOFException;
import java.io.IOException;

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
                network = listSocketDevice.getActiveConnexion().get(socketDevice.getRemoteDevice().getAddress());
                while (true) {
                    processRequest((MessageAdHoc) network.receiveObjectStream());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.d(TAG, "Error InterruptedException: " + e.getMessage());
            } catch (EOFException e) {
                Log.d(TAG, "Error EOFException: " + e.getMessage());
                e.printStackTrace(); //TODO remove
            } catch (IOException e) {
                Log.d(TAG, "Error IOException: " + e.getMessage());
                e.printStackTrace(); //TODO remove
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (network != null) {
                    String handleConnectionAborted[] = new String[2];

                    // Get remote device name
                    handleConnectionAborted[0] = network.getSocket().getRemoteDevice().getName();
                    // Get remote device address
                    handleConnectionAborted[1] = network.getSocket().getRemoteDevice().getAddress();
                    // Notify handler
                    handler.obtainMessage(BluetoothService.CONNECTION_ABORTED, handleConnectionAborted).sendToTarget();

                    // Close network
                    network.closeConnection();
                }

            }
        }
    }

    private void processRequest(MessageAdHoc request) throws IOException {
        Log.d(TAG, "Processing request " + request);
        handler.obtainMessage(BluetoothService.MESSAGE_READ, request).sendToTarget();
    }

    public String getNameThread() {
        return name;
    }
}
