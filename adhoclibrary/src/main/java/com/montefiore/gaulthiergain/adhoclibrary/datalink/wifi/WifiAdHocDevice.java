package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.os.Parcel;
import android.os.Parcelable;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;

public class WifiAdHocDevice extends AdHocDevice implements Parcelable {

    private String ipAddress;

    WifiAdHocDevice(String deviceAddress, String deviceName) {
        super(deviceAddress, deviceName, Service.WIFI);
    }

    public WifiAdHocDevice(String label, String macAddress, String deviceName, int type, String ipAddress) {
        super(label, macAddress, deviceName, type);
        this.ipAddress = ipAddress;
    }

    private WifiAdHocDevice(Parcel in) {
        super(in.readString(), in.readString(), in.readInt());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label);
        dest.writeString(deviceName);
        dest.writeString(macAddress);
        dest.writeInt(type);
        dest.writeString(ipAddress);
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }


    public String getIpAddress() {
        return ipAddress;
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
