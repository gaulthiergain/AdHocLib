package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 28/10/17.
 *
 */

public class AdHocBluetoothDevice implements Serializable, Parcelable {

    private final String name;
    private final String address;
    private final int rssi;

    public static final String EXTRA_DEVICE = "com.montefiore.gaulthiergain.adhoclib.bluetooth.extra.DEVICE";
    public static final String EXTRA_NAME = "com.montefiore.gaulthiergain.adhoclib.bluetooth.extra.NAME";
    public static final String EXTRA_ADDR = "com.montefiore.gaulthiergain.adhoclib.bluetooth.extra.ADDRESS";
    public static final String EXTRA_RSSI = "com.montefiore.gaulthiergain.adhoclib.bluetooth.extra.RSSI";


    AdHocBluetoothDevice(String name, String address, int rssi) {
        this.name = name;
        this.address = address;
        this.rssi = rssi;
    }

    private AdHocBluetoothDevice(Parcel in) {
        name = in.readString();
        address = in.readString();
        rssi = in.readInt();
    }

    public static final Creator<AdHocBluetoothDevice> CREATOR = new Creator<AdHocBluetoothDevice>() {
        @Override
        public AdHocBluetoothDevice createFromParcel(Parcel in) {
            return new AdHocBluetoothDevice(in);
        }

        @Override
        public AdHocBluetoothDevice[] newArray(int size) {
            return new AdHocBluetoothDevice[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(address);
        dest.writeInt(rssi);
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getRssi() {
        return rssi;
    }
}
