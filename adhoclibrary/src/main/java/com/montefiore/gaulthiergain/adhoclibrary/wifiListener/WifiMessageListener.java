package com.montefiore.gaulthiergain.adhoclibrary.wifiListener;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

/**
 * Created by gaulthiergain on 26/10/17.
 *
 */

public interface WifiMessageListener {
    void onMessageReceived(MessageAdHoc message);

    void onMessageSent(MessageAdHoc message);

    void onBroadcastSend(MessageAdHoc message);

    void onConnectionClosed(String deviceAddr);

    void onConnection(String remoteAddr, String localAddr);
}

