package com.montefiore.gaulthiergain.adhoclibrary.network.aodv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.io.Serializable;

/**
 * <p>This class represents a RREQ message and all theses fields for the AODV protocol. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

@JsonTypeName("RREQ")
public class RREQ extends AodvMessage {

    @JsonProperty("hopCount")
    private int hopCount;
    @JsonProperty("rreqId")
    private final long rreqId;
    @JsonProperty("destSequenceNum")
    private long destSequenceNum;
    @JsonProperty("destIpAddress")
    private final String destIpAddress;
    @JsonProperty("originSequenceNum")
    private long originSequenceNum;
    @JsonProperty("originIpAddress")
    private final String originIpAddress;

    /**
     * Default Constructor
     */
    public RREQ() {
        super(0);
        hopCount = 0;
        destIpAddress = "";
        originIpAddress = "";
        rreqId = 0;
    }

    /**
     * Constructor
     *
     * @param type              an integer value which represents the type of the RREQ message.
     * @param hopCount          an integer value which represents the hops number of the RREQ message.
     * @param rreqId            a long value which represents the broadcastId of the RREQ message.
     * @param destSequenceNum   a long value which represents the destination sequence number of the RREQ
     *                          message.
     * @param destIpAddress     a String value which represents the destination IP address of the RREQ
     *                          message.
     * @param originSequenceNum a long value which represents the originator sequence number of the RREQ
     *                          message.
     * @param originIpAddress   a String value which represents the originator IP address of the RREQ
     *                          message.
     */
    public RREQ(int type, int hopCount, long rreqId, long destSequenceNum, String destIpAddress, long originSequenceNum,
                String originIpAddress) {
        super(type);
        this.hopCount = hopCount;
        this.rreqId = rreqId;
        this.destSequenceNum = destSequenceNum;
        this.destIpAddress = destIpAddress;
        this.originSequenceNum = originSequenceNum;
        this.originIpAddress = originIpAddress;
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
     * Method allowing to get the destination sequence number of the RREQ message.
     *
     * @return a long value which represents the destination sequence number of the RREQ message.
     */
    public long getDestSequenceNum() {
        return destSequenceNum;
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
     * Method allowing to get the originator IP address of the RREQ message.
     *
     * @return a String value which represents the originator IP address of the RREQ message.
     */
    String getOriginIpAddress() {
        return originIpAddress;
    }

    /**
     * Method allowing to get the originator sequence number of the RREQ message.
     *
     * @return a long value which represents the originator sequence number of the RREQ message.
     */
    public long getOriginSequenceNum() {
        return originSequenceNum;
    }

    /**
     * Method increment the hop count of the RREQ message.
     */
    void incrementHopCount() {
        this.hopCount++;
    }

    /**
     * Method allowing to update the destination sequence number
     *
     * @param destSequenceNum a long value which represents the destination sequence number of the RREQ message.
     */
    public void setDestSequenceNum(long destSequenceNum) {
        this.destSequenceNum = destSequenceNum;
    }

    @Override
    public String toString() {
        return "RREQ{" +
                "type=" + type +
                ", hopCount=" + hopCount +
                ", rreqId=" + rreqId +
                ", destSequenceNum=" + destSequenceNum +
                ", destIpAddress='" + destIpAddress + '\'' +
                ", originSequenceNum=" + originSequenceNum +
                ", originIpAddress='" + originIpAddress + '\'' +
                '}';
    }
}