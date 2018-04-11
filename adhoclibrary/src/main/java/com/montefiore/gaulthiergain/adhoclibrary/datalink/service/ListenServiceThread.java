package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

/**
 * <p>This class allows to receive messages in background from the remote connected host.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
class ListenServiceThread extends Thread {
    private static final String TAG = "[AdHoc][ListenService]";
    private final boolean v;
    private final SocketManager network;
    private final Handler handler;

    /**
     * @param verbose a boolean value to set the debug/verbose mode.
     * @param network a Network object which manages the connection with the remote host.
     * @param handler a Handler object which allows to send and process {@link android.os.Message}
     *                and Runnable objects associated with a thread's.
     */
    ListenServiceThread(boolean verbose, SocketManager network, Handler handler) {
        this.v = verbose;
        this.network = network;
        this.handler = handler;
    }

    /**
     * Method allowing to receive messages in background from the remote connected host.
     */
    @Override
    public void run() {

        if (v) Log.d(TAG, "Listening message ...");

        while (true) {
            try {
                // Get MessageAdHoc
                MessageAdHoc messageAdHoc = network.receiveMessage();
                if (messageAdHoc == null) {
                    handler.obtainMessage(Service.MESSAGE_EXCEPTION, new Exception("NULL message")).sendToTarget();
                } else {
                    handler.obtainMessage(Service.MESSAGE_READ, messageAdHoc).sendToTarget();
                }
            } catch (IOException e) {
                if (network.getISocket() != null) {
                    processDisconnect();
                }
                break;
            } catch (ClassNotFoundException e) {
                handler.obtainMessage(Service.MESSAGE_EXCEPTION, e).sendToTarget();
            }
        }
    }

    /**
     * Method allowing to close the remote connection.
     */
    void cancel() throws IOException {
        if (network.getISocket() != null) {
            network.closeConnection();
        }
    }

    private void processDisconnect() {
        if (network.getISocket() instanceof AdHocSocketWifi) {
            // Notify handler and set remote device address
            handler.obtainMessage(Service.CONNECTION_ABORTED,
                    network.getISocket().getRemoteSocketAddress()).sendToTarget();
        } else {
            // Get Socket
            BluetoothSocket socket = (BluetoothSocket) network.getISocket().getSocket();
            // Notify handler and set remote device address and name
            handler.obtainMessage(Service.CONNECTION_ABORTED,
                    socket.getRemoteDevice().getAddress()).sendToTarget();
        }

        try {
            network.closeConnection();
        } catch (IOException e) {
            handler.obtainMessage(Service.LOG_EXCEPTION, e).sendToTarget();
        }
    }
}
