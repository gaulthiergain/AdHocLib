package com.montefiore.gaulthiergain.adhoclib.bluetoothListener;

import android.bluetooth.BluetoothDevice;

import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothAdHocDevice;

import java.util.HashMap;

/**
 * Created by gaulthiergain on 15/11/17.
 */

public interface MessageListener {
    void onMessageReceived(String message);
    void onMessageSent(String message);
    void onConnectionClosed();
    void onErrorMessage();
}
