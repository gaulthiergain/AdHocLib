package com.montefiore.gaulthiergain.adhoclibrary.datalink.connection;

public abstract class AbstractRemoteConnection {

    String deviceAddress;
    String deviceName;

    AbstractRemoteConnection(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public AbstractRemoteConnection(String deviceAddress, String deviceName) {
        this.deviceAddress = deviceAddress;
        this.deviceName = deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
