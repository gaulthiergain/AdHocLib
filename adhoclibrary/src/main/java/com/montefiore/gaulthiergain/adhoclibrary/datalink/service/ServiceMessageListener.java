package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

/**
 * <p>This interface allows to define callback functions for messages and connection handling.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface ServiceMessageListener {

    /**
     * Callback when message is received.
     *
     * @param message a MessageAdHoc object which defines the message.
     */
    void onMessageReceived(MessageAdHoc message);

    void onConnectionClosed(String remoteAddress);

    void onConnection(String remoteAddress);

    void onConnectionFailed(Exception e);

    void onMsgException(Exception e);
}
