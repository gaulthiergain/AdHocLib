package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class AdHocDevice implements Parcelable, Serializable {

    protected String label;
    protected final String deviceName;
    protected final String macAddress;
    protected final int type;

    private boolean directedConnected;

    public AdHocDevice() {
        this.deviceName = null;
        this.macAddress = null;
        this.type = 0;
    }

    public AdHocDevice(String macAddress, String deviceName, int type) {
        this.macAddress = macAddress;
        this.deviceName = checkName(deviceName);
        this.type = type;
        this.directedConnected = true;
    }

    public AdHocDevice(String label, String macAddress, String deviceName, int type) {
        this.label = label;
        this.macAddress = macAddress;
        this.deviceName = checkName(deviceName);
        this.type = type;
        this.directedConnected = true;
    }

    public AdHocDevice(String label, String macAddress, String deviceName, int type,
                       boolean directedConnected) {
        this(label, macAddress, deviceName, type);
        this.directedConnected = directedConnected;
    }

    public AdHocDevice(String label) {
        this();
        this.directedConnected = false;
        this.label = label;
    }

    private String checkName(String deviceName) {
        if (deviceName == null) {
            return "";
        }

        return deviceName;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getType() {
        return type;
    }

    @JsonIgnore
    public String getStringType() {
        return display(type);
    }

    public boolean isDirectedConnected() {
        return directedConnected;
    }

    protected String display(int type) {
        switch (type) {
            case Service.BLUETOOTH:
                return "Bluetooth";
            case Service.WIFI:
                return "Wifi";
            default:
                return "UNKNOWN";
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label);
        dest.writeString(macAddress);
        dest.writeString(deviceName);
        dest.writeInt(type);
        dest.writeByte((byte) (directedConnected ? 1 : 0));
    }

    private AdHocDevice(Parcel in) {
        this(in.readString(), in.readString(), in.readString(), in.readInt(), in.readByte() != 0);
    }

    protected static final Creator<AdHocDevice> CREATOR = new Creator<AdHocDevice>() {
        @Override
        public AdHocDevice createFromParcel(Parcel in) {
            return new AdHocDevice(in);
        }

        @Override
        public AdHocDevice[] newArray(int size) {
            return new AdHocDevice[size];
        }
    };

    @Override
    public String toString() {
        return "AdHocDevice{" +
                "label='" + label + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", type=" + display(type) +
                ", directedConnected=" + directedConnected +
                '}';
    }
}
