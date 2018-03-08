package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class allows to manage the connections with remote nodes and data flows. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

class ActiveConnections {
    private static final String TAG = "[AdHoc][AutoConActives]";

    private ConcurrentHashMap<String, NetworkObject> activesConnections;

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
     * @param networkObject a NetworkObject object which represents the state of the connection.
     * @return a boolean value which is true if the pair <key, network> has been successfully added
     * to the hashmap.
     */
    boolean addConnection(String key, NetworkObject networkObject) {
        if (!activesConnections.containsKey(key)) {
            activesConnections.put(key, networkObject);
            return true;
        }

        return false;
    }

    /**
     * Method allowing to get the active connections.
     *
     * @return ConcurrentHashMap<String-NetworkObject> object which maps the remote node name to
     * a NetworkObject object.
     */
    ConcurrentHashMap<String, NetworkObject> getActivesConnections() {
        return activesConnections;
    }

}
