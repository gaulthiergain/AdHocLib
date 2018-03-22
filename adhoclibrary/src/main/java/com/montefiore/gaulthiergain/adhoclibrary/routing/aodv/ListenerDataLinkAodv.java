package com.montefiore.gaulthiergain.adhoclibrary.routing.aodv;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

public interface ListenerDataLinkAodv {

    void brokenLink(String remoteNode) throws IOException, NoConnectionException;

    void processMsgReceived(MessageAdHoc message) throws IOException, AodvUnknownTypeException,
            AodvUnknownDestException, NoConnectionException;
}
