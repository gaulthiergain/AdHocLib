package com.montefiore.gaulthiergain.adhoclibrary.datalink.connection;

public class RemoteBtConnection extends RemoteConnection {

    private final String remoteUUID;

    public RemoteBtConnection(String deviceAddress, String deviceName) {
        super(deviceAddress, deviceName);
        this.remoteUUID = deviceAddress.replace(":", "").toLowerCase();
    }

    public String getRemoteUUID() {
        return remoteUUID;
    }

    @Override
    public String toString() {
        return "RemoteBtConnection{" +
                "deviceAddress='" + deviceAddress + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", remoteUUID='" + remoteUUID + '\'' +
                '}';
    }
}
