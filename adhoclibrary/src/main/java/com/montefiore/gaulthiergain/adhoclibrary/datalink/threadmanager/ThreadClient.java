package com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager;

import android.os.Handler;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.ISocket;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;

import java.io.EOFException;
import java.io.IOException;

class ThreadClient extends Thread {

    private final ListSocketDevice listSocketDevice;
    private final String name;
    private final Handler handler;
    private SocketManager network = null;

    ThreadClient(ListSocketDevice listSocketDevice, String name, Handler handler) {
        this.listSocketDevice = listSocketDevice;
        this.name = name;
        this.handler = handler;
    }

    public void run() {
        while (!isInterrupted()) {
            try {
                ISocket socketDevice = listSocketDevice.getSocketDevice();
                network = listSocketDevice.getActiveConnection().get(socketDevice.getRemoteSocketAddress());
                MessageAdHoc messageAdHoc;
                while (true) {
                    messageAdHoc = network.receiveMessage();
                    if (messageAdHoc == null) {
                        handler.obtainMessage(Service.MESSAGE_EXCEPTION, new Exception("NULL message")).sendToTarget();
                    } else {
                        processRequest(messageAdHoc);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (EOFException e) {
                handler.obtainMessage(Service.LOG_EXCEPTION, e).sendToTarget();
            } catch (IOException e) {
                handler.obtainMessage(Service.LOG_EXCEPTION, e).sendToTarget();
            } catch (ClassNotFoundException e) {
                handler.obtainMessage(Service.MESSAGE_EXCEPTION, e).sendToTarget();
            } finally {
                if (network != null) {
                    processDisconnect();
                }
            }
        }
    }

    private void processRequest(MessageAdHoc request) {
        handler.obtainMessage(Service.MESSAGE_READ, request).sendToTarget();
    }

    private void processDisconnect() {
        // Notify handler
        handler.obtainMessage(Service.CONNECTION_ABORTED,
                network.getRemoteSocketAddress()).sendToTarget();

        // Remove client from hashmap
        listSocketDevice.removeActiveConnexion(network.getRemoteSocketAddress());

        // Close network
        try {
            network.closeConnection();
            network = null;
        } catch (IOException e) {
            // Network is already closed
            network = null;
        }
    }

    String getNameThread() {
        return name;
    }
}
