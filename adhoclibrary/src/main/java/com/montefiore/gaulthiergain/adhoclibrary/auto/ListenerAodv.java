package com.montefiore.gaulthiergain.adhoclibrary.auto;

import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
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
     * Callback when a RREP GRATUITOUS message is received.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     */
    void receivedRREP_GRAT(MessageAdHoc message);

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
     * Callback when the RREQ timer for routing table is called.
     *
     * @param destAddr a String value which represents the destination address.
     * @param retry    an integer value which represents the retries of the RREQ Timer.
     */
    void timerExpiredRREQ(String destAddr, int retry);

    /**
     * Callback when a clientAodvUnknownTypeException has occured
     *
     * @param e an AodvUnknownTypeException object
     */
    void clientAodvUnknownTypeException(AodvUnknownTypeException e);

    /**
     * Callback when a serverAodvUnknownDestException has occured
     *
     * @param e an serverAodvUnknownDestException object
     */
    void clientAodvUnknownDestException(AodvUnknownDestException e);

    /**
     * Callback when a clientNoConnectionException has occured
     *
     * @param e an NoConnectionException object
     */
    void clientNoConnectionException(NoConnectionException e);

    /**
     * Callback when a clientIOException has occured
     *
     * @param e an IOException object
     */
    void clientIOException(IOException e);

    /**
     * Callback when a serverIOException has occured
     *
     * @param e an IOException object
     */
    void serverIOException(IOException e);

    /**
     * Callback when a serverNoConnectionException has occured
     *
     * @param e a NoConnectionException object
     */
    void serverNoConnectionException(NoConnectionException e);

    /**
     * Callback when a serverAodvUnknownTypeException has occured
     *
     * @param e an AodvUnknownTypeException object
     */
    void serverAodvUnknownTypeException(AodvUnknownTypeException e);

    /**
     * Callback when a serverAodvUnknownDestException has occured
     *
     * @param e an serverAodvUnknownDestException object
     */
    void serverAodvUnknownDestException(AodvUnknownDestException e);
}