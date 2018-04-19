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
    private ConcurrentHashMap<String, String> mapLabelMac;

    /**
     * Constructor
     */
    Neighbors() {

        this.neighbors = new ConcurrentHashMap<>();
        this.mapLabelMac = new ConcurrentHashMap<>();
    }

    /**
     * Method allowing to add a connection into the neighbors hashmap.
     *
     * @param label         a String value which represents the label of a remote device.
     * @param mac
     * @param socketManager a SocketManager object which represents the state of the connection.
     */
    void addNeighbors(String label, String mac, SocketManager socketManager) {
        neighbors.put(label, socketManager);
        mapLabelMac.put(label, mac);
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


    ConcurrentHashMap<String, String> getLabelMac() {
        return mapLabelMac;
    }

    void remove(String remoteLabel) {
        if (neighbors.containsKey(remoteLabel)) {
            neighbors.remove(remoteLabel);
            mapLabelMac.remove(remoteLabel);
        }
    }

    SocketManager getNeighbor(String remoteLabel) {
        if (neighbors.containsKey(remoteLabel)) {
            return neighbors.get(remoteLabel);
        }

        return null;
    }

    void clear() {
        neighbors.clear();
        mapLabelMac.clear();
    }
}
