package com.montefiore.gaulthiergain.adhoclib.threadPool;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by gaulthiergain on 10/11/17.
 */
public class ThreadServer extends Thread {

    private static final String TAG = "[AdHoc]";

    private final int nbThreads;
    private final Handler handler;
    private final ListSocketDevice listSocketDevice;
    private final BluetoothServerSocket serverSocket;

    private final ArrayList<ThreadClient> arrayThreadClients;

    public ThreadServer(Handler mHandler, int nbThreads, boolean secure, String name, BluetoothAdapter mAdapter,
                        UUID uuid, ListSocketDevice listSocketDevice) throws IOException {

        this.handler = mHandler;
        this.nbThreads = nbThreads;
        this.listSocketDevice = listSocketDevice;
        this.arrayThreadClients = new ArrayList<>();

        if (secure) {
            this.serverSocket = mAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
        } else {
            this.serverSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord(name, uuid);
        }

    }

    public void run() {

        // Start pool de threads
        for (int i = 0; i < nbThreads; i++) {
            ThreadClient threadClient = new ThreadClient(listSocketDevice, String.valueOf(i));
            arrayThreadClients.add(threadClient);
            threadClient.start();
        }

        // Server Waiting
        BluetoothSocket socket;
        while (!isInterrupted()) {
            try {
                Log.d(TAG, "Server is waiting on accept...");
                socket = serverSocket.accept();
                if (socket != null) {
                    Log.d(TAG, socket.getRemoteDevice().getAddress() + " accepted");
                    listSocketDevice.addSocketClient(socket);
                } else {
                    Log.d(TAG, "Error while accepting client");
                }

            } catch (IOException e) {
                Log.d(TAG, "Error: IO");
            }
        }
    }

    public void cancel() {
        Log.d(TAG, "cancel() thread server");
        try {
            serverSocket.close();
            for (int i = 0; i < arrayThreadClients.size(); i++) {
                arrayThreadClients.get(i).interrupt(); //TODO UPDATE IT
            }
        } catch (IOException e) {
            Log.e(TAG, "close() of server failed", e);
        }
    }
}
