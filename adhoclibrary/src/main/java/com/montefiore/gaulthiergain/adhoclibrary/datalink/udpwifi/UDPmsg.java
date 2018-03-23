package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 5/03/18.
 */
@JsonTypeName("UdpMsg")
public class UdpMsg implements Serializable {

    @JsonProperty("ownMac")
    private final String ownMac;
    @JsonProperty("sourceAddress")
    private final String sourceAddress;
    @JsonProperty("destinationAddress")
    private final String destinationAddress;

    public UdpMsg() {
        ownMac = "";
        sourceAddress = "";
        destinationAddress = "";
    }

    public UdpMsg(String ownMac, String sourceAddress, String destinationAddress) {
        this.ownMac = ownMac;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
    }

    public String getOwnMac() {
        return ownMac;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    @Override
    public String toString() {
        return "UdpMsg{" +
                "ownMac='" + ownMac + '\'' +
                ", sourceAddress='" + sourceAddress + '\'' +
                ", destinationAddress='" + destinationAddress + '\'' +
                '}';
    }
}
