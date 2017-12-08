package com.montefiore.gaulthiergain.adhoclibrary.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.util.logging.Handler;

/**
 * Created by gaulthiergain on 6/12/17.
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
    public static final int BROADCAST_WRITE = 7;              // broadcast sent

    // Constants for connection
    public static final int CONNECTION_ABORTED = 8;         // connection aborted
    public static final int CONNECTION_PERFORMED = 9;       // connection performed

    protected int state;
    protected final boolean v;
    protected final Context context;
    protected final String TAG = "[AdHoc][" + getClass().getName() + "]";

    private final MessageListener messageListener;

    public Service(boolean verbose, Context context, MessageListener messageListener) {
        this.v = verbose;
        this.context = context;
        this.messageListener = messageListener;
    }

    protected void setState(int state) {
        if (v) Log.d(TAG, "setState() " + state + " -> " + state);
        this.state = state;
    }

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
                default:
                    if (v) Log.d(TAG, "default");
            }
        }
    };
}
