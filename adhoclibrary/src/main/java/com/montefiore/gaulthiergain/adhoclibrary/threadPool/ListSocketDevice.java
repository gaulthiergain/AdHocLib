package com.montefiore.gaulthiergain.adhoclibrary.threadPool;

import com.montefiore.gaulthiergain.adhoclibrary.network.ISocket;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;

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

    private ArrayList<ISocket> listISockets;
    // Mapping between the remote address and a networkObject (a socket).
    private ConcurrentHashMap<String, NetworkObject> hashMapNetwork;

    /**
     * Constructor
     */
    public ListSocketDevice() {
        listISockets = new ArrayList<>();
        hashMapNetwork = new ConcurrentHashMap<>();
    }

    /**
     * Method allowing to get a ISocket from the listISockets list. This ISocket object represents
     * a client which will be served when a thread is available.
     *
     * @return a ISocket object which represents a socket between the server and the client.
     * @throws InterruptedException Thrown when a thread is waiting, sleeping, or occupied.
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
    synchronized void addSocketClient(ISocket isocket) {

        /* Add the ISocket object into hashMapNetwork to get a mapping between the remote address
        and a networkObject (respectively a socket). */
        String key = isocket.getRemoteSocketAddress();
        if (!hashMapNetwork.containsKey(key)) {
            hashMapNetwork.put(key, new NetworkObject(isocket));
        }

        listISockets.add(isocket);
        notify();
    }

    /**
     * Method allowing to return the active connections managed by the server.
     *
     * @return a ConcurrentHashMap<String, NetworkObject> which maps a remote device with a
     * NetworkObject (socket).
     */
    synchronized ConcurrentHashMap<String, NetworkObject> getActiveConnection() {
        return hashMapNetwork;
    }

    /**
     * Method allowing to remove an active connection (a socket) from the
     * ConcurrentHashMap<String, NetworkObject>.
     *
     * @param isocket a ISocket object which represents a socket between the server and the client.
     */
    synchronized void removeActiveConnexion(ISocket isocket) {
        String key = isocket.getRemoteSocketAddress();
        if (hashMapNetwork.containsKey(key)) {
            hashMapNetwork.remove(isocket.getRemoteSocketAddress());
        }
    }
}