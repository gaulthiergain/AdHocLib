package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 28/10/17.
 *
 */

public class BluetoothAdHocDevice implements Parcelable {

    public static final String EXTRA_DEVICE = "com.montefiore.gaulthiergain.adhoclib.bluetooth.extra.DEVICE";

    private final String uuid;
    private final int rssi;
    private final BluetoothDevice device;


    BluetoothAdHocDevice(BluetoothDevice device) {
        this.device = device;
        this.uuid = "983d081a-86e6-4581-8bcb-e5b28013ea08";
        this.rssi = -1;
    }

    BluetoothAdHocDevice(BluetoothDevice device, int rssi) {
        //this.uuid = "e0917680-d427-11e4-8830-" + device.getAddress().replace(":", ""); TODO
        this.uuid = "983d081a-86e6-4581-8bcb-e5b28013ea08";
        this.rssi = rssi;
        this.device = device;
    }

    private BluetoothAdHocDevice(Parcel in) {
        this.uuid = in.readString();
        this.rssi = in.readInt();
        this.device = in.readParcelable(BluetoothDevice.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uuid);
        dest.writeInt(rssi);
        dest.writeParcelable(device, flags);
    }

    public static final Creator<BluetoothAdHocDevice> CREATOR = new Creator<BluetoothAdHocDevice>() {
        @Override
        public BluetoothAdHocDevice createFromParcel(Parcel in) {
            return new BluetoothAdHocDevice(in);
        }

        @Override
        public BluetoothAdHocDevice[] newArray(int size) {
            return new BluetoothAdHocDevice[size];
        }
    };

    public String getUuid() {
        return uuid;
    }

    public int getRssi() {
        return rssi;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    @Override
    public String toString() {
        return device.getAddress() + " - " + device.getName();
    }


}
