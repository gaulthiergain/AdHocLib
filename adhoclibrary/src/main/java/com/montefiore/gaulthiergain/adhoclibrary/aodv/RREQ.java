package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import java.io.Serializable;

class RREQ implements Serializable {
    private final int type;
    private int hopCount;
    private final long rreqId;
    private final String destIpAddress;
    private long destSeqNum;
    private final String originIpAddress;
    private final long originSeqNum;
    //todo lifetime (increased with retries???)

    RREQ(int type, int hopCount, long rreqId, String destIpAddress, long destSeqNum, String originIpAddress,
         long originSeqNum) {
        this.type = type;
        this.hopCount = hopCount;
        this.rreqId = rreqId;
        this.destIpAddress = destIpAddress;
        this.destSeqNum = destSeqNum;
        this.originIpAddress = originIpAddress;
        this.originSeqNum = originSeqNum;
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

    long getDestSeqNum() {
        return destSeqNum;
    }

    String getOriginIpAddress() {
        return originIpAddress;
    }

    long getOriginSeqNum() {
        return originSeqNum;
    }

    @Override
    public String toString() {
        return "RREQ{" +
                "type=" + type +
                ", hopCount=" + hopCount +
                ", rreqId=" + rreqId +
                ", destIpAddress='" + destIpAddress + '\'' +
                ", destSeqNum='" + destSeqNum + '\'' +
                ", originIpAddress='" + originIpAddress + '\'' +
                ", originSeqNum='" + originSeqNum + '\'' +
                '}';
    }

    void incrementHopCount() {
        this.hopCount++;
    }
}

