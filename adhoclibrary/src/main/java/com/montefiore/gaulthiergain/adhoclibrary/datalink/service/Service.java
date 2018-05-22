package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;

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
    public static final byte STATE_NONE = 0;                 // no connection
    public static final byte STATE_LISTENING = 1;            // listening for incoming connections
    public static final byte STATE_CONNECTING = 2;           // initiating an outgoing connection
    public static final byte STATE_CONNECTED = 3;            // connected to a remote device

    // Constants for message handling
    public static final byte MESSAGE_READ = 5;                   // message received

    // Constants for connection
    public static final byte CONNECTION_ABORTED = 6;             // connection aborted
    public static final byte CONNECTION_PERFORMED = 7;           // connection performed
    public static final byte CONNECTION_FAILED = 8;              // connection failed

    public static final byte LOG_EXCEPTION = 9;                  // log exception
    public static final byte MESSAGE_EXCEPTION = 10;             // catch message exception
    public static final int NETWORK_UNREACHABLE = 11;

    protected int state;
    protected final boolean v;
    protected final boolean json;

    private final ServiceMessageListener serviceMessageListener;

    /**
     * Constructor
     *
     * @param verbose                a boolean value to set the debug/verbose mode.
     * @param json                   a boolean value to use json or bytes in network transfer.
     * @param serviceMessageListener a serviceMessageListener object which serves as callback functions.
     */
    Service(boolean verbose, boolean json, ServiceMessageListener serviceMessageListener) {
        this.state = 0;
        this.v = verbose;
        this.json = json;
        this.serviceMessageListener = serviceMessageListener;
    }

    /**
     * Method allowing to defines the state of a connection.
     *
     * @param state an integer values which defines the state of a connection.
     */
    protected void setState(int state) {
        if (v) Log.d(TAG, "setState() " + this.state + " -> " + state);
        this.state = state;
    }

    /**
     * Method allowing to get the state of a connection.
     *
     * @return an integer values which defines the state of a connection.
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
                    serviceMessageListener.onMessageReceived((MessageAdHoc) msg.obj);
                    break;
                case CONNECTION_ABORTED:
                    if (v) Log.d(TAG, "CONNECTION_ABORTED");
                    serviceMessageListener.onConnectionClosed((String) msg.obj);
                    break;
                case CONNECTION_PERFORMED:
                    if (v) Log.d(TAG, "CONNECTION_PERFORMED");
                    serviceMessageListener.onConnection((String) msg.obj);
                    break;
                case CONNECTION_FAILED:
                    if (v) Log.d(TAG, "CONNECTION_FAILED");
                    serviceMessageListener.onConnectionFailed((Exception) msg.obj);
                    break;
                case MESSAGE_EXCEPTION:
                    if (v) Log.e(TAG, "MESSAGE_EXCEPTION");
                    serviceMessageListener.onMsgException((Exception) msg.obj);
                    break;
                case LOG_EXCEPTION:
                    if (v) Log.w(TAG, "LOG_EXCEPTION: " + ((Exception) msg.obj).getMessage());
                    break;
            }
        }
    };
}
