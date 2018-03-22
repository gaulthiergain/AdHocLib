package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 5/03/18.
 */
@JsonTypeName("UDPmsg")
public class UDPmsg implements Serializable {

    @JsonProperty("ownMac")
    private final String ownMac;
    @JsonProperty("seqNumber")
    private final long seqNumber;
    @JsonProperty("sourceAddress")
    private final String sourceAddress;
    @JsonProperty("destinationAddress")
    private final String destinationAddress;

    public UDPmsg() {
        ownMac = "";
        seqNumber = 0;
        sourceAddress = "";
        destinationAddress = "";
    }

    public UDPmsg(String ownMac, long seqNumber, String sourceAddress, String destinationAddress) {
        this.ownMac = ownMac;
        this.seqNumber = seqNumber;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
    }

    public String getOwnMac() {
        return ownMac;
    }

    public long getSeqNumber() {
        return seqNumber;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    @Override
    public String toString() {
        return "UDPmsg{" +
                "ownMac='" + ownMac + '\'' +
                ", seqNumber=" + seqNumber +
                ", sourceAddress='" + sourceAddress + '\'' +
                ", destinationAddress='" + destinationAddress + '\'' +
                '}';
    }
}
