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
        while (true) {
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
                break;
            }
        }
    }

    public void cancel() throws IOException {
        Log.d(TAG, "cancel() thread server");
        for (ThreadClient threadClient : arrayThreadClients) {
            // Stop all threads client
            Log.d(TAG, "STOP thread" + threadClient.getNameThread());
            threadClient.interrupt();
        }

        // Close the server socket to throw an exception and thus stop the server thread
        serverSocket.close();
    }
}
