package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

/**
 * Created by gaulthiergain on 9/03/18.
 */

public class AdHocDevice {

    private final String address;
    private final String name;
    private final byte type;

    public AdHocDevice(String address, String name, byte type) {
        this.address = address;
        this.name = name;
        this.type = type;
    }

    public AdHocDevice(String address, String name) {
        this.address = address;
        this.name = name;
        this.type = DataLinkManager.BLUETOOTH;
    }

    public String getName() {
        return name;
    }

    public byte getType() {
        return type;
    }

    public String getAddress() {

        return address;
    }

    @Override
    public String toString() {
        return address + " - " + name + " - " + display(type);
    }

    private String display(byte type) {
        if (type == DataLinkManager.BLUETOOTH) {
            return "Bt";
        }

        return "Wifi";
    }
}
