package com.montefiore.gaulthiergain.adhoclibrary.wifi;

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

public class WifiService extends Service {

    protected final String TAG = "[AdHoc][WifiService]";
    private final WifiMessageListener messageListener;

    WifiService(Context context, boolean verbose, WifiMessageListener messageListener) {
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
                    if (v) Log.d(TAG, "WIFI-MESSAGE_READ");
                    messageListener.onMessageReceived((MessageAdHoc) msg.obj);
                    break;
                case MESSAGE_WRITE:
                    if (v) Log.d(TAG, "WIFI-MESSAGE_WRITE");
                    messageListener.onMessageSent((MessageAdHoc) msg.obj);
                    break;
                case CONNECTION_ABORTED:
                    if (v) Log.d(TAG, "WIFI-CONNECTION_ABORTED");
                    String handleConnectionAborted = (String) msg.obj;
                    messageListener.onConnectionClosed(handleConnectionAborted);
                    break;
                case CONNECTION_PERFORMED:
                    if (v) Log.d(TAG, "WIFI-CONNECTION_PERFORMED");
                    String[] handleConnectionPerformed = (String[]) msg.obj;
                    messageListener.onConnection(handleConnectionPerformed[0], handleConnectionPerformed[1]);
                    break;
                default:
                    if (v) Log.d(TAG, "WIFI-default");
            }
        }
    };
}
