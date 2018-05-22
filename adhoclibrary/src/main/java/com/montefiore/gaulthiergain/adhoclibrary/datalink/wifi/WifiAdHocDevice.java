package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.os.Parcel;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;

/**
 * <p>This class represents a remote Wi-Fi-enabled device.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class WifiAdHocDevice extends AdHocDevice {

    private String ipAddress;
    private int port;

    /**
     * Constructor
     *
     * @param deviceAddress a String value which represents the IP address of the device.
     * @param deviceName    a String value which represents the name of the device.
     */
    WifiAdHocDevice(String deviceAddress, String deviceName) {
        super(deviceAddress, deviceName, Service.WIFI);
        this.ipAddress = "";
        this.port = 0;
    }

    /**
     * Constructor
     *
     * @param label      a String value which represents the label used to identify device.
     * @param macAddress a String value which represents the MAC address of the device.
     * @param deviceName a String value which represents the name of the device.
     * @param type       an integer value which represents the type of the device.
     * @param ipAddress  a String value which represents the IP address of the device.
     */
    public WifiAdHocDevice(String label, String macAddress, String deviceName, int type,
                           String ipAddress) {
        super(label, macAddress, deviceName, type);
        this.ipAddress = ipAddress;
        this.port = 0;
    }

    /**
     * Private Constructor
     *
     * @param in a Parcel object which represents a container for a message (data and object
     *           references) that can be sent through an IBinder.
     */
    private WifiAdHocDevice(Parcel in) {
        super(in.readString(), in.readString(), in.readString(), in.readInt(), in.readByte() != 0);
        this.ipAddress = in.readString();
        this.port = in.readInt();
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
        dest.writeString(ipAddress);
        dest.writeInt(port);
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

    /**
     * Method allowing to set the device IP address.
     *
     * @param ipAddress a String value which represents the device IP address.
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }


    /**
     * Method allowing to set the listening prot.
     *
     * @param port an integer value to set the listening port number.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Method allowing to get the device IP address.
     *
     * @return a String value which represents the device IP address.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Method allowing to get the listening port.
     *
     * @return an integer value to set the listening port number.
     */
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "WifiAdHocDevice{" +
                "ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", label='" + label + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", macAddress='" + macAddress + '\'' +
                ", type=" + type +
                '}';
    }
}
