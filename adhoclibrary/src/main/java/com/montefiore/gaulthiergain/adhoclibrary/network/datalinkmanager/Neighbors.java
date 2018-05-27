package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class contains the direct neighbours of a node. </p>
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
     * @param mac           a String value which represents the MAC address of a remote device.
     * @param socketManager a SocketManager object which represents the state of the connection.
     */
    void addNeighbors(String label, String mac, SocketManager socketManager) {
        neighbors.put(label, socketManager);
        mapLabelMac.put(label, mac);
    }

    /**
     * Method allowing to get the active connections.
     *
     * @return a ConcurrentHashMap<String, SocketManager> object which maps the remote node name to
     * a SocketManager object.
     */
    ConcurrentHashMap<String, SocketManager> getNeighbors() {
        return neighbors;
    }

    /**
     * Method allowing to get the labels and MAC.
     *
     * @return a ConcurrentHashMap<String, String> object which maps the remote node label to
     * the MAC address of this device.
     */
    ConcurrentHashMap<String, String> getLabelMac() {
        return mapLabelMac;
    }

    /**
     * Method allowing to remove an entry from the data structure where the key is the remote label.
     *
     * @param remoteLabel a String value which represents the label of a remote device.
     */
    void remove(String remoteLabel) {
        if (neighbors.containsKey(remoteLabel)) {
            neighbors.remove(remoteLabel);
            mapLabelMac.remove(remoteLabel);
        }
    }

    /**
     * Method allowing to get a SocketManager object from the data structure where the key
     * is the remote label.
     *
     * @param remoteLabel a String value which represents the label of a remote device.
     * @return a SocketManager object associated to a given remote label.
     */
    SocketManager getNeighbor(String remoteLabel) {
        if (neighbors.containsKey(remoteLabel)) {
            return neighbors.get(remoteLabel);
        }

        return null;
    }

    /**
     * Method allowing to clear all the data structures.
     */
    void clear() {
        neighbors.clear();
        mapLabelMac.clear();
    }
}
