package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class allows to manage the connections with remote nodes and data flows. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

public class Neighbors {

    private ConcurrentHashMap<String, NetworkObject> neighbors;

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
     * @param networkObject a NetworkObject object which represents the state of the connection.
     */
    public void addNeighbors(String key, NetworkObject networkObject) {
        neighbors.put(key, networkObject);
    }

    /**
     * Method allowing to get the active connections.
     *
     * @return a ConcurrentHashMap(String, NetworkManager) object which maps the remote node name to
     * a NetworkManager object.
     */
    public ConcurrentHashMap<String, NetworkObject> getNeighbors() {
        return neighbors;
    }

}
