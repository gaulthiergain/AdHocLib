package com.montefiore.gaulthiergain.adhoclib.threadPool;

import android.bluetooth.BluetoothSocket;
import android.util.Log;
import java.util.ArrayList;

/**
 * Created by gaulthiergain on 10/11/17.
 *
 */
public class ListSocketDevice {

    private static final String TAG = "[AdHoc]";

    private ArrayList<BluetoothSocket> listTasks;

    public ListSocketDevice() {
        listTasks = new ArrayList<>();
    }

    public synchronized BluetoothSocket getSocketDevice() throws InterruptedException {
        Log.d(TAG, "Waiting Socket...");
        while (listTasks.isEmpty()) {
            wait();
        }
        return listTasks.remove(0);
    }


    public synchronized void addSocketClient(BluetoothSocket socket) {
        listTasks.add(socket);
        Log.d(TAG, "Add waiting Socket");
        notify();
    }

}