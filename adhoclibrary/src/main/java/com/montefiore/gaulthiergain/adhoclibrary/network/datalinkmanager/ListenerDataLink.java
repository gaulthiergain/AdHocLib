package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

public interface ListenerDataLink {

    void brokenLink(String remoteNode) throws IOException, NoConnectionException;

    void processMsgReceived(MessageAdHoc message) throws IOException, AodvUnknownTypeException,
            AodvUnknownDestException, NoConnectionException;
}
