package com.montefiore.gaulthiergain.adhoclibrary.auto;

import com.montefiore.gaulthiergain.adhoclibrary.aodv.EntryRoutingTable;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.Hashtable;

public interface ListenerAodv {
    void receivedRREQ(MessageAdHoc msg);

    void receivedRREP(MessageAdHoc msg);

    void receivedRERR(MessageAdHoc msg);

    void receivedDATA(MessageAdHoc msg);

    void receivedDATA_ACK(MessageAdHoc msg);

    void timerFlushRoutingTable(Hashtable<String, EntryRoutingTable> routingTable);

    void timerExpiredRREQ(String address, int retry);

    void clientAodvUnknownTypeException(AodvUnknownTypeException e);

    void clientNoConnectionException(NoConnectionException e);

    void clientIOException(IOException e);

    void serverIOException(IOException e);

    void serverNoConnectionException(NoConnectionException e);

    void serverAodvUnknownTypeException(AodvUnknownTypeException e);
}
