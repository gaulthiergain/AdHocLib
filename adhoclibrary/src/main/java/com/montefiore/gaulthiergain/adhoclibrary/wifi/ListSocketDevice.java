package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.network.BluetoothNetwork;
import com.montefiore.gaulthiergain.adhoclibrary.network.WifiNetwork;

import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaulthiergain on 10/11/17.
 */
public class ListSocketDevice {

    private static final String TAG = "[AdHoc]";

    private ArrayList<Socket> listTasks;
    private ConcurrentHashMap<String, WifiNetwork> hashMapNetwork;

    public ListSocketDevice() {
        listTasks = new ArrayList<>();
        hashMapNetwork = new ConcurrentHashMap<>();
    }

    public synchronized Socket getSocketDevice() throws InterruptedException {
        Log.d(TAG, "Waiting Socket...");
        while (listTasks.isEmpty()) {
            wait();
        }
        return listTasks.remove(0);
    }


    public synchronized void addSocketClient(Socket socket) {

        String key = socket.getRemoteSocketAddress().toString();
        if (!hashMapNetwork.containsKey(key)) {
            hashMapNetwork.put(key, new WifiNetwork(socket, true));
        }

        listTasks.add(socket);
        Log.d(TAG, "Add waiting Socket");
        notify();
    }

    public ConcurrentHashMap<String, WifiNetwork> getActiveConnexion() {
        return hashMapNetwork;
    }

    public void removeActiveConnexion(BluetoothSocket socket) {
        hashMapNetwork.remove(socket.getRemoteDevice().toString());
    }

}