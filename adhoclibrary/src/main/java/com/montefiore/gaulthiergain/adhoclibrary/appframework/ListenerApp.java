package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;

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
    void onReceivedData(String senderName, String senderAddress, Object pdu);

    void onSendData(String senderName, String senderAddress, Object pdu);

    /**
     * Callback when exception occurs.
     *
     * @param e an Exception object which represents the exception.
     */
    void traceException(Exception e);

    void onConnectionClosed(String remoteAddress, String remoteName);

    void onConnection(String remoteAddress, String remoteName);

    void onConnectionFailed(String remoteName);
}
