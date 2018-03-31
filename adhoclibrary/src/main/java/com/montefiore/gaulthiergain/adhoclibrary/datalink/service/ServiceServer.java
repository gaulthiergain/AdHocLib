package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager.ThreadServer;

import java.io.IOException;

/**
 * <p>This class defines the server's logic and methods and aims to serve as a common interface for
 * {@link com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceServer} and
 * {@link com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceServer} classes. </p>
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
     * @param verbose         a boolean value to set the debug/verbose mode.
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param json            a boolean value to use json or bytes in network transfer.
     * @param messageListener a messageListener object which serves as callback functions.
     */
    public ServiceServer(boolean verbose, Context context, boolean json, MessageListener messageListener) {
        super(verbose, context, json, messageListener);
    }

    /**
     * Method allowing to stop the listening thread.
     *
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public void stopListening() throws IOException {

        if (threadListen != null) {
            if (v) Log.d(TAG, "Stop listening");
            threadListen.cancel();
            threadListen = null;
            setState(STATE_NONE);
        }
    }
}
