package com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice;

public class RemoteBtDevice extends AbstractRemoteDevice {

    private final String deviceName;
    private final String remoteUUID;
    private String deviceLocalAddress;

    public RemoteBtDevice(String deviceAddress, String deviceName) {
        super(deviceAddress);
        this.remoteUUID = deviceAddress.replace(":", "").toLowerCase();
        this.deviceName = deviceName;
    }

    public RemoteBtDevice(String deviceAddress, String deviceName, String deviceLocalAddress) {
        super(deviceAddress);
        this.deviceName = deviceName;
        this.remoteUUID = deviceAddress.replace(":", "").toLowerCase();
        this.deviceLocalAddress = deviceLocalAddress;
    }

    public String getDeviceName() {
        return deviceName;
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
        return "RemoteBtDevice{" +
                "deviceAddress='" + deviceAddress + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", remoteUUID='" + remoteUUID + '\'' +
                ", deviceLocalAddress='" + deviceLocalAddress + '\'' +
                '}';
    }
}
