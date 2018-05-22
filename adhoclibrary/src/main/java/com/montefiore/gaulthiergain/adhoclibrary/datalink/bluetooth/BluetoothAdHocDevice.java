package com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.ParcelUuid;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;

import java.util.UUID;

/**
 * <p>This class represents a remote Bluetooth-enabled device.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class BluetoothAdHocDevice extends AdHocDevice {

    private final String uuidString;
    private final ParcelUuid uuid;
    private final int rssi;
    private final BluetoothDevice device;

    /**
     * Constructor
     *
     * @param device a BluetoothDevice object which represents a remote Bluetooth device.
     */
    BluetoothAdHocDevice(BluetoothDevice device) {
        super(device.getAddress().toUpperCase(), device.getName(), Service.BLUETOOTH);
        this.uuidString = BluetoothUtil.UUID + device.getAddress().replace(":", "").toLowerCase();
        this.uuid = ParcelUuid.fromString(uuidString);
        this.rssi = -1;
        this.device = device;
    }

    /**
     * Constructor
     *
     * @param device a BluetoothDevice object which represents a remote Bluetooth device.
     * @param rssi   an integer value which represents the RSSI of the remote Bluetooth device.
     */
    BluetoothAdHocDevice(BluetoothDevice device, int rssi) {
        super(device.getAddress().toUpperCase(), device.getName(), Service.BLUETOOTH);
        this.uuidString = BluetoothUtil.UUID + device.getAddress().replace(":", "").toLowerCase();
        this.uuid = ParcelUuid.fromString(uuidString);
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
        super(in.readString(), in.readString(), in.readString(), in.readInt(), in.readByte() != 0);
        this.uuid = in.readParcelable(ParcelUuid.class.getClassLoader());
        this.uuidString = in.readString();
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
        super.writeToParcel(dest, flags);
        dest.writeParcelable(uuid, flags);
        dest.writeString(uuidString);
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
     * Method allowing to get the UUID as a String of the remote Bluetooth device.
     *
     * @return a String value which represents the UUID of the remote Bluetooth device.
     */
    public String getUuid() {
        return uuidString;
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
    BluetoothDevice getDevice() {
        return device;
    }

    /**
     * Method allowing to get the UUID of the remote Bluetooth device.
     *
     * @return a UUID value which represents the UUID of the remote Bluetooth device.
     */
    UUID getUUID() {
        return uuid.getUuid();
    }

    @Override
    public String toString() {
        return "BluetoothAdHocDevice{" +
                "uuidString='" + uuidString + '\'' +
                ", uuid=" + uuid +
                ", rssi=" + rssi +
                ", label='" + label + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", type=" + type +
                '}';
    }
}
