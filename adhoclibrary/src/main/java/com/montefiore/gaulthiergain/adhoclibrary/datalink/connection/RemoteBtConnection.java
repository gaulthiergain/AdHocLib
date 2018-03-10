package com.montefiore.gaulthiergain.adhoclibrary.datalink.connection;

public class RemoteBtConnection extends AbstractRemoteConnection {

    private final String remoteUUID;
    private String deviceLocalAddress;

    public RemoteBtConnection(String deviceAddress, String deviceName) {
        super(deviceAddress, deviceName);
        this.remoteUUID = deviceAddress.replace(":", "").toLowerCase();
    }

    public RemoteBtConnection(String deviceAddress, String deviceName, String deviceLocalAddress) {
        super(deviceAddress, deviceName);
        this.remoteUUID = deviceAddress.replace(":", "").toLowerCase();
        this.deviceLocalAddress = deviceLocalAddress;
    }

    public String getDeviceLocalAddress() {
        return deviceLocalAddress;
    }

    public String getRemoteUUID() {
        return remoteUUID;
    }

    public void setDeviceLocalAddress(String deviceLocalAddress) {
        this.deviceLocalAddress = deviceLocalAddress;
    }

    @Override
    public String toString() {
        return "RemoteBtConnection{" +
                "deviceAddress='" + deviceAddress + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", remoteUUID='" + remoteUUID + '\'' +
                ", deviceLocalAddress='" + deviceLocalAddress + '\'' +
                '}';
    }
}
