package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;

/**
 * <p>This interface allows to define callback functions for messages and connection handling.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface ServiceMessageListener {

    /**
     * Method allowing to notify when message is received.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     */
    void onMessageReceived(MessageAdHoc message);

    /**
     * Method allowing to notify when a connection is closed with a remote peer.
     *
     * @param remoteAddress a String value which represents the address of the remote peer.
     */
    void onConnectionClosed(String remoteAddress);

    /**
     * Method allowing to notify when a connection is performed with a remote peer.
     *
     * @param remoteAddress a String value which represents the address of the remote peer.
     */
    void onConnection(String remoteAddress);

    /**
     * Method allowing to notify when an exception has occurred during the connection process.
     *
     * @param e an Exception object which contains the exception which has been caught during
     *          the connection process.
     */
    void onConnectionFailed(Exception e);

    /**
     * Method allowing to notify when an exception has occurred during the message process.
     *
     * @param e an Exception object which contains the exception which has been caught during
     *          the message process.
     */
    void onMsgException(Exception e);
}
