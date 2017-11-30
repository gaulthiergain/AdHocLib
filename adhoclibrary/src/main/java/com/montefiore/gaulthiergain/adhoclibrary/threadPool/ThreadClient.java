package com.montefiore.gaulthiergain.adhoclibrary.threadPool;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothService;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.network.ISocket;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.wifi.WifiService;

import java.io.EOFException;
import java.io.IOException;

public class ThreadClient extends Thread {

    private static final String TAG = "[AdHoc]";

    private final ListSocketDevice listSocketDevice;
    private final String name;
    private final Handler handler;
    private NetworkObject network = null;

    ThreadClient(ListSocketDevice listSocketDevice, String name, Handler handler) {
        this.listSocketDevice = listSocketDevice;
        this.name = name;
        this.handler = handler;
    }

    public void run() {
        ISocket socketDevice = null;
        while (!isInterrupted()) {
            try {
                socketDevice = listSocketDevice.getSocketDevice();
                network = listSocketDevice.getActiveConnexion().get(socketDevice.getRemoteSocketAddress());
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
                    if (socketDevice instanceof AdHocSocketWifi) {
                        // Notify handler
                        String messageHandle = network.getISocket().getRemoteSocketAddress();
                        handler.obtainMessage(WifiService.CONNECTION_ABORTED, messageHandle).sendToTarget();
                    } else {

                        // Get Socket
                        BluetoothSocket socket = (BluetoothSocket) network.getISocket().getSocket();

                        String handleConnectionAborted[] = new String[2];
                        // Get remote device name
                        handleConnectionAborted[0] = socket.getRemoteDevice().getName();
                        // Get remote device address
                        handleConnectionAborted[1] = socket.getRemoteDevice().getAddress();
                        // Notify handler
                        handler.obtainMessage(BluetoothService.CONNECTION_ABORTED, handleConnectionAborted).sendToTarget();
                    }

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