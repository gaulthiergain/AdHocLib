package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.Random;

/**
 * <p>This class defines the client's logic and methods and aims to serve as a common interface for
 * {@link com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceClient} and
 * {@link com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceClient} classes. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public abstract class ServiceClient extends Service {

    protected static final short LOW = 1500;
    protected static final short HIGH = 2500;
    protected static final String TAG = "[AdHoc][ServiceClient]";

    protected SocketManager network;
    protected final short attempts;
    protected final boolean background;

    private long backOffTime;
    private ListenServiceThread threadListening;

    /**
     * Constructor
     *
     * @param verbose         a boolean value to set the debug/verbose mode.
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param attempts        a short value which represents the number of attempts.
     * @param json            a boolean value to use json or bytes in network transfer.
     * @param background      a boolean value which defines if the service must listen messages
     *                        to background.
     * @param messageListener a messageListener object which serves as callback functions.
     */
    public ServiceClient(boolean verbose, Context context, short attempts, boolean json,
                         boolean background, MessageListener messageListener) {
        super(verbose, context, json, messageListener);
        this.attempts = attempts;
        this.background = background;
        this.backOffTime = (long) new Random().nextInt(HIGH - LOW) + LOW;
    }

    /**
     * Method allowing to send a message to the remote host.
     *
     * @param msg a MessageAdHoc object which defines the message.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     */
    public void send(MessageAdHoc msg) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "send()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        } else {
            // Send message to remote device
            network.sendMessage(msg);

            // Notify handler
            handler.obtainMessage(Service.MESSAGE_WRITE, msg).sendToTarget();
        }
    }

    /**
     * Method allowing to stop the background listening process.
     */
    private void stopListeningInBackground() throws IOException {
        if (v) Log.d(TAG, "stopListeningInBackground()");

        if (state == STATE_LISTENING_CONNECTED) {
            // Cancel any thread currently running a connection
            if (threadListening != null) {
                threadListening.cancel();
                threadListening = null;
            }
        }
    }

    /**
     * Method allowing to listen in background to receive messages from remote hosts.
     *
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     */
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

            // Start the thread to connect with the given device
            threadListening = new ListenServiceThread(v, network, handler);
            threadListening.start();

            // Update the state
            setState(STATE_LISTENING_CONNECTED);
        }
    }

    protected long getBackOffTime() {
        return (backOffTime *= 2);
    }
}
