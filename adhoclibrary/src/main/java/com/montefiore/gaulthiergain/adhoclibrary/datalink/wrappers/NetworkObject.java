package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

/**
 * Created by gaulthiergain on 23/03/18.
 */

class NetworkObject {
    private final byte type;
    private final Object socketManager;

    NetworkObject(byte type, Object socketManager) {
        this.type = type;
        this.socketManager = socketManager;
    }

    public byte getType() {
        return type;
    }

    public Object getSocketManager() {
        return socketManager;
    }

    @Override
    public String toString() {
        return "NetworkObject{" +
                "type=" + type +
                ", socketManager=" + socketManager +
                '}';
    }
}
