package com.montefiore.gaulthiergain.adhoclibrary.service;

import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

/**
 * <p>This interface allows to define callback functions for messages and connection handling.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface MessageListener {
    /**
     * Callback when message is received.
     *
     * @param message a MessageAdHoc object which defines the message.
     */
    void onMessageReceived(MessageAdHoc message);

    /**
     * Callback when message is sent.
     *
     * @param message a MessageAdHoc object which defines the message.
     */
    void onMessageSent(MessageAdHoc message);

    /**
     * Callback when message is forwarded.
     *
     * @param message a MessageAdHoc object which defines the message.
     */
    void onForward(MessageAdHoc message);

    /**
     * Callback when connection is closed.
     *
     * @param deviceName    a String value which represents the remote device's name.
     * @param deviceAddress a String value which represents the remote device's address.
     */
    void onConnectionClosed(String deviceName, String deviceAddress);

    /**
     * Callback when connection is performed.
     *
     * @param deviceName    a String value which represents the remote device's name.
     * @param deviceAddress a String value which represents the remote device's address.
     * @param localAddress  a String value which represents the local device's address.
     */
    void onConnection(String deviceName, String deviceAddress, String localAddress);
}
