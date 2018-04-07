package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.montefiore.gaulthiergain.adhoclibrary.util.SHeader;

/**
 * Created by gaulthiergain on 5/03/18.
 */
@JsonTypeName("UdpHeader")
public class UdpHeader extends SHeader {

    private final String destAddress;

    public UdpHeader() {
        super();
        this.destAddress = "";
    }

    public UdpHeader(int type, String address, String mac, String label, String name, String destAddress) {
        super(type, address, mac, label, name);
        this.destAddress = destAddress;
    }

    public String getDestinationAddress() {
        return destAddress;
    }

    @Override
    public String toString() {
        return "UdpHeader{" +
                "destAddress='" + destAddress + '\'' +
                ", type=" + type +
                ", label='" + label + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
