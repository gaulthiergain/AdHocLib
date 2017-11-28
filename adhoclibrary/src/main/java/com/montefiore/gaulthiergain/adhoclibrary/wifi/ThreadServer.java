package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.network.WifiNetwork;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaulthiergain on 10/11/17.
 */
public class ThreadServer extends Thread {

    private static final String TAG = "[AdHoc]";

    private final int nbThreads;
    private final Handler handler;
    private final ListSocketDevice listSocketDevice;
    private final ServerSocket serverSocket;

    private final ArrayList<ThreadClient> arrayThreadClients;

    public ThreadServer(Handler mHandler, int nbThreads, int port, ListSocketDevice listSocketDevice) throws IOException {

        this.handler = mHandler;
        this.nbThreads = nbThreads;
        this.listSocketDevice = listSocketDevice;
        this.arrayThreadClients = new ArrayList<>();
        this.serverSocket = new ServerSocket(port);

    }

    public void run() {

        // Start pool de threads
        for (int i = 0; i < nbThreads; i++) {
            ThreadClient threadClient = new ThreadClient(listSocketDevice, String.valueOf(i), handler);
            arrayThreadClients.add(threadClient);
            threadClient.start();
        }

        // Server Waiting
        Socket socket;
        while (true) {
            try {
                Log.d(TAG, "Server is waiting on accept...");
                socket = serverSocket.accept();
                if (socket != null) {
                    // Add to the list
                    Log.d(TAG, socket.getRemoteSocketAddress() + " accepted");
                    listSocketDevice.addSocketClient(socket);

                    /*
                    // Notify handler
                    String messageHandle = socket.getRemoteSocketAddress().toString();
                    handler.obtainMessage(WifiService.CONNECTION_PERFORMED, messageHandle).sendToTarget();
                    */
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

    public ConcurrentHashMap<String, WifiNetwork> getActiveConnexion() {
        return listSocketDevice.getActiveConnexion();
    }
}
