package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

public interface ListenerAodv {

    /**
     * Callback when the discovery is completed.
     */
    void onDiscoveryCompleted();

    /**
     * Callback when the getPairedDevices is completed.
     */
    void onPairedCompleted();

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
     * Callback when a IOException has occured
     *
     * @param e an IOException object
     */
    void IOException(IOException e);

    /**
     * Callback when a NoConnectionException has occured
     *
     * @param e a NoConnectionException object
     */
    void NoConnectionException(NoConnectionException e);

    /**
     * Callback when a AodvUnknownTypeException has occured
     *
     * @param e an AodvUnknownTypeException object
     */
    void AodvUnknownTypeException(AodvUnknownTypeException e);

    /**
     * Callback when a AodvUnknownDestException has occured
     *
     * @param e an AodvUnknownDestException object
     */
    void AodvUnknownDestException(AodvUnknownDestException e);

    /**
     * Callback when a DeviceException has occured
     *
     * @param e a DeviceException object
     */
    void DeviceException(DeviceException e);
}