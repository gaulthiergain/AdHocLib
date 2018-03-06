package com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice;

public class RemoteBtDevice extends AbstractRemoteDevice {

    private final String deviceName;
    private String deviceLocalAddress;

    public RemoteBtDevice(String deviceAddress, String deviceName) {
        super(deviceAddress);
        this.deviceName = deviceName;
    }

    public RemoteBtDevice(String deviceAddress, String deviceName, String deviceLocalAddress) {
        super(deviceAddress);
        this.deviceName = deviceName;
        this.deviceLocalAddress = deviceLocalAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceLocalAddress() {
        return deviceLocalAddress;
    }

    public void setDeviceLocalAddress(String deviceLocalAddress) {
        this.deviceLocalAddress = deviceLocalAddress;
    }

    @Override
    public String toString() {
        return "RemoteBtDevice{" +
                "deviceAddress='" + deviceAddress + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceLocalAddress='" + deviceLocalAddress + '\'' +
                '}';
    }
}
