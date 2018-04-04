package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceConfig;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager.ListSocketDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager.ThreadServer;

import java.io.IOException;

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
     * @param json            a boolean value to use json or bytes in network transfer.
     * @param messageListener a messageListener object which serves as callback functions.
     */
    public WifiServiceServer(boolean verbose, Context context, boolean json,
                             MessageListener messageListener) {
        super(verbose, context, json, messageListener);
    }

    /**
     * Method allowing to listen for incoming wifi connections.
     *
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public void listen(ServiceConfig config) throws IOException {
        if (v) Log.d(TAG, "Listening()");

        // Cancel any thread currently running a connection
        if (threadListen != null) {
            threadListen.cancel();
            threadListen = null;
        }

        // Start thread Listening
        threadListen = new ThreadServer(handler, config.getNbThreads(), v, config.getServerPort(),
                new ListSocketDevice(json));
        threadListen.start();

        // Update state
        setState(STATE_LISTENING);

        if (v) Log.d(TAG, "Listening on port: " + config.getServerPort());
    }
}
