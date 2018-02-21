package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class allows to manage the connections with remote nodes and data flows. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

public class AutoConnectionActives {
    private static final String TAG = "[AdHoc][AutoConActives]";

    private ConcurrentHashMap<String, NetworkObject> activesConnections;
    private ConcurrentHashMap<String, Long> activesDataPath;

    /**
     * Constructor
     */
    public AutoConnectionActives() {
        this.activesConnections = new ConcurrentHashMap<>();
        this.activesDataPath = new ConcurrentHashMap<>();
    }

    /**
     * Method allowing to add a connection into the activesConnections hashmap.
     *
     * @param key           a String value which represents the address of a remote device.
     * @param networkObject a NetworkObject object which represents the state of the connection.
     */
    public void addConnection(String key, NetworkObject networkObject) {
        if (!activesConnections.containsKey(key)) {
            activesConnections.put(key, networkObject);
            Log.d(TAG, "Add " + key + " into active connection");
        }
    }

    /**
     * Method allowing to update the Data Path (active data flow)
     *
     * @param key a String value which represents the address of a remote device.
     */
    public void updateDataPath(String key) {
        activesDataPath.put(key, System.currentTimeMillis());
        Log.d(TAG, "Update " + key + " in data path");
    }

    /**
     * Method allowing to get the active connections.
     *
     * @return ConcurrentHashMap<String, NetworkObject> object which maps the remote node name to
     * a NetworkObject object.
     */
    public ConcurrentHashMap<String, NetworkObject> getActivesConnections() {
        return activesConnections;
    }

    /**
     * Method allowing to get the active data path.
     *
     * @return ConcurrentHashMap<String, Long> object which maps the remote node name to
     * a timestamp.
     */
    public ConcurrentHashMap<String, Long> getActivesDataPath() {
        return activesDataPath;
    }
}
