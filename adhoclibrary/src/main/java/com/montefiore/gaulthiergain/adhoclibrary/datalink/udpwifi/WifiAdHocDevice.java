package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

/**
 * Created by gaulthiergain on 22/03/18.
 */

public class WifiAdHocDevice {
    private final String name;
    private final String ipAddress;
    private final String macAddress;

    public WifiAdHocDevice(String name, String ipAddress, String macAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public String toString() {
        return "WifiAdHocDevice{" +
                "name='" + name + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", macAddress='" + macAddress + '\'' +
                '}';
    }
}