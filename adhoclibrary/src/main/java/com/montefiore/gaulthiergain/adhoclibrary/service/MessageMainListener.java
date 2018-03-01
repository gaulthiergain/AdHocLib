package com.montefiore.gaulthiergain.adhoclibrary.service;

import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

/**
 * <p>This interface allows to define callback functions for messages and connection handling.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface MessageMainListener {
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
}
