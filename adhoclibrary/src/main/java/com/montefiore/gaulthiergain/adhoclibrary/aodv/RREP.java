package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import java.io.Serializable;

class RREP implements Serializable {
    private final int type;
    private int hopCount;
    private final String destIpAddress;
    private long destSeqNum;
    private final String originIpAddress;
    private final long lifetime;

    RREP(int type, int hopCount, String destIpAddress, long destSeqNum, String originIpAddress,
         long lifetime) {
        this.type = type;
        this.hopCount = hopCount;
        this.destIpAddress = destIpAddress;
        this.destSeqNum = destSeqNum;
        this.originIpAddress = originIpAddress;
        this.lifetime = lifetime;
    }

    @Override
    public String toString() {
        return "RREP{" +
                "type=" + type +
                ", hopCount=" + hopCount +
                ", destIpAddress='" + destIpAddress + '\'' +
                ", destSeqNum=" + destSeqNum +
                ", originIpAddress='" + originIpAddress + '\'' +
                ", lifetime=" + lifetime +
                '}';
    }

    public int getType() {
        return type;
    }

    int getHopCount() {
        return hopCount;
    }

    String getDestIpAddress() {
        return destIpAddress;
    }

    long getDestSeqNum() {
        return destSeqNum;
    }

    String getOriginIpAddress() {
        return originIpAddress;
    }

    public long getLifetime() {
        return lifetime;
    }

    void incrementHopCount() {
        this.hopCount++;
    }
}
