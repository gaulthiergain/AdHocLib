package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

/**
 * Created by gaulthiergain on 23/03/18.
 */

public class NetworkObject {
    private final byte type;
    private final Object networkManager;

    public NetworkObject(byte type, Object networkManager) {
        this.type = type;
        this.networkManager = networkManager;
    }

    public byte getType() {
        return type;
    }

    public Object getNetworkManager() {
        return networkManager;
    }

    @Override
    public String toString() {
        return "NetworkObject{" +
                "type=" + type +
                ", networkManager=" + networkManager +
                '}';
    }
}
