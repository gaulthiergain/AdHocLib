package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager.ThreadServer;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class defines the server's logic and methods and aims to serve as a common interface for
 * {@link com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceServer} and
 * {@link com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceServer} classes. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public abstract class ServiceServer extends Service {

    protected static final String TAG = "[AdHoc][ServiceServer]";
    protected ThreadServer threadListen;

    /**
     * Constructor
     *
     * @param verbose         a boolean value to set the debug/verbose mode.
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param messageListener a messageListener object which serves as callback functions.
     */
    public ServiceServer(boolean verbose, Context context, MessageListener messageListener) {
        super(verbose, context, messageListener);
    }

    /**
     * Method allowing to stop the listening thread.
     *
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public void stopListening() throws IOException {

        if (threadListen != null) {
            if (v) Log.d(TAG, "Stop listening");
            threadListen.cancel();
            threadListen = null;
            setState(STATE_NONE);
        }
    }

    /**
     * Method allowing to send a message to all connected devices except a particular one.
     *
     * @param message a MessageAdHoc object which defines the message.
     * @param address a String value which represents the sender's address.
     * @return a boolean value which represents the result of the sending.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     */
    private boolean _sendtoAllExcept(MessageAdHoc message, String address)
            throws NoConnectionException, IOException {

        // Get remote connection
        ConcurrentHashMap<String, NetworkManager> hashMap = threadListen.getActiveConnexion();

        if (hashMap.size() == 0) {
            // If no remote device, return false
            return false;
        }

        // Send message to all connected devices
        for (Map.Entry<String, NetworkManager> pairs : hashMap.entrySet()) {
            if (!pairs.getKey().equals(address)) {
                pairs.getValue().sendMessage(message);
                if (v) Log.d(TAG, "Send " + message + " to " + pairs.getKey());
            }
        }

        return true;
    }

    /**
     * Method allowing to send a message to all connected devices.
     *
     * @param message a MessageAdHoc object which defines the message.
     * @return a boolean value which represents the result of the sending.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     */
    private boolean _sendtoAll(MessageAdHoc message) throws IOException, NoConnectionException {

        // Get remote connection
        ConcurrentHashMap<String, NetworkManager> hashMap = threadListen.getActiveConnexion();
        if (hashMap.size() == 0) {
            // If no remote device, return false
            return false;
        }

        // Send message to all connected devices
        for (Map.Entry<String, NetworkManager> pairs : hashMap.entrySet()) {
            pairs.getValue().sendMessage(message);
            if (v) Log.d(TAG, "Send " + message + " to " + pairs.getKey());
        }

        return true;
    }

    /**
     * Method allowing to send a message to a particular devices.
     *
     * @param message a MessageAdHoc object which defines the message.
     * @param address a String value which represents the sender's address.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     */
    public void sendTo(MessageAdHoc message, String address) throws NoConnectionException, IOException {
        if (v) Log.d(TAG, "sendTo()");

        // Get remote connection
        ConcurrentHashMap<String, NetworkManager> hashMap = threadListen.getActiveConnexion();

        // Get associated socket
        NetworkManager network = hashMap.get(address);
        if (network == null) {
            throw new NoConnectionException("No remote connexion with " + address);
        } else {
            // Send message to connected device
            network.sendMessage(message);
            if (v)
                Log.d(TAG, "Send " + message + " to " + address);

            // Notify handler
            handler.obtainMessage(Service.MESSAGE_WRITE, message).sendToTarget();
        }
    }

    /**
     * Method allowing to forward a message to all connected devices except a particular one.
     *
     * @param message a MessageAdHoc object which defines the message.
     * @param address a String value which represents the sender's address.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     */
    public void forwardToAllExcept(MessageAdHoc message, String address) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "forwardToAllExcept()");

        if (!_sendtoAllExcept(message, address)) {
            throw new NoConnectionException("No remote connection");
        }

        // Notify handler
        handler.obtainMessage(Service.FORWARD, message).sendToTarget();
    }

    /**
     * Method allowing to forward a message to all connected devices.
     *
     * @param message a MessageAdHoc object which defines the message.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     */
    public void forwardToAll(MessageAdHoc message) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "forwardToAll()");

        if (!_sendtoAll(message)) {
            throw new NoConnectionException("No remote connection");
        }

        // Notify handler
        handler.obtainMessage(Service.FORWARD, message).sendToTarget();
    }

    /**
     * Method allowing to send a message to all connected devices except a particular one.
     *
     * @param message a MessageAdHoc object which defines the message..
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     */
    public void sendToAllExcept(MessageAdHoc message, String senderAddr)
            throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "sendToAllExcept()");

        if (!_sendtoAllExcept(message, senderAddr)) {
            throw new NoConnectionException("No remote connection");
        }

        // Notify handler
        handler.obtainMessage(Service.MESSAGE_WRITE, message).sendToTarget();
    }

    /**
     * Method allowing to send a message to all connected devices.
     *
     * @param message a MessageAdHoc object which defines the message.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     */
    public void sendToAll(MessageAdHoc message) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "sendToAll()");

        if (!_sendtoAll(message)) {
            throw new NoConnectionException("No remote connection");
        }

        // Notify handler
        handler.obtainMessage(Service.MESSAGE_WRITE, message).sendToTarget();
    }
}
