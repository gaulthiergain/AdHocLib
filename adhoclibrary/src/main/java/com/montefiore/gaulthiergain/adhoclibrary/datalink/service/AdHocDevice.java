package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

public class AdHocDevice {

    protected String label;
    protected final String deviceName;
    protected final String macAddress;
    protected int type;

    public AdHocDevice(String macAddress, String deviceName, int type) {
        this.macAddress = macAddress;
        this.deviceName = deviceName;
        this.type = type;
    }

    public AdHocDevice(String label, String macAddress, String deviceName, int type) {
        this.label = label;
        this.macAddress = macAddress;
        this.deviceName = deviceName;
        this.type = type;
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
                ", type=" + type +
                '}';
    }

    private String display(int type) {
        if (type == Service.BLUETOOTH) {
            return "Bluetooth";
        }

        return "Wifi";
    }
}
