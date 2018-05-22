package com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager;

import android.os.Handler;
import android.os.Message;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.ISocket;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;

import java.io.EOFException;
import java.io.IOException;

/**
 * <p>This class represents runnable worker which manages the connection/communication with remote
 * devices.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
class ThreadClient extends Thread {

    private final ListSocketDevice listSocketDevice;
    private final String name;
    private final Handler handler;
    private SocketManager network = null;

    /**
     * Constructor
     *
     * @param listSocketDevice a listSocketDevice object which contains the clients managed by the
     *                         threadPool.
     * @param name             a String value which represents the connection's name.
     * @param handler          a Handler object which allows to send and process {@link Message}
     *                         and Runnable objects associated with a thread's.
     */
    ThreadClient(ListSocketDevice listSocketDevice, String name, Handler handler) {
        this.listSocketDevice = listSocketDevice;
        this.name = name;
        this.handler = handler;
    }

    @Override
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
                        handler.obtainMessage(Service.MESSAGE_READ, messageAdHoc).sendToTarget();
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

    /**
     * Method allowing to process a disconnection.
     */
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

    /**
     * Method allowing to return the name of the current thread.
     *
     * @return a String value which represents the name of the current thread.
     */
    String getNameThread() {
        return name;
    }
}
