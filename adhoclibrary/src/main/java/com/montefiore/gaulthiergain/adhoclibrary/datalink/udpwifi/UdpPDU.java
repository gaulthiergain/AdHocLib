package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import java.io.Serializable;

/**
 * <p>This class represents a PDU that is exchanged during control process for UDP wrapper.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class UdpPDU implements Serializable {

    private int port;
    private String hostAddress;

    /**
     * Default constructor
     */
    public UdpPDU() {
    }

    /**
     * Constructor
     *
     * @param port        a String value which represents the remote port.
     * @param hostAddress a String value which represents the remote peer address.
     */
    public UdpPDU(int port, String hostAddress) {
        this.port = port;
        this.hostAddress = hostAddress;
    }

    /**
     * Method allowing to get a remote port.
     *
     * @return a String value which represents the remote port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Method allowing to get a remote peer address.
     *
     * @return a String value which represents the remote peer address.
     */
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
