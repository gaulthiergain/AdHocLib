package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import java.io.Serializable;

public class Data implements Serializable {
    private final String destIpAddress;
    private Serializable pdu;

    public Data(String destIpAddress, Serializable pdu) {
        this.destIpAddress = destIpAddress;
        this.pdu = pdu;
    }

    public String getDestIpAddress() {
        return destIpAddress;
    }

    public Serializable getPdu() {
        return pdu;
    }

    @Override
    public String toString() {
        return "DATA{" +
                "destIpAddress='" + destIpAddress + '\'' +
                ", pdu=" + pdu +
                '}';
    }
}
