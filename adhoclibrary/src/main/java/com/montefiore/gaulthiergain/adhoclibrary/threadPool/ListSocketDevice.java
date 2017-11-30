package com.montefiore.gaulthiergain.adhoclibrary.threadPool;

import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.network.ISocket;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaulthiergain on 10/11/17.
 */
public class ListSocketDevice {

    private static final String TAG = "[AdHoc]";

    private ArrayList<ISocket> listISockets;
    private ConcurrentHashMap<String, NetworkObject> hashMapNetwork;

    public ListSocketDevice() {
        listISockets = new ArrayList<>();
        hashMapNetwork = new ConcurrentHashMap<>();
    }

    public synchronized ISocket getSocketDevice() throws InterruptedException {
        Log.d(TAG, "Waiting Socket...");
        while (listISockets.isEmpty()) {
            wait();
        }
        return listISockets.remove(0);
    }


    public synchronized void addSocketClient(ISocket isocket) {

        String key = isocket.getRemoteSocketAddress();
        if (!hashMapNetwork.containsKey(key)) {
            hashMapNetwork.put(key, new NetworkObject(isocket));
        }

        listISockets.add(isocket);
        Log.d(TAG, "Add waiting Socket");
        notify();
    }

    public ConcurrentHashMap<String, NetworkObject> getActiveConnexion() {
        return hashMapNetwork;
    }

    public void removeActiveConnexion(ISocket socket) {
        hashMapNetwork.remove(socket.getRemoteSocketAddress());
    }

}