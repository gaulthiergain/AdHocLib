package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

/**
 * Created by gaulthiergain on 9/03/18.
 */

public class DiscoveredDevice {

    public static final byte BLUETOOTH = 1;
    public static final byte WIFI = 2;


    private final String address;
    private final String name;
    private final byte type;

    public DiscoveredDevice(String address, String name, byte type) {
        this.address = address;
        this.name = name;
        this.type = type;
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
        return "DiscoveredDevice{" +
                "address='" + address + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
