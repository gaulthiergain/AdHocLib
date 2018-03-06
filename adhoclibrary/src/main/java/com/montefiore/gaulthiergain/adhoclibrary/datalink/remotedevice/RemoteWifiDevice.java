package com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice;

public class RemoteWifiDevice extends AbstractRemoteDevice {

    private String deviceLocalAddress;

    public RemoteWifiDevice(String deviceAddress) {
        super(deviceAddress);
    }

    public RemoteWifiDevice(String deviceAddress, String deviceLocalAddress) {
        super(deviceAddress);
        this.deviceLocalAddress = deviceLocalAddress;
    }

    public String getDeviceLocalAddress() {
        return deviceLocalAddress;
    }

    public void setDeviceLocalAddress(String deviceLocalAddress) {
        this.deviceLocalAddress = deviceLocalAddress;
    }

    @Override
    public String toString() {
        return "RemoteWifiDevice{" +
                "deviceAddress='" + deviceAddress + '\'' +
                ", deviceLocalAddress='" + deviceLocalAddress + '\'' +
                '}';
    }
}
