package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvAbstractException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

/**
 * <p>This class defines the constants for connection states and message handling and aims to serve
 * as a common interface for service {@link ServiceClient} and {@link ServiceServer} classes. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public abstract class Service {

    protected final String TAG = "[AdHoc][Service]";

    // Constant for type
    public static final byte WIFI = 0;
    public static final byte BLUETOOTH = 1;

    // Constants that indicate the current connection state
    protected static final byte STATE_NONE = 0;                 // no connection
    protected static final byte STATE_LISTENING = 1;            // listening for incoming connections
    protected static final byte STATE_CONNECTING = 2;           // initiating an outgoing connection
    protected static final byte STATE_CONNECTED = 3;            // connected to a remote device
    protected static final byte STATE_LISTENING_CONNECTED = 4;  // connected to a remote device and listening

    // Constants for message handling
    public static final byte MESSAGE_READ = 5;                   // message received

    // Constants for connection
    public static final byte CONNECTION_ABORTED = 6;             // connection aborted
    public static final byte CONNECTION_PERFORMED = 7;           // connection performed
    public static final byte CATH_EXCEPTION = 8;                 // catch exception
    public static final byte CONNECTION_FAILED = 9;              // connection failed

    protected int state;
    protected final boolean v;
    protected final boolean json;
    protected final Context context;

    private final MessageListener messageListener;

    /**
     * Constructor
     *
     * @param verbose         a boolean value to set the debug/verbose mode.
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param json            a boolean value to use json or bytes in network transfer.
     * @param messageListener a messageListener object which serves as callback functions.
     */
    Service(boolean verbose, Context context, boolean json, MessageListener messageListener) {
        this.v = verbose;
        this.context = context;
        this.json = json;
        this.messageListener = messageListener;
    }

    /**
     * Method allowing to defines the state of a connection.
     *
     * @param state a integer values which defines the state of a connection.
     */
    protected void setState(int state) {
        if (v) Log.d(TAG, "setState() " + state + " -> " + state);
        this.state = state;
    }

    /**
     * Method allowing to get the state of a connection.
     *
     * @return a integer values which defines the state of a connection.
     */
    public int getState() {
        return state;
    }

    @SuppressLint("HandlerLeak")
    protected final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_READ:
                    if (v) Log.d(TAG, "MESSAGE_READ");
                    messageListener.onMessageReceived((MessageAdHoc) msg.obj);
                    break;
                case CONNECTION_ABORTED:
                    if (v) Log.d(TAG, "CONNECTION_ABORTED");
                    messageListener.onConnectionClosed((String) msg.obj);
                    break;
                case CONNECTION_PERFORMED:
                    if (v) Log.d(TAG, "CONNECTION_PERFORMED");
                    messageListener.onConnection((String) msg.obj);
                    break;
                case CONNECTION_FAILED:
                    if (v) Log.d(TAG, "CONNECTION_FAILED");
                    messageListener.onConnectionFailed((Exception) msg.obj);
                    break;
                case CATH_EXCEPTION:
                    Exception e = (Exception) msg.obj;
                    if (v) Log.e(TAG, "CATH_EXCEPTION: " + e.getMessage());
                    break;
            }
        }
    };
}
