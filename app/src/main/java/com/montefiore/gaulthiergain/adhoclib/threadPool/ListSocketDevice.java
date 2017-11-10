package com.montefiore.gaulthiergain.adhoclib.threadPool;

import android.util.Log;

import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by gaulthiergain on 10/11/17.
 *
 */
public class ListSocketDevice {

    private static final String TAG = "[AdHoc]";

    private ArrayList<Socket> listTasks;

    ListSocketDevice() {
        listTasks = new ArrayList<>();
    }

    public synchronized Socket getSocketDevice() throws InterruptedException {
        Log.d(TAG, "Waiting Socket...");
        while (listTasks.isEmpty()){
            wait();
        }
        return listTasks.remove(0);
    }


    public synchronized void addSocketClient(Socket socket) {
        listTasks.add(socket);
        Log.d(TAG, "Add waiting Socket");
        notify();
    }
}