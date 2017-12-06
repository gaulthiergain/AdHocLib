package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

/**
 * Created by gaulthiergain on 11/11/17.
 */

public class BluetoothService extends Service {

    protected final String TAG = "[AdHoc][Bluet.Service]";
    private final MessageListener messageListener;

    BluetoothService(Context context, boolean verbose, MessageListener messageListener) {
        super(verbose, context);
        this.messageListener = messageListener;
        this.setState(STATE_NONE);
    }

    @SuppressLint("HandlerLeak")
    protected final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MESSAGE_READ:
                    if (v) Log.d(TAG, "BLUETOOTH-MESSAGE_READ");
                    messageListener.onMessageReceived((MessageAdHoc) msg.obj);
                    break;
                case MESSAGE_WRITE:
                    if (v) Log.d(TAG, "BLUETOOTH-MESSAGE_WRITE");
                    messageListener.onMessageSent((MessageAdHoc) msg.obj);
                    break;
                case CONNECTION_ABORTED:
                    if (v) Log.d(TAG, "BLUETOOTH-CONNECTION_ABORTED");
                    String handleConnectionAborted[] = (String[]) msg.obj;
                    messageListener.onConnectionClosed(handleConnectionAborted[0], handleConnectionAborted[1]);
                    break;
                case CONNECTION_PERFORMED:
                    if (v) Log.d(TAG, "BLUETOOTH-CONNECTION_PERFORMED");
                    String handleConnectionPerformed[] = (String[]) msg.obj;
                    messageListener.onConnection(handleConnectionPerformed[0], handleConnectionPerformed[1]);
                    break;
                default:
                    if (v) Log.d(TAG, "BLUETOOTH-default");
            }
        }
    };
}
