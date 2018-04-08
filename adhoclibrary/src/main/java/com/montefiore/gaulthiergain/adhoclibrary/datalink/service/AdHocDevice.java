package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

public class AdHocDevice {

    protected String label;
    protected final String deviceName;
    protected final String macAddress;
    protected int type;

    public AdHocDevice(String macAddress, String deviceName, int type) {
        this.macAddress = macAddress;
        this.deviceName = checkName(deviceName);
        this.type = type;
    }

    public AdHocDevice(String label, String macAddress, String deviceName, int type) {
        this.label = label;
        this.macAddress = macAddress;
        this.deviceName = checkName(deviceName);
        this.type = type;
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

    @Override
    public String toString() {
        return "AdHocDevice{" +
                "label='" + label + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", type=" + display(type) +
                '}';
    }

    private String display(int type) {
        switch (type) {
            case Service.BLUETOOTH:
                return "Bluetooth";
            case Service.WIFI:
                return "Wifi";
            default:
                return "UNKNOWN";
        }
    }
}
