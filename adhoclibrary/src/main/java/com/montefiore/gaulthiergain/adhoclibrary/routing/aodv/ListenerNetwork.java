package com.montefiore.gaulthiergain.adhoclibrary.routing.aodv;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.RemoteConnection;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DiscoveredDevice;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.util.HashMap;

/**
 * Created by gaulthiergain on 22/03/18.
 */

public interface ListenerNetwork {
    /**
     * Callback when the discovery is completed.
     *
     * @param mapAddressDevice
     */
    void onDiscoveryCompleted(HashMap<String, DiscoveredDevice> mapAddressDevice);

    /**
     * Callback when the getPairedDevices is completed.
     */
    void onPairedCompleted();

    /**
     * Callback when a RREQ message is received.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     */
    void receivedDATA(MessageAdHoc message);

    /**
     * Callback when exception occurs.
     *
     * @param e an Exception object which represents the exception.
     */
    void catchException(Exception e);

    void onConnectionClosed(RemoteConnection remoteDevice);

    void onConnection(RemoteConnection remoteDevice);
}
