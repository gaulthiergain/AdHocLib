package com.montefiore.gaulthiergain.adhoclibrary.util;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 17/11/17.
 */

public class Header implements Serializable {
    private final String type;
    private final String senderAddr;
    private final String senderName;

    public Header(String type, String senderAddr, String senderName) {
        this.type = type;
        this.senderAddr = senderAddr;
        this.senderName = senderName;
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

    @Override
    public String toString() {
        return "Header{" +
                "type='" + type + '\'' +
                ", senderAddr='" + senderAddr + '\'' +
                ", senderName='" + senderName + '\'' +
                '}';
    }
}
