package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;

import java.io.IOException;

public interface ListenerApp {
    /**
     * Callback when a Data message is received.
     *
     * @param pdu
     */
    void onReceivedData(AdHocDevice adHocDevice, Object pdu);

    void onConnection(AdHocDevice adHocDevice);

    void onConnectionFailed(Exception e);

    void onConnectionClosed(AdHocDevice adHocDevice);

    void onConnectionClosedFailed(Exception e);

    void processMsgException(Exception e);
}
