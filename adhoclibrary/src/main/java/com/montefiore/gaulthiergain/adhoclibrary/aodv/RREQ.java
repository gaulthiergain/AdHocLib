package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import java.io.Serializable;

/**
 * <p>This class represents a RREQ message and all theses fields for the AODV protocol. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
class RREQ implements Serializable {
    private final int type;
    private int hopCount;
    private final long rreqId;
    private final String destIpAddress;
    private long sequenceNum;
    private final String originIpAddress;
    //todo lifetime (increased with retries???)

    /**
     * Constructor
     *
     * @param type            an integer value which represents the type of the RREQ message.
     * @param hopCount        an integer value which represents the hops number of the RREQ message.
     * @param rreqId          a long value which represents the broadcastId of the RREQ message.
     * @param destIpAddress   a String value which represents the destination IP address of the RREQ
     *                        message.
     * @param sequenceNum     an integer value which represents the sequence number of the RREQ
     *                        message.
     * @param originIpAddress a String value which represents the source IP address of the RREQ
     *                        message.
     */
    RREQ(int type, int hopCount, long rreqId, String destIpAddress, long sequenceNum,
         String originIpAddress) {
        this.type = type;
        this.hopCount = hopCount;
        this.rreqId = rreqId;
        this.destIpAddress = destIpAddress;
        this.sequenceNum = sequenceNum;
        this.originIpAddress = originIpAddress;
    }

    /**
     * Method allowing to get the type of the RREQ message.
     *
     * @return an integer value which represents the type of the RREQ message.
     */
    int getType() {
        return type;
    }

    /**
     * Method allowing to get the hops number of the RREQ message.
     *
     * @return an integer value which represents the hops number of the RREQ message.
     */
    int getHopCount() {
        return hopCount;
    }

    /**
     * Method allowing to get the broadcastId of the RREQ message.
     *
     * @return a long value which represents the broadcastId.
     */
    long getRreqId() {
        return rreqId;
    }

    /**
     * Method allowing to get the destination IP address of the RREQ message.
     *
     * @return a String value which represents the destination IP address of the RREQ message.
     */
    String getDestIpAddress() {
        return destIpAddress;
    }

    /**
     * Method allowing to get the source IP address of the RREQ message.
     *
     * @return a String value which represents the source IP address of the RREQ message.
     */
    String getOriginIpAddress() {
        return originIpAddress;
    }

    /**
     * Method allowing to get the sequence number of the RREQ message.
     *
     * @return an integer value which represents the sequence number of the RREQ message.
     */
    public long getSequenceNum() {
        return sequenceNum;
    }

    /**
     * Method increment the hop count of the RREQ message.
     */
    void incrementHopCount() {
        this.hopCount++;
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
}

