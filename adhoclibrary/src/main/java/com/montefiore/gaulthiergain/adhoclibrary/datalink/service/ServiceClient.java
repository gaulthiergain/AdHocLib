package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiClient;

import java.io.IOException;
import java.util.Random;

/**
 * <p>This class defines the client's logic and methods and aims to serve as a common interface for
 * {@link BluetoothClient} and
 * {@link WifiClient} classes. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public abstract class ServiceClient extends Service {

    private static final short LOW = 1500;
    private static final short HIGH = 2500;
    protected static final String TAG = "[AdHoc][ServiceClient]";

    protected SocketManager network;
    protected final int timeOut;
    protected final short attempts;

    private long backOffTime;
    private ListenServiceThread threadListening;

    /**
     * Constructor
     *
     * @param verbose                a boolean value to set the debug/verbose mode.
     * @param timeOut                an integer value which represents the time-out of a connection.
     * @param attempts               a short value which represents the number of attempts.
     * @param json                   a boolean value to use json or bytes in network transfer.
     * @param serviceMessageListener a serviceMessageListener object which contains callback functions.
     */
    public ServiceClient(boolean verbose, int timeOut, short attempts, boolean json,
                         ServiceMessageListener serviceMessageListener) {
        super(verbose, json, serviceMessageListener);
        this.timeOut = timeOut;
        this.attempts = attempts;
        this.backOffTime = (long) new Random().nextInt(HIGH - LOW) + LOW;
    }

    /**
     * Method allowing to send a message to the remote host.
     *
     * @param msg a MessageAdHoc object which represents the message to send through the network.
     * @throws NoConnectionException signals that a No Connection Exception exception has occurred.
     * @throws IOException           signals that an I/O exception of some sort has occurred.
     */
    public void send(MessageAdHoc msg) throws IOException, NoConnectionException {

        if (v) Log.d(TAG, "send()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        } else {
            // Send message to remote device
            network.sendMessage(msg);
        }
    }

    /**
     * Method allowing to listen in background to receive messages from remote hosts.
     *
     * @throws NoConnectionException signals that a No Connection Exception exception has occurred.
     * @throws IOException           signals that an I/O exception of some sort has occurred.
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
        }
    }

    /**
     * Method allowing to define a connection time-out backOff time.
     *
     * @return a long value which represents a backOffTime.
     */
    protected long getBackOffTime() {
        return (backOffTime *= 2);
    }

    /**
     * Method allowing to stop the background listening process.
     */
    private void stopListeningInBackground() throws IOException {

        if (v) Log.d(TAG, "stopListeningInBackground()");

        if (state == STATE_CONNECTED) {
            // Cancel any thread currently running a connection
            if (threadListening != null) {
                threadListening.cancel();
                threadListening = null;
            }
        }
    }
}
