package com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice;

public abstract class AbstractRemoteDevice {

    String deviceAddress;

    AbstractRemoteDevice(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }
}
