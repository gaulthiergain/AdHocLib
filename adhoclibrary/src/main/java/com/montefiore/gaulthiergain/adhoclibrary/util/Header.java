package com.montefiore.gaulthiergain.adhoclibrary.util;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 17/11/17.
 */

public class Header implements IHeader, Serializable {
    private final int type;
    private final String senderAddr;
    private final String senderName;

    public Header() {
        this.type = 0;
        this.senderAddr = "";
        this.senderName = "";
    }

    public Header(int type, String senderAddr, String senderName) {
        this.type = type;
        this.senderAddr = senderAddr;
        this.senderName = senderName;
    }

    public int getType() {
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
