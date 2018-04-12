package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class allows to manage the connections with remote nodes and data flows. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

class Neighbors {

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
     void addNeighbors(String key, SocketManager socketManager) {
        neighbors.put(key, socketManager);
    }

    /**
     * Method allowing to get the active connections.
     *
     * @return a ConcurrentHashMap(String, SocketManager) object which maps the remote node name to
     * a SocketManager object.
     */
     ConcurrentHashMap<String, SocketManager> getNeighbors() {
        return neighbors;
    }

     void remove(String remoteLabel) {
        if (neighbors.containsKey(remoteLabel)) {
            neighbors.remove(remoteLabel);
        }
    }

     SocketManager getNeighbor(String remoteLabel) {
        if (neighbors.containsKey(remoteLabel)) {
            return neighbors.get(remoteLabel);
        }

        return null;
    }
}
