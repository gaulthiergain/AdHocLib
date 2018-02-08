package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import java.io.Serializable;

public class RREP implements Serializable {
    private final int type;
    private int hopCount;
    private final String destIpAddress;
    private long destSeqNum;
    private final String originIpAddress;
    private final long lifetime;

    public RREP(int type, int hopCount, String destIpAddress, long destSeqNum, String originIpAddress,
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
                ", destIpAddress='" + destIpAddress.substring(24, 36) + '\'' +
                ", destSeqNum=" + destSeqNum +
                ", originIpAddress='" + originIpAddress.substring(24, 36) + '\'' +
                ", lifetime=" + lifetime +
                '}';
    }

    public int getType() {
        return type;
    }

    public int getHopCount() {
        return hopCount;
    }

    public String getDestIpAddress() {
        return destIpAddress;
    }

    public long getDestSeqNum() {
        return destSeqNum;
    }

    public String getOriginIpAddress() {
        return originIpAddress;
    }

    public long getLifetime() {
        return lifetime;
    }

    public void incrementHopCount() {
        this.hopCount++;
    }
}
