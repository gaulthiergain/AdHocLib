package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import java.io.Serializable;


public class UdpPDU implements Serializable {
    private int port;
    private String hostAddress;

    public UdpPDU() {
    }

    public UdpPDU(int port, String hostAddress) {
        this.port = port;
        this.hostAddress = hostAddress;
    }

    public int getPort() {
        return port;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    @Override
    public String toString() {
        return "UdpPDU{" +
                "port=" + port +
                ", hostAddress='" + hostAddress + '\'' +
                '}';
    }
}
