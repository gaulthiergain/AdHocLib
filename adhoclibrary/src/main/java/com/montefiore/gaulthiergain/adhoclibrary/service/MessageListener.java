package com.montefiore.gaulthiergain.adhoclibrary.service;

import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

/**
 * Created by gaulthiergain on 15/11/17.
 */

public interface MessageListener {
    void onMessageReceived(MessageAdHoc message);

    void onMessageSent(MessageAdHoc message);

    void onBroadcastSend(MessageAdHoc message);

    void onConnectionClosed(String deviceName, String deviceAddr);

    void onConnection(String deviceName, String deviceAddr, String localAddr);
}
