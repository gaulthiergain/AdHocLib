package com.montefiore.gaulthiergain.adhoclibrary.util;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 17/11/17.
 */

@JsonTypeName("Header")
public class Header implements Serializable {
    protected int type;
    protected String label;
    protected String name;

    private String address;
    private String mac;

    public Header() {

    }

    public Header(int type, String label, String name) {
        this.type = type;
        this.label = label;
        this.name = name;
    }

    public Header(int type, String mac, String label, String name) {
        this.type = type;
        this.mac = mac;
        this.label = label;
        this.name = name;
    }

    public Header(int type, String address, String mac, String label, String name) {
        this.type = type;
        this.address = address;
        this.mac = mac;
        this.label = label;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
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
                ", label='" + label + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", mac='" + mac + '\'' +
                '}';
    }
}