package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceMessageListener;
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
public class WifiServer extends ServiceServer {

    /**
     * Constructor
     *
     * @param verbose         a boolean value to set the debug/verbose mode.
     * @param json            a boolean value to use json or bytes in network transfer.
     * @param serviceMessageListener a serviceMessageListener object which serves as callback functions.
     */
    public WifiServer(boolean verbose, boolean json,
                      ServiceMessageListener serviceMessageListener) {
        super(verbose, json, serviceMessageListener);
    }

    /**
     * Method allowing to listen for incoming wifi connections.
     *
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public void listen(ServiceConfig config) throws IOException {

        // Cancel any thread currently running a connection
        if (threadListen != null) {
            threadListen.cancel();
            threadListen = null;
        }

        if(config.getNbThreads() > 0) {

            // Start thread Listening
            threadListen = new ThreadServer(handler, config.getNbThreads(), v, config.getServerPort(),
                    new ListSocketDevice(json));
            threadListen.start();

            if (v) Log.d(TAG, "Listening on port: " + config.getServerPort());

            // Update state
            setState(STATE_LISTENING);
        }
    }
}
