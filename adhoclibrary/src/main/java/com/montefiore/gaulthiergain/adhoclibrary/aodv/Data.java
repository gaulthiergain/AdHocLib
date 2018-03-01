package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import java.io.Serializable;

/**
 * <p>This class represents a DATA message and all theses fields for the AODV protocol. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class Data implements Serializable {

    private final String destIpAddress;
    private Serializable pdu;

    /**
     * Constructor
     *
     * @param destIpAddress a String value which represents the destination IP address of the
     *                      DATA message.
     * @param pdu           a Serializable value which represents the PDU of the DATA message.
     */
    public Data(String destIpAddress, Serializable pdu, RREQ rreq) {
        this.destIpAddress = destIpAddress;
        this.pdu = pdu;
    }

    public Data(String destIpAddress, Serializable pdu) {
        this.destIpAddress = destIpAddress;
        this.pdu = pdu;
    }

    /**
     * Method allowing to get the destination IP address of the DATA message.
     *
     * @return a String value which represents the destination IP address of the DATA message.
     */
    public String getDestIpAddress() {
        return destIpAddress;
    }

    /**
     * Method allowing to get the PDU of the DATA message.
     *
     * @return a Serializable object which represents the PDU of the DATA message.
     */
    public Serializable getPdu() {
        return pdu;
    }

    @Override
    public String toString() {
        return "Data{" +
                "destIpAddress='" + destIpAddress + '\'' +
                ", pdu=" + pdu +
                '}';
    }
}