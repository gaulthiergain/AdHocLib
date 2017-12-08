package com.montefiore.gaulthiergain.adhoclibrary.service;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

/**
 * Created by gaulthiergain on 8/12/17.
 */

public abstract class ServiceClient extends Service {

    protected NetworkObject network;
    private ListenServiceThread threadListening;
    protected final boolean background;

    public ServiceClient(boolean verbose, Context context, MessageListener messageListener, boolean background) {
        super(verbose, context, messageListener);
        this.background = background;
    }

    public void disconnect() throws NoConnectionException {
        if (v) Log.d(TAG, "disconnect()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        } else {
            if (state == STATE_CONNECTED) {
                network.closeConnection();
            } else if (state == STATE_LISTENING_CONNECTED) {
                stopListeningInBackground();
            }

            // Update the state of the connection
            setState(STATE_NONE);
        }
    }

    public void send(MessageAdHoc msg) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "send()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        } else {
            // Send message to remote device
            network.sendObjectStream(msg);

            // Notify handler
            handler.obtainMessage(Service.MESSAGE_WRITE, msg).sendToTarget();
        }
    }

    private void stopListeningInBackground() {
        if (v) Log.d(TAG, "stopListeningInBackground()");

        if (state == STATE_LISTENING_CONNECTED) {
            // Cancel any thread currently running a connection
            if (threadListening != null) {
                threadListening.cancel();
                threadListening = null;
            }
            // Update the state of the connection
            setState(STATE_CONNECTED);
        }
    }

    protected void listenInBackground() throws NoConnectionException, IOException {
        if (v) Log.d(TAG, "listenInBackground()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        } else {
            // Cancel any thread currently running a connection
            if (threadListening != null) {
                threadListening.cancel();
                threadListening = null;
            }

            setState(STATE_LISTENING_CONNECTED);

            // Start the thread to connect with the given device
            threadListening = new ListenServiceThread(network, handler);
            threadListening.start();
        }
    }
}
