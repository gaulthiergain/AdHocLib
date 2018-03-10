package com.montefiore.gaulthiergain.adhoclibrary.datalink.connection;

public class RemoteWifiConnection extends AbstractRemoteConnection {

    private String deviceLocalAddress;

    public RemoteWifiConnection(String deviceAddress) {
        super(deviceAddress);
    }

    public RemoteWifiConnection(String deviceAddress, String deviceLocalAddress) {
        super(deviceAddress);
        this.deviceLocalAddress = deviceLocalAddress;
    }

    public RemoteWifiConnection(String deviceAddress, String deviceName, String deviceLocalAddress) {
        super(deviceAddress, deviceName);
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
        return "RemoteWifiConnection{" +
                "deviceAddress='" + deviceAddress + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceLocalAddress='" + deviceLocalAddress + '\'' +
                '}';
    }
}
