package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.os.Parcel;
import android.os.Parcelable;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;

public class WifiAdHocDevice extends AdHocDevice implements Parcelable {

    WifiAdHocDevice(String deviceAddress, String deviceName) {
        super(deviceAddress, deviceName, Service.WIFI);
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
        dest.writeString(deviceAddress);
        dest.writeString(deviceName);
        dest.writeInt(type);
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
