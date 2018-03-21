package com.montefiore.gaulthiergain.adhoclibrary.datalink.connection;

public class RemoteConnection {

    String deviceAddress;
    String deviceName;

    public RemoteConnection(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public RemoteConnection(String deviceAddress, String deviceName) {
        this.deviceAddress = deviceAddress;
        this.deviceName = deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }
}
