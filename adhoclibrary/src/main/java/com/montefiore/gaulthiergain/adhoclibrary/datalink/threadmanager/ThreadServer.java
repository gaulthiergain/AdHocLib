package com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.AdHocServerSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.AdHocServerSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.AdHocSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.IServerSocket;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.ISocket;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
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

    private final short nbThreads;
    private final boolean v;
    private final Handler handler;
    private final ListSocketDevice listSocketDevice;
    private final IServerSocket serverSocket;

    private final ArrayList<ThreadClient> arrayThreadClients;

    /**
     * Constructor
     *
     * @param handler          a Handler object which allows to send and process {@link Message}
     *                         and Runnable objects associated with a thread's.
     * @param nbThreads        a short value to determine the number of threads managed by the
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
    public ThreadServer(Handler handler, short nbThreads, boolean verbose, boolean secure, String name
            , BluetoothAdapter mAdapter, UUID uuid, ListSocketDevice listSocketDevice)
            throws IOException {

        if (mAdapter == null) {
            throw new IOException("Bluetooth adapter is null");
        }

        if (uuid == null) {
            throw new IOException("Uuid is null");
        }

        this.handler = handler;
        this.nbThreads = nbThreads;
        this.v = verbose;
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
     * @param nbThreads        a short value to determine the number of threads managed by the
     *                         server.
     * @param verbose          a boolean value to set the debug/verbose mode.
     * @param port             an integer value to set the listening port number.
     * @param listSocketDevice a listSocketDevice object which contains the clients managed by the
     *                         threadPool.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public ThreadServer(Handler handler, short nbThreads, boolean verbose, int port,
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

        while (true) {
            try {
                if (v) Log.d(TAG, "Server is waiting on accept...");
                ISocket isocket = acceptISocket();

                if (v) Log.d(TAG, isocket.getRemoteSocketAddress() + " accepted");
                listSocketDevice.addSocketClient(isocket);

                // Notify handler
                handler.obtainMessage(Service.CONNECTION_PERFORMED,
                        isocket.getRemoteSocketAddress()).sendToTarget();

            } catch (SocketException e) {
                handler.obtainMessage(Service.LOG_EXCEPTION, e).sendToTarget();
                break;
            } catch (IOException e) {
                handler.obtainMessage(Service.LOG_EXCEPTION, e).sendToTarget();
                break;
            }
        }
    }

    /**
     * Method allowing to accept remote connection.
     *
     * @return a ISocket object which represents a socket between the server and the client.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    private ISocket acceptISocket() throws IOException {
        // Manage client depending the connection
        if (serverSocket instanceof AdHocServerSocketBluetooth) {
            return new AdHocSocketBluetooth((BluetoothSocket) serverSocket.accept());
        } else {
            return new AdHocSocketWifi((Socket) serverSocket.accept());
        }
    }

    /**
     * Method allowing to stop all the threads client and the thread server itself.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void cancel() throws IOException {
        if (v) Log.d(TAG, "cancel() thread server");

        // Use iterator to
        Iterator iterator = arrayThreadClients.iterator();
        while (iterator.hasNext()) {
            ThreadClient threadClient = (ThreadClient) iterator.next();
            // Stop all threads client
            if (v) Log.d(TAG, "STOP thread " + threadClient.getNameThread());
            threadClient.interrupt();
        }

        // Clear the array of threads
        arrayThreadClients.clear();

        // Close the server socket to throw an exception and thus stop the server thread
        serverSocket.close();
    }

    /**
     * Method allowing to return the active connections managed by the server.
     *
     * @return a ConcurrentHashMap<String, SocketManager> which maps a remote device with a
     * SocketManager (socket).
     */
    public ConcurrentHashMap<String, SocketManager> getActiveConnexion() {
        return listSocketDevice.getActiveConnection();
    }
}
