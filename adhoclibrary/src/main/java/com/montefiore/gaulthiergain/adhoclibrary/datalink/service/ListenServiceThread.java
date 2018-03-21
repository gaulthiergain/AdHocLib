package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.RemoteBtConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.RemoteWifiConnection;
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
    private final NetworkManager network;
    private final Handler handler;

    /**
     * @param verbose a boolean value to set the debug/verbose mode.
     * @param network a Network object which manages the connection with the remote host.
     * @param handler a Handler object which allows to send and process {@link android.os.Message}
     *                and Runnable objects associated with a thread's.
     */
    ListenServiceThread(boolean verbose, NetworkManager network, Handler handler) {
        this.v = verbose;
        this.network = network;
        this.handler = handler;
    }

    /**
     * Method allowing to receive messages in background from the remote connected host.
     */
    @Override
    public void run() {
        if (v) Log.d(TAG, "start Listening ...");

        MessageAdHoc message;
        while (true) {
            try {
                if (v) Log.d(TAG, "Waiting response from server ...");

                // Get response MessageAdHoc
                message = network.receiveMessage();
                if (v) Log.d(TAG, "Response: " + message);
                handler.obtainMessage(Service.MESSAGE_READ, message).sendToTarget();
            } catch (IOException e) {
                if (network.getISocket() != null) {

                    if (network.getISocket() instanceof AdHocSocketWifi) {
                        // Notify handler and set remote device address
                        handler.obtainMessage(Service.CONNECTION_ABORTED,
                                new RemoteWifiConnection(network.getISocket().getRemoteSocketAddress()))
                                .sendToTarget();
                    } else {
                        // Get Socket
                        BluetoothSocket socket = (BluetoothSocket) network.getISocket().getSocket();
                        // Notify handler and set remote device address and name
                        handler.obtainMessage(Service.CONNECTION_ABORTED,
                                new RemoteBtConnection(socket.getRemoteDevice().getAddress(),
                                        socket.getRemoteDevice().getName())).sendToTarget();
                    }

                    try {
                        network.closeConnection();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                break;
            } catch (ClassNotFoundException e) {
                handler.obtainMessage(Service.CATH_EXCEPTION, e).sendToTarget();
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
}
