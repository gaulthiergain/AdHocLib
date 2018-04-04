package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class allows to manage the connections with remote nodes and data flows. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

public class Neighbors {

    private ConcurrentHashMap<String, SocketManager> neighbors;

    /**
     * Constructor
     */
    Neighbors() {
        this.neighbors = new ConcurrentHashMap<>();
    }

    /**
     * Method allowing to add a connection into the neighbors hashmap.
     *
     * @param key           a String value which represents the address of a remote device.
     * @param socketManager a SocketManager object which represents the state of the connection.
     */
    public void addNeighbors(String key, SocketManager socketManager) {
        neighbors.put(key, socketManager);
    }

    /**
     * Method allowing to get the active connections.
     *
     * @return a ConcurrentHashMap(String, SocketManager) object which maps the remote node name to
     * a SocketManager object.
     */
    public ConcurrentHashMap<String, SocketManager> getNeighbors() {
        return neighbors;
    }

    public void remove(String remoteLabel) {
        if (neighbors.containsKey(remoteLabel)) {
            neighbors.remove(remoteLabel);
        }
    }

    public SocketManager getNeighbor(String remoteLabel) {
        if (neighbors.containsKey(remoteLabel)) {
            return neighbors.get(remoteLabel);
        }

        return null;
    }
}
