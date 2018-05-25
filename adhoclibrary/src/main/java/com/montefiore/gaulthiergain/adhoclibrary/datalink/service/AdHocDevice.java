package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * <p>This class represents a general ad hoc device. It can have Wi-Fi, Bluetooth or both enabled.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class AdHocDevice implements Parcelable, Serializable {

    protected String label;
    protected final String deviceName;
    protected final String macAddress;
    protected final int type;
    private boolean directedConnected;

    /**
     * Default constructor
     */
    public AdHocDevice() {
        this.deviceName = null;
        this.macAddress = null;
        this.type = 0;
    }

    /**
     * Constructor
     *
     * @param macAddress a String value which represents the MAC address of the device.
     * @param deviceName a String value which represents the name of the device.
     * @param type       an integer value which represents the type of the device.
     */
    public AdHocDevice(String macAddress, String deviceName, int type) {
        this.macAddress = macAddress;
        this.deviceName = checkName(deviceName);
        this.type = type;
        this.directedConnected = true;
    }

    /**
     * Constructor
     *
     * @param label      a String value which represents the label used to identify device.
     * @param macAddress a String value which represents the MAC address of the device.
     * @param deviceName a String value which represents the name of the device.
     * @param type       an integer value which represents the type of the device.
     */
    public AdHocDevice(String label, String macAddress, String deviceName, int type) {
        this.label = label;
        this.macAddress = macAddress;
        this.deviceName = checkName(deviceName);
        this.type = type;
        this.directedConnected = true;
    }

    /**
     * Constructor
     *
     * @param label             a String value which represents the label used to identify device.
     * @param macAddress        a String value which represents the MAC address of the device.
     * @param deviceName        a String value which represents the name of the device.
     * @param type              an integer value which represents the type of the device.
     * @param directedConnected a boolean value which represents if the device is directly connected.
     */
    public AdHocDevice(String label, String macAddress, String deviceName, int type,
                       boolean directedConnected) {
        this(label, macAddress, deviceName, type);
        this.directedConnected = directedConnected;
    }

    /**
     * Constructor
     *
     * @param label a String value which represents the label used to identify device.
     */
    public AdHocDevice(String label) {
        this();
        this.directedConnected = false;
        this.label = label;
    }

    /**
     * Private Constructor
     *
     * @param in a Parcel object which represents a container for a message (data and object
     *           references) that can be sent through an IBinder.
     */
    private AdHocDevice(Parcel in) {
        this(in.readString(), in.readString(), in.readString(), in.readInt(), in.readByte() != 0);
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
        dest.writeString(label);
        dest.writeString(macAddress);
        dest.writeString(deviceName);
        dest.writeInt(type);
        dest.writeByte((byte) (directedConnected ? 1 : 0));
    }

    /**
     * Interface that must be implemented and provided as a public CREATOR
     * field that generates instances of your Parcelable class from a Parcel.
     */
    protected static final Creator<AdHocDevice> CREATOR = new Creator<AdHocDevice>() {
        @Override
        public AdHocDevice createFromParcel(Parcel in) {
            return new AdHocDevice(in);
        }

        @Override
        public AdHocDevice[] newArray(int size) {
            return new AdHocDevice[size];
        }
    };

    /**
     * Method allowing to check if the name of the ad hoc device is correct.
     *
     * @param deviceName a String value which represents the name of the device.
     * @return a String value which represents the name of the device and is verified.
     */
    private String checkName(String deviceName) {
        if (deviceName == null) {
            return "";
        }

        return deviceName;
    }

    /**
     * Method allowing to set the label used to identify device.
     *
     * @param label a String value which represents the label used to identify device.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Method allowing to set the flag directedConnected.
     *
     * @param directedConnected a boolean value which represents if the device is directly connected.
     */
    public void setDirectedConnected(boolean directedConnected) {
        this.directedConnected = directedConnected;
    }

    /**
     * Method allowing to get the label used to identify device.
     *
     * @return a String value which represents the label used to identify device.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Method allowing to get the MAC address of the device.
     *
     * @return a String value which represents the MAC address of the device.
     */
    public String getMacAddress() {
        return macAddress;
    }

    /**
     * Method allowing to get the name of the device.
     *
     * @return a String value which represents the name of the device.
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Method allowing to get the type of the device.
     *
     * @return an integer value which represents the type of the device.
     */
    public int getType() {
        return type;
    }

    /**
     * Method allowing to print in a friendly way the type of the device.
     *
     * @return a String value which returns the type of the device (Bluetooth/Wi-Fi)
     */
    @JsonIgnore
    public String getStringType() {
        return display(type);
    }

    /**
     * Method allowing to know if the device is directly connected.
     *
     * @return a boolean value which represents if the device is directly connected.
     */
    public boolean isDirectedConnected() {
        return directedConnected;
    }

    /**
     * Method allowing to format in a friendly way the type of the device.
     *
     * @param type an integer value which represents the type of the device.
     * @return a String value which returns the type of the device (Bluetooth/Wi-Fi)
     */
    protected String display(int type) {
        switch (type) {
            case Service.BLUETOOTH:
                return "Bluetooth";
            case Service.WIFI:
                return "Wifi";
            default:
                return "UNKNOWN";
        }
    }

    @Override
    public String toString() {
        return "AdHocDevice{" +
                "label='" + label + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", type=" + display(type) +
                ", directedConnected=" + directedConnected +
                '}';
    }
}
