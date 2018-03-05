package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 5/03/18.
 */

public class UDPmsg implements Serializable {

    private final String ownMac;
    private final long seqNumber;
    private final String destinationAddress;

    public UDPmsg(String ownMac, long seqNumber, String destinationAddress) {
        this.ownMac = ownMac;
        this.seqNumber = seqNumber;
        this.destinationAddress= destinationAddress;
    }

    public String getOwnMac() {
        return ownMac;
    }

    public long getSeqNumber() {
        return seqNumber;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    @Override
    public String toString() {
        return "UDPmsg{" +
                "ownMac='" + ownMac + '\'' +
                ", seqNumber=" + seqNumber +
                ", destinationAddress='" + destinationAddress + '\'' +
                '}';
    }
}
