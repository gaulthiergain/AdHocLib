package com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.ISocket;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class allows to manage a list of clients used by the threadPool.</p>
 * <p>The clients are represented by ISocket objects ({@link ISocket}).</p>
 * <p>As this class is used by threads, all methods are synchronized.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class ListSocketDevice {

    private final boolean json;
    private final ArrayList<ISocket> listISockets;
    // Mapping between the remote address and a networkObject (a socket).
    private final ConcurrentHashMap<String, SocketManager> hashMapNetwork;

    /**
     * Constructor
     *
     * @param json a boolean value to use json or bytes in network transfer.
     */
    public ListSocketDevice(boolean json) {
        this.json = json;
        this.listISockets = new ArrayList<>();
        this.hashMapNetwork = new ConcurrentHashMap<>();
    }

    /**
     * Method allowing to get a ISocket from the listISockets list. This ISocket object represents
     * a client which will be served when a thread is available.
     *
     * @return a ISocket object which represents a socket between the server and the client.
     * @throws InterruptedException signals an exception when a thread is waiting, sleeping, or occupied.
     */
    synchronized ISocket getSocketDevice() throws InterruptedException {
        while (listISockets.isEmpty()) {
            wait();
        }
        return listISockets.remove(0);
    }

    /**
     * Method allowing to add temporary a ISocket into the listISockets list.  This ISocket object
     * represents a client which will be served until a thread is available.
     *
     * @param isocket a ISocket object which represents a socket between the server and the client.
     */
    synchronized void addSocketClient(ISocket isocket) throws IOException {

        /* Add the ISocket object into hashMapNetwork to get a mapping between the remote address
        and a networkObject (respectively a socket). */
        String key = isocket.getRemoteSocketAddress();
        if (!hashMapNetwork.containsKey(key)) {
            hashMapNetwork.put(key, new SocketManager(isocket, json));
        }

        listISockets.add(isocket);
        notify();
    }

    /**
     * Method allowing to return the active connections managed by the server.
     *
     * @return a ConcurrentHashMap<String, SocketManager> which maps a remote device with a
     * SocketManager (socket).
     */
    synchronized ConcurrentHashMap<String, SocketManager> getActiveConnection() {
        return hashMapNetwork;
    }

    /**
     * Method allowing to remove an active connection (a socket) from the
     * ConcurrentHashMap<String, SocketManager>.
     *
     * @param key a String value which represents the key mapping of hashMapNetwork.
     */
    synchronized void removeActiveConnexion(String key) {
        if (hashMapNetwork.containsKey(key)) {
            hashMapNetwork.remove(key);
        }
    }
}