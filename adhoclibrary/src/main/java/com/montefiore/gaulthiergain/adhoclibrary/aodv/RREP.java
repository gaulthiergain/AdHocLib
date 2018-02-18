package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import java.io.Serializable;

class RREP implements Serializable {
    private final int type;
    private int hopCount;
    private final String destIpAddress;
    private long sequenceNum;
    private final String originIpAddress;
    private final long lifetime;

    RREP(int type, int hopCount, String destIpAddress, long sequenceNum, String originIpAddress,
         long lifetime) {
        this.type = type;
        this.hopCount = hopCount;
        this.destIpAddress = destIpAddress;
        this.sequenceNum = sequenceNum;
        this.originIpAddress = originIpAddress;
        this.lifetime = lifetime;
    }

    @Override
    public String toString() {
        return "RREP{" +
                "type=" + type +
                ", hopCount=" + hopCount +
                ", destIpAddress='" + destIpAddress + '\'' +
                ", destSeqNum=" + sequenceNum +
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

    long getSequenceNum() {
        return sequenceNum;
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
