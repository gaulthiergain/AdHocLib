package com.montefiore.gaulthiergain.adhoclibrary.applayer;

import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DiscoveredDevice;

import java.util.HashMap;

/**
 * Created by gaulthiergain on 10/03/18.
 */

public interface ListenerApp {
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
     * @param message an object which represents a message exchanged between nodes.
     */
    void receivedData(Object message);

    /**
     * Callback when exception occurs.
     *
     * @param e an Exception object which represents the exception.
     */
    void catchException(Exception e);

    void onConnectionClosed(String remoteName, String remoteAddress);

    void onConnection(String remoteName, String remoteAddress);
}
