package com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice;

public abstract class AbstractRemoteDevice {

    final String deviceAddress;

    AbstractRemoteDevice(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }
}
