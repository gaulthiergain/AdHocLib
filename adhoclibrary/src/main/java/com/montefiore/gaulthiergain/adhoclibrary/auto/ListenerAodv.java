package com.montefiore.gaulthiergain.adhoclibrary.auto;

import com.montefiore.gaulthiergain.adhoclibrary.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

public interface ListenerAodv {

    /**
     * Callback when a RREQ message is received.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     */
    void receivedRREQ(MessageAdHoc message);

    /**
     * Callback when a RREQ message is received.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     */
    void receivedRREP(MessageAdHoc message);

    /**
     * Callback when a RREQ message is received.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     */
    void receivedRERR(MessageAdHoc message);

    /**
     * Callback when a RREQ message is received.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     */
    void receivedDATA(MessageAdHoc message);

    /**
     * Callback when a RREQ message is received.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     */
    void receivedDATA_ACK(MessageAdHoc message);

    /**
     * Callback when the flush timer for routing table is called.
     */
    void timerFlushRoutingTable();

    /**
     * Callback when the RREQ timer for routing table is called.
     *
     * @param destAddr a String value which represents the destination address.
     * @param retry    an integer value which represents the retries of the RREQ Timer.
     */
    void timerExpiredRREQ(String destAddr, int retry);

    /**
     * Callback when a clientAodvUnknownTypeException has occured
     *
     * @param exception an AodvUnknownTypeException object
     */
    void clientAodvUnknownTypeException(AodvUnknownTypeException exception);

    /**
     * Callback when a clientNoConnectionException has occured
     *
     * @param exception an NoConnectionException object
     */
    void clientNoConnectionException(NoConnectionException exception);

    /**
     * Callback when a clientIOException has occured
     *
     * @param exception an IOException object
     */
    void clientIOException(IOException exception);

    /**
     * Callback when a serverIOException has occured
     *
     * @param exception an IOException object
     */
    void serverIOException(IOException exception);

    /**
     * Callback when a serverNoConnectionException has occured
     *
     * @param exception a NoConnectionException object
     */
    void serverNoConnectionException(NoConnectionException exception);

    /**
     * Callback when a serverAodvUnknownTypeException has occured
     *
     * @param exception an AodvUnknownTypeException object
     */
    void serverAodvUnknownTypeException(AodvUnknownTypeException exception);
}
