package com.montefiore.gaulthiergain.adhoclibrary.aodv;

/**
 * Created by gaulthiergain on 9/02/18.
 */


import java.io.Serializable;

class RERR implements Serializable {
    private final int type;
    private int hopCount;
    private final String unreachableDestIpAddress;
    private long unreachableDestSeqNum;

    RERR(int type, int hopCount, String unreachableDestIpAddress, long unreachableDestSeqNum) {
        this.type = type;
        this.hopCount = hopCount;
        this.unreachableDestIpAddress = unreachableDestIpAddress;
        this.unreachableDestSeqNum = unreachableDestSeqNum;
    }

    @Override
    public String toString() {
        return "RERR{" +
                "type=" + type +
                ", hopCount=" + hopCount +
                ", unreachableDestIpAddress='" + unreachableDestIpAddress + '\'' +
                ", unreachableDestSeqNum=" + unreachableDestSeqNum +
                '}';
    }

    int getType() {
        return type;
    }

    int getHopCount() {
        return hopCount;
    }

    String getUnreachableDestIpAddress() {
        return unreachableDestIpAddress;
    }

    long getUnreachableDestSeqNum() {
        return unreachableDestSeqNum;
    }
}

