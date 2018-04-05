package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

/**
 * Created by gaulthiergain on 22/03/18.
 */

public class WifiUdpDevice {
    private final String name;
    private final String ipAddress;

    public WifiUdpDevice(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }


    @Override
    public String toString() {
        return "WifiUdpDevice{" +
                "name='" + name + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}