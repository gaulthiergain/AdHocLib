package com.montefiore.gaulthiergain.adhoclib.threadPool;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by gaulthiergain on 10/11/17.
 *
 */
public class ThreadServer extends Thread {

    private static final String TAG = "[AdHoc]";

    private int port;
    private ListSocketDevice listSocketDevice;
    private ServerSocket serverSocket = null;

    ThreadServer(int port, ListSocketDevice listSocketDevice) {
        this.port = port;
        this.listSocketDevice = listSocketDevice;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            Log.d(TAG,"Error: IO " + e);
            //TODO exception
        }
        // Start pool de threads
        for (int i = 0; i < 3; i++) {
            ThreadClient thr = new ThreadClient(listSocketDevice, String.valueOf(i));
            thr.start();
        }

        // Server Waiting
        Socket socket;
        while (!isInterrupted()) {
            try {
                Log.d(TAG,"Server is waiting on accept...");
                socket = serverSocket.accept();
                if (socket != null) {
                    Log.d(TAG,socket.getRemoteSocketAddress().toString() + " accepted");
                    listSocketDevice.addSocketClient(socket);
                } else {
                    Log.d(TAG,"Error while accepting client");
                }

            } catch (IOException e) {
                Log.d(TAG,"Error: IO");
            }
        }
    }
}
