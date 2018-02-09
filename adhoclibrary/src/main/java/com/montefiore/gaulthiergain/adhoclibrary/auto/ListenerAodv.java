package com.montefiore.gaulthiergain.adhoclibrary.auto;

import com.montefiore.gaulthiergain.adhoclibrary.aodv.EntryRoutingTable;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.util.Hashtable;

public interface ListenerAodv {
    void receivedRREQ(MessageAdHoc msg);

    void receivedRREP(MessageAdHoc msg);

    void receivedRERR(MessageAdHoc msg);

    void receivedDATA(MessageAdHoc msg);

    void receivedDATA_ACK(MessageAdHoc msg);

    void timerFlushRoutingTable(Hashtable<String, EntryRoutingTable> routingTable);
}
