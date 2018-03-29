package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;

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
    void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice);

    /**
     * Callback when a RREQ message is received.
     *
     * @param senderName
     * @param senderAddress
     * @param pdu
     */
    void receivedData(String senderName, String senderAddress, Object pdu);

    /**
     * Callback when exception occurs.
     *
     * @param e an Exception object which represents the exception.
     */
    void catchException(Exception e);

    void onConnectionClosed(String remoteName, String remoteAddress);

    void onConnection(String remoteName, String remoteAddress);
}
