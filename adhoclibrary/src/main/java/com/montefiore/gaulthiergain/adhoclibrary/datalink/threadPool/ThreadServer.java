package com.montefiore.gaulthiergain.adhoclibrary.datalink.threadPool;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.AdHocServerSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.AdHocServerSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.AdHocSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.IServerSocket;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.ISocket;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class allows to start and manage the client threads. It is also responsible to manage
 * the connection/communication with remote devices.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class ThreadServer extends Thread {

    private static final String TAG = "[AdHoc][ThreadServer]";

    private final int nbThreads;
    private final boolean v;
    private UUID uuid;
    private final Handler handler;
    private final ListSocketDevice listSocketDevice;
    private final IServerSocket serverSocket;

    private final ArrayList<ThreadClient> arrayThreadClients;

    /**
     * Constructor
     *
     * @param handler          a Handler object which allows to send and process {@link Message}
     *                         and Runnable objects associated with a thread's.
     * @param nbThreads        an integer value to determine the number of threads managed by the
     *                         server.
     * @param verbose          a boolean value to set the debug/verbose mode.
     * @param secure           a boolean value to determine if the connection is secure.
     * @param name             a String value which represents the connection's name.
     * @param mAdapter         a BluetoothAdapter object which represents the local device Bluetooth
     *                         adapter.
     * @param uuid             an UUID object which identify the physical device.
     * @param listSocketDevice a listSocketDevice object which contains the clients managed by the
     *                         threadPool.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public ThreadServer(Handler handler, int nbThreads, boolean verbose, boolean secure, String name
            , BluetoothAdapter mAdapter, UUID uuid, ListSocketDevice listSocketDevice)
            throws IOException {

        this.handler = handler;
        this.nbThreads = nbThreads;
        this.v = verbose;
        this.uuid = uuid;
        this.listSocketDevice = listSocketDevice;
        this.arrayThreadClients = new ArrayList<>();

        if (secure) {
            this.serverSocket = new AdHocServerSocketBluetooth(
                    mAdapter.listenUsingRfcommWithServiceRecord(name, uuid));
        } else {
            this.serverSocket = new AdHocServerSocketBluetooth(
                    mAdapter.listenUsingInsecureRfcommWithServiceRecord(name, uuid));
        }
    }

    /**
     * Constructor
     *
     * @param handler          a Handler object which allows to send and process {@link Message}
     *                         and Runnable objects associated with a thread's.
     * @param nbThreads        an integer value to determine the number of threads managed by the
     *                         server.
     * @param verbose          a boolean value to set the debug/verbose mode.
     * @param port             an integer value to set the listening port number.
     * @param listSocketDevice a listSocketDevice object which contains the clients managed by the
     *                         threadPool.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public ThreadServer(Handler handler, int nbThreads, boolean verbose, int port,
                        ListSocketDevice listSocketDevice) throws IOException {

        this.handler = handler;
        this.nbThreads = nbThreads;
        this.v = verbose;
        this.listSocketDevice = listSocketDevice;
        this.arrayThreadClients = new ArrayList<>();
        this.serverSocket = new AdHocServerSocketWifi(new ServerSocket(port));
    }

    /**
     * Method allowing to start the client threads and to manage the connection/communication
     * with remote devices.
     */
    public void run() {

        // Start threadPool
        for (int i = 0; i < nbThreads; i++) {
            ThreadClient threadClient = new ThreadClient(listSocketDevice, String.valueOf(i), handler);
            arrayThreadClients.add(threadClient);
            threadClient.start();
        }

        // Manage client depending the connection
        if (serverSocket instanceof AdHocServerSocketBluetooth) {
            bluetoothRun();
        } else {
            wifiRun();
        }
    }

    /**
     * Method allowing to run the wifi server logic code.
     */
    private void wifiRun() {
        // Server Waiting
        Socket socket;
        ISocket isocket;
        while (true) {
            try {
                if (v) Log.d(TAG, "Server is waiting on accept...");
                socket = (Socket) serverSocket.accept();
                if (socket != null) {

                    // Add to the list
                    isocket = new AdHocSocketWifi(socket);
                    if (v) Log.d(TAG, isocket.getRemoteSocketAddress() + " accepted");
                    listSocketDevice.addSocketClient(isocket);

                    // Notify handler
                    String messageHandle[] = new String[2];
                    messageHandle[0] = isocket.getRemoteSocketAddress();
                    messageHandle[1] = "name"; //TODO
                    messageHandle[2] = socket.getLocalAddress().toString().substring(1);
                    handler.obtainMessage(Service.CONNECTION_PERFORMED, messageHandle).sendToTarget();
                } else {
                    if (v) Log.d(TAG, "Error while accepting client");
                }

            } catch (IOException e) {
                if (v) Log.e(TAG, "Error IO: " + e.getMessage());
                break;
            }
        }
    }

    /**
     * Method allowing to run the bluetooth server logic code.
     */
    private void bluetoothRun() {
        // Server Waiting
        BluetoothSocket socket;
        ISocket isocket;
        while (true) {
            try {
                if (v) Log.d(TAG, "Server is waiting on accept...");
                socket = (BluetoothSocket) serverSocket.accept();
                if (socket != null) {

                    // Add to the list
                    isocket = new AdHocSocketBluetooth(socket);
                    if (v) Log.d(TAG, socket.getRemoteDevice().getAddress() + " accepted");
                    listSocketDevice.addSocketClient(isocket);

                    // Notify handler
                    String messageHandle[] = new String[3];
                    messageHandle[0] = socket.getRemoteDevice().getName();
                    messageHandle[1] = socket.getRemoteDevice().getAddress();
                    messageHandle[2] = uuid.toString();
                    handler.obtainMessage(Service.CONNECTION_PERFORMED, messageHandle).sendToTarget();
                } else {
                    if (v) Log.d(TAG, "Error while accepting client");
                }

            } catch (IOException e) {
                if (v) Log.e(TAG, "Error IO: " + e.getMessage());
                break;
            }
        }
    }

    /**
     * Method allowing to stop all the threads client and the thread server itself.
     *
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public void cancel() throws IOException {
        if (v) Log.d(TAG, "cancel() thread server");
        for (ThreadClient threadClient : arrayThreadClients) {
            // Stop all threads client
            if (v) Log.d(TAG, "STOP thread " + threadClient.getNameThread());
            threadClient.interrupt();
        }

        // Close the server socket to throw an exception and thus stop the server thread
        serverSocket.close();
    }

    /**
     * Method allowing to return the active connections managed by the server.
     *
     * @return a ConcurrentHashMap<String, NetworkObject> which maps a remote device with a
     * NetworkObject (socket).
     */
    public ConcurrentHashMap<String, NetworkObject> getActiveConnexion() {
        return listSocketDevice.getActiveConnection();
    }
}
