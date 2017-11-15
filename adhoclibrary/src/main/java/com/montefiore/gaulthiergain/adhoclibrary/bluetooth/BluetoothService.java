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
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTENING = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    public static final int STATE_LISTENING_CONNECTED = 4;

    public static final int MESSAGE_STATE_CHANGE = 5;
    public static final int MESSAGE_READ = 6;
    public static final int MESSAGE_WRITE = 7;

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

        // Give the new state to the Handler so the UI Activity can update TODO
        // mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    public int getState() {
        return state;
    }

    @SuppressLint("HandlerLeak")
    protected final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d("[AdHoc]", "WITHIN handleMessage" + msg);
            switch (msg.what) {

                case MESSAGE_READ:
                    //TODO CHANGE NULL HERE
                    messageListener.onMessageReceived((String) msg.obj, null);
                    Log.d(TAG, "RCVD HANDLER: " + msg.arg1);
                    break;
                case MESSAGE_WRITE:
                    messageListener.onMessageSent((String) msg.obj);
                    break;
            }
        }
    };
}
