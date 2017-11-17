package com.montefiore.gaulthiergain.adhoclibrary.bluetoothListener;

/**
 * Created by gaulthiergain on 15/11/17.
 */

public interface MessageListener {
    void onMessageReceived(String message, String senderName, String senderAddr);
    void onMessageSent(String message);
    void onConnectionClosed(String deviceName, String deviceAddr);
    void onConnection(String deviceName, String deviceAddr);
}
