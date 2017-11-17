package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetoothListener.MessageListener;

/**
 * Created by gaulthiergain on 11/11/17.
 */

public class BluetoothService {

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;                 // no connection
    public static final int STATE_LISTENING = 1;            // listening for incoming connections
    public static final int STATE_CONNECTING = 2;           // initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;            // connected to a remote device
    public static final int STATE_LISTENING_CONNECTED = 4;  // connected to a remote device and listening

    // Constants for message handling
    public static final int MESSAGE_READ = 5;               // message received
    public static final int MESSAGE_WRITE = 6;              // message sent

    // COnstants for connection
    public static final int CONNECTION_ABORTED = 7;         // connection aborted

    protected int state;
    protected final boolean v;
    protected final MessageListener messageListener;
    protected final String TAG = "[AdHoc][" + getClass().getName() + "]";
    protected final Context context;

    public BluetoothService(Context context, boolean verbose, MessageListener messageListener) {
        this.context = context;
        this.v = verbose;
        this.messageListener = messageListener;
        this.setState(STATE_NONE);
    }

    protected void setState(int state) {
        if (v) Log.d(TAG, "setState() " + state + " -> " + state);
        this.state = state;
    }

    public int getState() {
        return state;
    }

    @SuppressLint("HandlerLeak")
    protected final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case MESSAGE_READ:
                    if (v)  Log.d(TAG, "MESSAGE_READ");
                    String handleMessage[] = (String[]) msg.obj;
                    messageListener.onMessageReceived(handleMessage[0], handleMessage[1], handleMessage[2]);
                    break;
                case MESSAGE_WRITE:
                    if (v)  Log.d(TAG, "MESSAGE_WRITE");
                    messageListener.onMessageSent((String) msg.obj);
                    break;
                case CONNECTION_ABORTED:
                    if (v) Log.d(TAG, "CONNECTION_ABORTED");
                    messageListener.onConnectionClosed();
                    break;
            }
        }
    };
}
