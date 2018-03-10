package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.AbstractRemoteConnection;

/**
 * <p>This interface allows to define callback functions for messages and connection handling.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface MessageListener extends MessageMainListener {

    void onConnectionClosed(AbstractRemoteConnection remoteDevice);

    void onConnection(AbstractRemoteConnection remoteDevice);
}
