package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

/**
 * <p>This interface allows to define callback functions for messages and connection handling.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface MessageListener extends MessageMainListener {

    void onConnectionClosed(String remoteAddress);

    void onConnection(String remoteAddress);

    void onConnectionFailed(String remoteAddress);
}
