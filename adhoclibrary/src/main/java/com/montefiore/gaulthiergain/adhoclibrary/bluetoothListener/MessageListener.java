package com.montefiore.gaulthiergain.adhoclibrary.bluetoothListener;

/**
 * Created by gaulthiergain on 15/11/17.
 */

public interface MessageListener {
    void onMessageReceived(String message, String sender);
    void onMessageSent(String message);
    void onConnectionClosed();
    void onErrorMessage();
}
