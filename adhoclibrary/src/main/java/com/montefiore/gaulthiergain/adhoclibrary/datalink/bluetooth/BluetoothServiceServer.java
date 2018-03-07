package com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.MaxThreadReachedException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
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

    private static final short MAX_THREAD = 8; //todo check if good value

    /**
     * Constructor
     *
     * @param verbose         a boolean value to set the debug/verbose mode.
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param messageListener a messageListener object which serves as callback functions.
     */
    public BluetoothServiceServer(boolean verbose, Context context, MessageListener messageListener) {
        super(verbose, context, messageListener);
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
     * @throws IOException               Signals that an I/O exception of some sort has occurred.
     * @throws MaxThreadReachedException Signals that the maximum number of threads is reached.
     */
    public void listen(short nbThreads, boolean secure, String name, BluetoothAdapter bluetoothAdapter,
                       UUID uuid) throws IOException, MaxThreadReachedException {
        if (v) Log.d(TAG, "Listening()");

        // Cancel any thread currently running a connection
        if (threadListen != null) {
            threadListen.cancel();
            threadListen = null;
        }

        if (nbThreads >= MAX_THREAD) {
            throw new MaxThreadReachedException("Number of threads must be smaller than " + MAX_THREAD);
        }

        // Start thread Listening
        threadListen = new ThreadServer(handler, nbThreads, true, secure, name, bluetoothAdapter, uuid,
                new ListSocketDevice());
        threadListen.start();

        // Update state
        setState(STATE_LISTENING);

        if (v) Log.d(TAG, "Listening on device: " + uuid.toString());
    }

    /**
     * Method allowing to return the active connections managed by the server.
     *
     * @return a ConcurrentHashMap<String, NetworkObject> which maps a remote device with a
     * NetworkObject (socket).
     */
    public ConcurrentHashMap<String, NetworkObject> getActiveConnections() {
        return threadListen.getActiveConnexion();
    }
}
