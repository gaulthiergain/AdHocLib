package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

public interface ListenerDataLink {

    void brokenLink(String remoteNode) throws IOException;

    void processMsgReceived(MessageAdHoc message);
}
