package com.montefiore.gaulthiergain.adhoclibrary.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

/**
 * <p>This class defines the constants for connection states and message handling and aims to serve
 * as a common interface for service {@link ServiceClient} and {@link ServiceServer} classes. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public abstract class Service {

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;                 // no connection
    public static final int STATE_LISTENING = 1;            // listening for incoming connections
    public static final int STATE_CONNECTING = 2;           // initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;            // connected to a remote device
    public static final int STATE_LISTENING_CONNECTED = 4;  // connected to a remote device and listening

    // Constants for message handling
    public static final int MESSAGE_READ = 5;               // message received
    public static final int MESSAGE_WRITE = 6;              // message sent
    public static final int FORWARD = 7;                    // broadcast sent

    // Constants for connection
    public static final int CONNECTION_ABORTED = 8;         // connection aborted
    public static final int CONNECTION_PERFORMED = 9;       // connection performed

    protected int state;
    protected final boolean v;
    protected final Context context;
    protected final String TAG = "[AdHoc][Service]";

    private final MessageListener messageListener;

    /**
     * Constructor
     *
     * @param verbose         a boolean value to set the debug/verbose mode.
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param messageListener a messageListener object which serves as callback function.
     */
    Service(boolean verbose, Context context, MessageListener messageListener) {
        this.v = verbose;
        this.context = context;
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
    protected final android.os.Handler handler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_READ:
                    if (v) Log.d(TAG, "MESSAGE_READ");
                    messageListener.onMessageReceived((MessageAdHoc) msg.obj);
                    break;
                case MESSAGE_WRITE:
                    if (v) Log.d(TAG, "MESSAGE_WRITE");
                    messageListener.onMessageSent((MessageAdHoc) msg.obj);
                    break;
                case CONNECTION_ABORTED:
                    if (v) Log.d(TAG, "CONNECTION_ABORTED");
                    String handleConnectionAborted[] = (String[]) msg.obj;
                    messageListener.onConnectionClosed(handleConnectionAborted[0], handleConnectionAborted[1]);
                    break;
                case CONNECTION_PERFORMED:
                    if (v) Log.d(TAG, "CONNECTION_PERFORMED");
                    String handleConnectionPerformed[] = (String[]) msg.obj;
                    messageListener.onConnection(handleConnectionPerformed[0], handleConnectionPerformed[1], handleConnectionPerformed[1]);
                    break;
                case FORWARD:
                    if (v) Log.d(TAG, "FORWARD");
                    messageListener.onForward((MessageAdHoc) msg.obj);
                    break;
                default:
                    if (v) Log.d(TAG, "default");
            }
        }
    };
}
