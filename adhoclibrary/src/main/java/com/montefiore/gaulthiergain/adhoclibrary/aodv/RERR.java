package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import java.io.Serializable;

/**
 * <p>This class represents a RERR message and all theses fields for the AODV protocol. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
class RERR implements Serializable {
    private final int type;
    private int hopCount;
    private final String unreachableDestIpAddress;
    private long unreachableDestSeqNum;

    /**
     * Constructor
     *
     * @param type                     an integer value which represents the type of the RERR
     *                                 message.
     * @param hopCount                 an integer value which represents the hops number of the RERR
     *                                 message.
     * @param unreachableDestIpAddress a String value which represents the unreachable IP
     *                                 destination address of the RERR message.
     * @param unreachableDestSeqNum    a long value which represents unreachable sequence number
     *                                 of the RERR message.
     */
    RERR(int type, int hopCount, String unreachableDestIpAddress, long unreachableDestSeqNum) {
        this.type = type;
        this.hopCount = hopCount;
        this.unreachableDestIpAddress = unreachableDestIpAddress;
        this.unreachableDestSeqNum = unreachableDestSeqNum;
    }

    /**
     * Method allowing to get the type of the RERR message.
     *
     * @return an integer value which represents the type of the RERR message.
     */
    int getType() {
        return type;
    }

    /**
     * Method allowing to get the hops number of the RERR message.
     *
     * @return an integer value which represents the hops number of the RERR message.
     */
    int getHopCount() {
        return hopCount;
    }

    /**
     * Method allowing to get the unreachable IP destination address of the RERR message.
     *
     * @return a String value which represents the unreachable IP destination address of the RERR
     * message.
     */
    String getUnreachableDestIpAddress() {
        return unreachableDestIpAddress;
    }

    /**
     * Method allowing to get the source IP address of the RERR message.
     *
     * @return a long value which represents unreachable sequence number of the RERR message.
     */
    long getUnreachableDestSeqNum() {
        return unreachableDestSeqNum;
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
}

