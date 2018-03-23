package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class allows to manage the connections with remote nodes and data flows. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

public class ActiveConnections {

    private ConcurrentHashMap<String, NetworkManager> activesConnections;

    /**
     * Constructor
     */
    ActiveConnections() {
        this.activesConnections = new ConcurrentHashMap<>();
    }

    /**
     * Method allowing to add a connection into the activesConnections hashmap.
     *
     * @param key           a String value which represents the address of a remote device.
     * @param networkManager a NetworkManager object which represents the state of the connection.
     * @return a boolean value which is true if the pair (key, network) has been successfully added
     * to the hashmap.
     */
    public boolean addConnection(String key, NetworkManager networkManager) {
        if (!activesConnections.containsKey(key)) {
            activesConnections.put(key, networkManager);
            return true;
        }

        return false;
    }

    /**
     * Method allowing to get the active connections.
     *
     * @return a ConcurrentHashMap(String, NetworkManager) object which maps the remote node name to
     * a NetworkManager object.
     */
    public ConcurrentHashMap<String, NetworkManager> getActivesConnections() {
        return activesConnections;
    }

}
