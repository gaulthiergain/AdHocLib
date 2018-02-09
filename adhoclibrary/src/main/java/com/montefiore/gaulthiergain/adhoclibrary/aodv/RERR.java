package com.montefiore.gaulthiergain.adhoclibrary.aodv;

/**
 * Created by gaulthiergain on 9/02/18.
 */


import java.io.Serializable;

public class RERR implements Serializable {
    private final int type;
    private int hopCount;
    private final String unreachableDestIpAddress;
    private long unreachableDestSeqNum;

    public RERR(int type, int hopCount, String unreachableDestIpAddress, long unreachableDestSeqNum) {
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

    public int getType() {
        return type;
    }

    public int getHopCount() {
        return hopCount;
    }

    public String getUnreachableDestIpAddress() {
        return unreachableDestIpAddress;
    }

    public long getUnreachableDestSeqNum() {
        return unreachableDestSeqNum;
    }
}

