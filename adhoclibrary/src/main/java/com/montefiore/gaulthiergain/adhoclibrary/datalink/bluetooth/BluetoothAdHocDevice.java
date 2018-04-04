package com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * <p>This class represents a remote Bluetooth device and is really just a thin wrapper for a
 * BluetoothDevice</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class BluetoothAdHocDevice implements Parcelable {

    private final String longUuidString;
    private final int rssi;
    private final BluetoothDevice device;

    /**
     * Constructor
     *
     * @param device a BluetoothDevice object which represents a remote Bluetooth device.
     */
    BluetoothAdHocDevice(BluetoothDevice device) {
        this.longUuidString = BluetoothUtil.UUID + device.getAddress().replace(":", "").toLowerCase();
        this.rssi = -1;
        this.device = device;
    }

    /**
     * Constructor
     *
     * @param device a BluetoothDevice object which represents a remote Bluetooth device.
     * @param rssi   an integer value which represents the rssi of the remote Bluetooth device.
     */
    BluetoothAdHocDevice(BluetoothDevice device, int rssi) {
        this.longUuidString = BluetoothUtil.UUID + device.getAddress().replace(":", "").toLowerCase();
        this.rssi = rssi;
        this.device = device;
    }

    /**
     * Private Constructor
     *
     * @param in a Parcel object which represents a container for a message (data and object
     *           references) that can be sent through an IBinder.
     */
    private BluetoothAdHocDevice(Parcel in) {
        this.longUuidString = in.readString();
        this.rssi = in.readInt();
        this.device = in.readParcelable(BluetoothDevice.class.getClassLoader());
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable
     * instance's marshaled representation. For example, if the object will
     * include a file descriptor in the output of {@link #writeToParcel(Parcel, int)},
     * the return value of this method must include the
     * {@link #CONTENTS_FILE_DESCRIPTOR} bit.
     *
     * @return a bitmask indicating the set of special object types marshaled
     * by this Parcelable object instance.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(longUuidString);
        dest.writeInt(rssi);
        dest.writeParcelable(device, flags);
    }

    /**
     * Interface that must be implemented and provided as a public CREATOR
     * field that generates instances of your Parcelable class from a Parcel.
     */
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

    /**
     * Method allowing to get the UUID of the remote Bluetooth device.
     *
     * @return a String value which represents the UUID of the remote Bluetooth device.
     */
    public String getUuid() {
        return longUuidString;
    }

    /**
     * Method allowing to get the rssi of the remote Bluetooth device.
     *
     * @return an integer value which represents the rssi of the remote Bluetooth device.
     */
    public int getRssi() {
        return rssi;
    }

    /**
     * Method allowing to get the BluetoothDevice object.
     *
     * @return a BluetoothDevice object which represents the remote Bluetooth device.
     */
    public BluetoothDevice getDevice() {
        return device;
    }

    @Override
    public String toString() {
        return device.getAddress() + " - " + device.getName();
    }

}
