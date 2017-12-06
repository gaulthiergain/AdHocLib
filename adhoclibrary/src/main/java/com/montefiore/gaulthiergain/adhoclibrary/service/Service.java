package com.montefiore.gaulthiergain.adhoclibrary.service;

import android.content.Context;
import android.util.Log;

/**
 * Created by gaulthiergain on 6/12/17.
 */

public class Service {

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
    protected final boolean v;
    protected final String TAG = "[AdHoc][" + getClass().getName() + "]";
    protected final Context context;
    protected int state;

    public Service(boolean verbose, Context context) {
        this.v = verbose;
        this.context = context;
    }

    protected void setState(int state) {
        if (v) Log.d(TAG, "setState() " + state + " -> " + state);
        this.state = state;
    }

    public int getState() {
        return state;
    }
}
