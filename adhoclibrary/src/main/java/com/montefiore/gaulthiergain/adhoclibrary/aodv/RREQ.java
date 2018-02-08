package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import java.io.Serializable;

public class RREQ implements Serializable {
    private final int type;
    private int hopCount;
    private final long rreqId;
    private final String destIpAddress;
    private long destSeqNum;
    private final String originIpAddress;
    private final long originSeqNum;
    //todo lifetime (increased with retries???)

    public RREQ(int type, int hopCount, long rreqId, String destIpAddress, long destSeqNum, String originIpAddress,
                long originSeqNum) {
        this.type = type;
        this.hopCount = hopCount;
        this.rreqId = rreqId;
        this.destIpAddress = destIpAddress;
        this.destSeqNum = destSeqNum;
        this.originIpAddress = originIpAddress;
        this.originSeqNum = originSeqNum;
    }

    public int getType() {
        return type;
    }

    public int getHopCount() {
        return hopCount;
    }

    public long getRreqId() {
        return rreqId;
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

    public long getOriginSeqNum() {
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

    public void incrementHopCount() {
        this.hopCount++;
    }
}

