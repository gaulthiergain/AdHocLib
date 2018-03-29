package com.montefiore.gaulthiergain.adhoclibrary.network.aodv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * <p>This class represents a RERR message and all theses fields for the AODV protocol. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
@JsonTypeName("RERR")
public class RERR {

    @JsonProperty("type")
    private final int type;
    @JsonProperty("unreachableDestIpAddress")
    private final String unreachableDestIpAddress;
    @JsonProperty("unreachableDestSeqNum")
    private final long unreachableDestSeqNum;

    public RERR() {
        this.type = 0;
        this.unreachableDestIpAddress = "";
        this.unreachableDestSeqNum = 0;
    }

    /**
     * Constructor
     *
     * @param type                     an integer value which represents the type of the RERR
     *                                 message.
     * @param unreachableDestIpAddress a String value which represents the unreachable IP
     *                                 destination address of the RERR message.
     * @param unreachableDestSeqNum    a long value which represents unreachable sequence number
     *                                 of the RERR message.
     */
    public RERR(int type, String unreachableDestIpAddress, long unreachableDestSeqNum) {
        this.type = type;
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
                ", unreachableDestIpAddress='" + unreachableDestIpAddress + '\'' +
                ", unreachableDestSeqNum=" + unreachableDestSeqNum +
                '}';
    }
}

