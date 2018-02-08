package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;

import java.util.concurrent.ConcurrentHashMap;

public class AutoConnectionActives {
    private static final String TAG = "[AdHoc][AutoConActives]";
    private ConcurrentHashMap<String, NetworkObject> activesConnections;

    private ConcurrentHashMap<String, Long> activesDataPath; //todo update with DataPath object?

    public AutoConnectionActives() {
        this.activesConnections = new ConcurrentHashMap<>();
        this.activesDataPath = new ConcurrentHashMap<>();
    }

    public void addConnection(String key, NetworkObject networkObject) {
        if (!activesConnections.containsKey(key)) {
            activesConnections.put(key, networkObject);
            Log.d(TAG, "Add " + key);
        }
    }

    public void updateDataPath(String key) {
        activesDataPath.put(key, System.currentTimeMillis());
    }

    public ConcurrentHashMap<String, NetworkObject> getActivesConnections() {
        return activesConnections;
    }

    public ConcurrentHashMap<String, Long> getActivesDataPath() {
        return activesDataPath;
    }
}
