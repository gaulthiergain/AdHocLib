package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager.ListSocketDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager.ThreadServer;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class defines the server's logic for wifi implementation. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class WifiServiceServer extends ServiceServer {

    /**
     * Constructor
     *
     * @param verbose         a boolean value to set the debug/verbose mode.
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param messageListener a messageListener object which serves as callback functions.
     */
    public WifiServiceServer(boolean verbose, Context context, MessageListener messageListener) {
        super(verbose, context, messageListener);
    }

    /**
     * Method allowing to listen for incoming wifi connections.
     *
     * @param nbThreads a short value to determine the number of threads managed by the
     *                  server.
     * @param port      an integer value to set the listening port number.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public void listen(short nbThreads, int port) throws IOException {
        if (v) Log.d(TAG, "Listening()");

        // Cancel any thread currently running a connection
        if (threadListen != null) {
            threadListen.cancel();
            threadListen = null;
        }

        // Start thread Listening
        threadListen = new ThreadServer(handler, nbThreads, true, port, new ListSocketDevice());
        threadListen.start();

        // Update state
        setState(STATE_LISTENING);

        if (v) Log.d(TAG, "Listening on port: " + port);
    }

    public ConcurrentHashMap<String, NetworkObject> getActiveConnections() {
        return threadListen.getActiveConnexion();
    }
}
