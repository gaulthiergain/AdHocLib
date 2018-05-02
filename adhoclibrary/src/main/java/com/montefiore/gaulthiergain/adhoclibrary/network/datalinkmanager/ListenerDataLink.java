package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;

import java.io.IOException;

public interface ListenerDataLink {

    void initInfos(String mac, String name);

    void brokenLink(String remoteNode) throws IOException;

    void processMsgReceived(MessageAdHoc message);
}
