package com.montefiore.gaulthiergain.adhoclibrary.auto;


public abstract class SmartDevice {
    protected final String name;
    protected final String address;

    public SmartDevice(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
