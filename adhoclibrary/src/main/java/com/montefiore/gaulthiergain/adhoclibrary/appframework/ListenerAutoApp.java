package com.montefiore.gaulthiergain.adhoclibrary.appframework;

public interface ListenerAutoApp {
    /**
     * Callback when a RREQ message is received.
     *
     * @param senderName
     * @param senderAddress
     * @param pdu
     */
    void onReceivedData(String senderName, String senderAddress, Object pdu);

    /**
     * Callback when exception occurs.
     *
     * @param e an Exception object which represents the exception.
     */
    void traceException(Exception e);

    void onConnectionClosed(String remoteAddress, String remoteName);

    void onConnection(String remoteAddress, String remoteName, int hops);

    void onConnectionFailed(String remoteName);//todo exception
}
