package com.montefiore.gaulthiergain.adhoclibrary.util;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 17/11/17.
 */

public class Header implements Serializable {
    private final String type;
    private final String senderAddr;
    private final String senderName;
    private int rssi;

    public Header(String type, String senderAddr, String senderName) {
        this.type = type;
        this.senderAddr = senderAddr;
        this.senderName = senderName;
    }

    public Header(String type, String senderAddr, String senderName, int rssi) {
        this(type, senderAddr, senderName);
        this.rssi = rssi;
    }

    public String getType() {
        return type;
    }

    public String getSenderAddr() {
        return senderAddr;
    }

    public String getSenderName() {
        return senderName;
    }

    public int getRssi() {
        return rssi;
    }

    @Override
    public String toString() {
        return "Header{" +
                "type='" + type + '\'' +
                ", senderAddr='" + senderAddr + '\'' +
                ", senderName='" + senderName + '\'' +
                ", rssi=" + rssi +
                '}';
    }
}
