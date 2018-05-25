package com.montefiore.gaulthiergain.adhoclibrary.network.aodv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.io.Serializable;

/**
 * <p>This class represents a RREP message and all theses fields for the AODV protocol. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

@JsonTypeName("RREP")
public class RREP extends AodvMessage {

    @JsonProperty("hopCount")
    private int hopCount;
    @JsonProperty("destIpAddress")
    private final String destIpAddress;
    @JsonProperty("sequenceNum")
    private long sequenceNum;
    @JsonProperty("originIpAddress")
    private final String originIpAddress;
    @JsonProperty("lifetime")
    private final long lifetime;

    /**
     * Default constructor
     */
    public RREP() {
        super(0);
        hopCount = 0;
        destIpAddress = "";
        sequenceNum = 0;
        originIpAddress = "";
        lifetime = 0;
    }

    /**
     * Constructor
     *
     * @param type            an integer value which represents the type of the RREP message.
     * @param hopCount        an integer value which represents the hops number of the RREP message.
     * @param destIpAddress   a String value which represents the destination IP address of the RREP
     *                        message.
     * @param sequenceNum     a long value which represents the sequence number of the RREP
     *                        message
     * @param originIpAddress a String value which represents the source IP address of the RREP
     *                        message.
     * @param lifetime        a long value which represents the lifetime of the RREP message.
     */
    public RREP(int type, int hopCount, String destIpAddress, long sequenceNum, String originIpAddress,
                long lifetime) {
        super(type);
        this.hopCount = hopCount;
        this.destIpAddress = destIpAddress;
        this.sequenceNum = sequenceNum;
        this.originIpAddress = originIpAddress;
        this.lifetime = lifetime;
    }

    /**
     * Method allowing to get the hops number of the RREP message.
     *
     * @return an integer value which represents the hops number of the RREP message.
     */
    int getHopCount() {
        return hopCount;
    }

    /**
     * Method allowing to get the destination IP address of the RREP message.
     *
     * @return a String value which represents the destination IP address of the RREP message.
     */
    String getDestIpAddress() {
        return destIpAddress;
    }

    /**
     * Method allowing to get the sequence number of the RREP message.
     *
     * @return an integer value which represents the sequence number of the RREP message.
     */
    long getSequenceNum() {
        return sequenceNum;
    }

    /**
     * Method allowing to get the source IP address of the RREP message.
     *
     * @return a String value which represents the source IP address of the RREP message.
     */
    String getOriginIpAddress() {
        return originIpAddress;
    }

    /**
     * Method allowing to get the lifetime of the RREP message.
     *
     * @return a long value which represents the lifetime of the RREP message.
     */
    public long getLifetime() {
        return lifetime;
    }

    /**
     * Method increment the hop count of the RREP message.
     *
     * @return an integer which represents the hop count incremented
     */
    public int incrementHopCount() {
        return ++this.hopCount;
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
}
