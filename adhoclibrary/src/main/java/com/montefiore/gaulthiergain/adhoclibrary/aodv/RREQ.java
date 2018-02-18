package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import java.io.Serializable;

class RREQ implements Serializable {
    private final int type;
    private int hopCount;
    private final long rreqId;
    private final String destIpAddress;
    private long sequenceNum;
    private final String originIpAddress;
    //todo lifetime (increased with retries???)

    RREQ(int type, int hopCount, long rreqId, String destIpAddress, long sequenceNum,
         String originIpAddress) {
        this.type = type;
        this.hopCount = hopCount;
        this.rreqId = rreqId;
        this.destIpAddress = destIpAddress;
        this.sequenceNum = sequenceNum;
        this.originIpAddress = originIpAddress;
    }

    int getType() {
        return type;
    }

    int getHopCount() {
        return hopCount;
    }

    long getRreqId() {
        return rreqId;
    }

    String getDestIpAddress() {
        return destIpAddress;
    }

    String getOriginIpAddress() {
        return originIpAddress;
    }

    public long getSequenceNum() {
        return sequenceNum;
    }

    @Override
    public String toString() {
        return "RREQ{" +
                "type=" + type +
                ", hopCount=" + hopCount +
                ", rreqId=" + rreqId +
                ", destIpAddress='" + destIpAddress + '\'' +
                ", sequenceNum='" + sequenceNum + '\'' +
                ", originIpAddress='" + originIpAddress + '\'' +
                '}';
    }

    void incrementHopCount() {
        this.hopCount++;
    }
}

