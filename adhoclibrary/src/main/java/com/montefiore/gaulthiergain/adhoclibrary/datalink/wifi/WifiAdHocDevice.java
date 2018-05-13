package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.os.Parcel;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;

public class WifiAdHocDevice extends AdHocDevice {

    private String ipAddress;
    private int port;

    WifiAdHocDevice(String deviceAddress, String deviceName) {
        super(deviceAddress, deviceName, Service.WIFI);
        this.ipAddress = "";
        this.port = 0;
    }

    public WifiAdHocDevice(String label, String macAddress, String deviceName, int type,
                           String ipAddress) {
        super(label, macAddress, deviceName, type);
        this.ipAddress = ipAddress;
        this.port = 0;
    }

    private WifiAdHocDevice(Parcel in) {
        super(in.readString(), in.readString(), in.readString(), in.readInt(), in.readByte() != 0);
        this.ipAddress = in.readString();
        this.port = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(ipAddress);
        dest.writeInt(port);
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }


    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Interface that must be implemented and provided as a public CREATOR
     * field that generates instances of your Parcelable class from a Parcel.
     */
    public static final Creator<WifiAdHocDevice> CREATOR = new Creator<WifiAdHocDevice>() {
        @Override
        public WifiAdHocDevice createFromParcel(Parcel in) {
            return new WifiAdHocDevice(in);
        }

        @Override
        public WifiAdHocDevice[] newArray(int size) {
            return new WifiAdHocDevice[size];
        }
    };
}
