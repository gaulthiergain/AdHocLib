package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager.ThreadServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServer;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class defines the server's logic and methods and aims to serve as a common interface for
 * {@link BluetoothServer} and
 * {@link WifiServer} classes. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public abstract class ServiceServer extends Service {

    protected static final String TAG = "[AdHoc][ServiceServer]";
    protected ThreadServer threadListen;

    /**
     * Constructor
     *
     * @param verbose                a boolean value to set the debug/verbose mode.
     * @param json                   a boolean value to use json or bytes in network transfer.
     * @param serviceMessageListener a serviceMessageListener object which contains callback functions.
     */
    public ServiceServer(boolean verbose, boolean json, ServiceMessageListener serviceMessageListener) {
        super(verbose, json, serviceMessageListener);
    }

    /**
     * Method allowing to launch a server to handle incoming connections in background.
     * This method is abstract and must be implemented by specialized classes.
     *
     * @param serviceConfig a ServiceConfig object which contains different parameters to setup server.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public abstract void listen(ServiceConfig serviceConfig) throws IOException;

    /**
     * Method allowing to stop the listening thread.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void stopListening() throws IOException {

        if (threadListen != null) {
            if (v) Log.d(TAG, "Stop listening");
            threadListen.cancel();
            threadListen = null;
            setState(STATE_NONE);
        }
    }

    /**
     * Method allowing to return the active connections managed by the server.
     *
     * @return a ConcurrentHashMap<String, SocketManager> which maps a remote device with a
     * SocketManager (socket).
     */
    public ConcurrentHashMap<String, SocketManager> getActiveConnections() {
        return threadListen.getActiveConnexion();
    }
}
