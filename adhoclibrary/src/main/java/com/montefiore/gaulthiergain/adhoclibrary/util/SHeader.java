package com.montefiore.gaulthiergain.adhoclibrary.util;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SHeader")
public class SHeader extends Header {

    private String address;
    private final String mac;

    public SHeader() {
        super(0, "", "");
        this.address = "";
        this.mac = "";
    }

    public SHeader(int type, String mac, String label, String name) {
        this.type = type;
        this.mac = mac;
        this.label = label;
        this.name = name;
    }

    public SHeader(int type, String address, String mac, String label, String name) {
        this.type = type;
        this.address = address;
        this.mac = mac;
        this.label = label;
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public String getMac() {
        return mac;
    }

    @Override
    public String toString() {
        return "Header{" +
                "type=" + type +
                ", address='" + address + '\'' +
                ", macAddress='" + mac + '\'' +
                ", labelAddress='" + label + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
