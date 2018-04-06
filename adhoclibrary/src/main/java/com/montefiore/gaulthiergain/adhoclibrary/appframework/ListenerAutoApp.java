package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import java.io.IOException;

public interface ListenerAutoApp {
    /**
     * Callback when a Data message is received.
     *
     * @param senderName
     * @param senderAddress
     * @param pdu
     */
    void onReceivedData(String senderName, String senderAddress, Object pdu);

    void onConnection(String remoteAddress, String remoteName, int hops);

    void onConnectionFailed(Exception e);

    void onConnectionClosed(String remoteAddress, String remoteName);

    void onConnectionClosedFailed(Exception e);

    void processMsgException(Exception e);
}
