package com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.RemoteWifiConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.ISocket;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.RemoteBtConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.EOFException;
import java.io.IOException;

class ThreadClient extends Thread {

    private final ListSocketDevice listSocketDevice;
    private final String name;
    private final Handler handler;
    private NetworkManager network = null;

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
                network = listSocketDevice.getActiveConnection().get(socketDevice.getRemoteSocketAddress());
                while (true) {
                    processRequest(network.receiveMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (EOFException e) {
                handler.obtainMessage(Service.CATH_EXCEPTION, e).sendToTarget();
            } catch (IOException e) {
                handler.obtainMessage(Service.CATH_EXCEPTION, e).sendToTarget();
            } catch (ClassNotFoundException e) {
                handler.obtainMessage(Service.CATH_EXCEPTION, e).sendToTarget();
            } finally {
                if (network != null) {
                    processDisconnect(socketDevice);
                }
            }
        }
    }

    private void processRequest(MessageAdHoc request) throws IOException {
        handler.obtainMessage(Service.MESSAGE_READ, request).sendToTarget();
    }

    private void processDisconnect(ISocket socketDevice) {
        if (socketDevice instanceof AdHocSocketWifi) {
            // Notify handler
            handler.obtainMessage(Service.CONNECTION_ABORTED,
                    new RemoteWifiConnection(network.getISocket().getRemoteSocketAddress()))
                    .sendToTarget();
        } else {
            // Get Socket
            BluetoothSocket socket = (BluetoothSocket) network.getISocket().getSocket();
            // Notify handler
            handler.obtainMessage(Service.CONNECTION_ABORTED,
                    new RemoteBtConnection(socket.getRemoteDevice().getAddress(),
                            socket.getRemoteDevice().getName())).sendToTarget();
        }

        // Remove client from hashmap
        listSocketDevice.removeActiveConnexion(socketDevice);

        // Close network
        try {
            network.closeConnection();
        } catch (IOException e) {
            handler.obtainMessage(Service.CATH_EXCEPTION, e).sendToTarget();
        }
    }

    String getNameThread() {
        return name;
    }
}
