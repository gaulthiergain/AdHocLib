package com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager.ListSocketDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager.ThreadServer;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class defines the server's logic for bluetooth implementation. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class BluetoothServiceServer extends ServiceServer {

    /**
     * Constructor
     *
     * @param verbose         a boolean value to set the debug/verbose mode.
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param json            a boolean value to use json or bytes in network transfer.
     * @param messageListener a messageListener object which serves as callback functions.
     */
    public BluetoothServiceServer(boolean verbose, Context context, boolean json,
                                  MessageListener messageListener) {
        super(verbose, context, json, messageListener);
    }

    /**
     * Method allowing to listen for incoming bluetooth connections.
     *
     * @param nbThreads        a short value to determine the number of threads managed by the
     *                         server.
     * @param secure           a boolean value to determine if the connection is secure.
     * @param name             a String value which represents the connection's name.
     * @param bluetoothAdapter a BluetoothAdapter object which represents the local device Bluetooth
     *                         adapter.
     * @param uuid             an UUID object which identify the physical device.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public void listen(short nbThreads, boolean secure, String name, BluetoothAdapter bluetoothAdapter,
                       UUID uuid) throws IOException {
        if (v) Log.d(TAG, "Listening()");

        // Cancel any thread currently running a connection
        if (threadListen != null) {
            threadListen.cancel();
            threadListen = null;
        }

        // Start thread Listening
        threadListen = new ThreadServer(handler, nbThreads, true, secure, name, bluetoothAdapter, uuid,
                new ListSocketDevice(json));
        threadListen.start();

        // Update state
        setState(STATE_LISTENING);

        if (v) Log.d(TAG, "Listening on device: " + uuid.toString());
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
