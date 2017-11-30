package com.montefiore.gaulthiergain.adhoclibrary.threadPool;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothService;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocServerSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocServerSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.network.IServerSocket;
import com.montefiore.gaulthiergain.adhoclibrary.network.ISocket;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.wifi.WifiService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaulthiergain on 10/11/17.
 */
public class ThreadServer extends Thread {

    private static final String TAG = "[AdHoc]";

    private final int nbThreads;
    private final Handler handler;
    private final ListSocketDevice listSocketDevice;
    private final IServerSocket serverSocket;

    private final ArrayList<ThreadClient> arrayThreadClients;

    public ThreadServer(Handler mHandler, int nbThreads, boolean secure, String name, BluetoothAdapter mAdapter,
                        UUID uuid, ListSocketDevice listSocketDevice) throws IOException {

        this.handler = mHandler;
        this.nbThreads = nbThreads;
        this.listSocketDevice = listSocketDevice;
        this.arrayThreadClients = new ArrayList<>();

        if (secure) {
            this.serverSocket = new AdHocServerSocketBluetooth(mAdapter.listenUsingRfcommWithServiceRecord(name, uuid));
        } else {
            this.serverSocket = new AdHocServerSocketBluetooth(mAdapter.listenUsingInsecureRfcommWithServiceRecord(name, uuid));
        }

    }

    public ThreadServer(Handler mHandler, int nbThreads, int port,
                        ListSocketDevice listSocketDevice) throws IOException {

        this.handler = mHandler;
        this.nbThreads = nbThreads;
        this.listSocketDevice = listSocketDevice;
        this.arrayThreadClients = new ArrayList<>();
        this.serverSocket = new AdHocServerSocketWifi(new ServerSocket(port));

    }

    public void run() {

        // Start pool de threads
        for (int i = 0; i < nbThreads; i++) {
            ThreadClient threadClient = new ThreadClient(listSocketDevice, String.valueOf(i), handler);
            arrayThreadClients.add(threadClient);
            threadClient.start();
        }

        // Manage client
        if (serverSocket instanceof AdHocServerSocketBluetooth) {
            Log.d(TAG, "BLUETOOTH RUN");
            bluetoothRun();
        } else {
            Log.d(TAG, "WIFI RUN");
            wifiRun();
        }
    }

    public void wifiRun() {
        // Server Waiting
        Socket socket;
        ISocket isocket;
        while (true) {
            try {
                Log.d(TAG, "Server is waiting on accept...");
                socket = (Socket) serverSocket.accept();

                if (socket != null) {
                    //TODO comment
                    isocket = new AdHocSocketWifi(socket);
                    // Add to the list
                    Log.d(TAG, isocket.getRemoteSocketAddress() + " accepted");
                    listSocketDevice.addSocketClient(isocket);

                    // Notify handler
                    String messageHandle = isocket.getRemoteSocketAddress();
                    handler.obtainMessage(WifiService.CONNECTION_PERFORMED, messageHandle).sendToTarget();
                } else {
                    Log.d(TAG, "Error while accepting client");
                }

            } catch (IOException e) {
                Log.d(TAG, "Error: IO");
                break;
            }
        }
    }

    public void bluetoothRun() {
        // Server Waiting
        BluetoothSocket socket;
        ISocket isocket;
        while (true) {
            try {
                Log.d(TAG, "Server is waiting on accept...");
                socket = (BluetoothSocket) serverSocket.accept();

                if (socket != null) {
                    //TODO comment
                    isocket = new AdHocSocketBluetooth(socket);
                    // Add to the list
                    Log.d(TAG, socket.getRemoteDevice().getAddress() + " accepted");
                    listSocketDevice.addSocketClient(isocket);

                    // Notify handler
                    String messageHandle[] = new String[2];
                    messageHandle[0] = socket.getRemoteDevice().getName();
                    messageHandle[1] = socket.getRemoteDevice().getAddress();
                    handler.obtainMessage(BluetoothService.CONNECTION_PERFORMED, messageHandle).sendToTarget();
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

    public ConcurrentHashMap<String, NetworkObject> getActiveConnexion() {
        return listSocketDevice.getActiveConnexion();
    }
}
