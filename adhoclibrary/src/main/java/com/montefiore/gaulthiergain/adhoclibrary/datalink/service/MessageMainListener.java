package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

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
     * Callback when exception occurs.
     *
     * @param e an Exception object which represents the exception.
     */
    void catchException(Exception e);
}
