package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothService;
import com.montefiore.gaulthiergain.adhoclibrary.network.WifiNetwork;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class ThreadClient extends Thread {

    private static final String TAG = "[AdHoc]";

    private final ListSocketDevice listSocketDevice;
    private final String name;
    private final Handler handler;
    private WifiNetwork network = null;

    ThreadClient(ListSocketDevice listSocketDevice, String name, Handler handler) {
        this.listSocketDevice = listSocketDevice;
        this.name = name;
        this.handler = handler;
    }

    public void run() {
        while (!isInterrupted()) {
            try {
                Socket socketDevice = listSocketDevice.getSocketDevice();
                network = listSocketDevice.getActiveConnexion().get(socketDevice.getRemoteSocketAddress().toString());
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
                    /*
                    // Notify handler
                    String messageHandle = socket.getRemoteSocketAddress().toString();
                    handler.obtainMessage(WifiService.CONNECTION_PERFORMED, messageHandle).sendToTarget();
                    */

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
