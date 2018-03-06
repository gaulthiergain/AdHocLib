package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice.AbstractRemoteDevice;

/**
 * <p>This interface allows to define callback functions for messages and connection handling.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface MessageListener extends MessageMainListener {

    void onConnectionClosed(AbstractRemoteDevice remoteDevice);

    void onConnection(AbstractRemoteDevice remoteDevice);
}
