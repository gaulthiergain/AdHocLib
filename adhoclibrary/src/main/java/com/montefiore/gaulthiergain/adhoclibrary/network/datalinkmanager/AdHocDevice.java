package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

/**
 * Created by gaulthiergain on 9/03/18.
 */

public class AdHocDevice {

    private final String mac;
    private final String name;
    private final byte type;

    public AdHocDevice(String mac, String name, byte type) {
        this.mac = mac;
        this.name = name;
        this.type = type;
    }

    public AdHocDevice(String mac, String name) {
        this.mac = mac;
        this.name = name;
        this.type = DataLinkManager.BLUETOOTH;
    }

    public String getName() {
        return name;
    }

    public byte getType() {
        return type;
    }

    public String getMac() {

        return mac;
    }

    @Override
    public String toString() {
        return mac + " - " + name + " - " + display(type);
    }

    private String display(byte type) {
        if (type == DataLinkManager.BLUETOOTH) {
            return "Bt";
        }

        return "Wifi";
    }
}
