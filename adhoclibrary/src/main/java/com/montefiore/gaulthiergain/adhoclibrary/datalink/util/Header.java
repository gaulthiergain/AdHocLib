package com.montefiore.gaulthiergain.adhoclibrary.datalink.util;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.io.Serializable;

/**
 * <p>This class represents the format of the header of the messages exchanged by applications using
 * the library.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
@JsonTypeName("header")
public class Header implements Serializable {

    protected int type;
    protected String label;
    protected String name;

    private String address;
    private String mac;
    private int deviceType;

    /**
     * Default constructor
     */
    public Header() {

    }

    /**
     * Constructor
     *
     * @param type  an integer value which represents the type of the message.
     * @param label a String value which represents the label used to identify device.
     * @param name  a String value which represents the device name.
     */
    public Header(int type, String label, String name) {
        this.type = type;
        this.label = label;
        this.name = name;
    }

    /**
     * Constructor
     *
     * @param type  an integer value which represents the type of the message.
     * @param mac   a String value which represents the device MAC address.
     * @param label a String value which represents the label used to identify device.
     * @param name  a String value which represents the device name.
     */
    public Header(int type, String mac, String label, String name) {
        this.type = type;
        this.mac = mac;
        this.label = label;
        this.name = name;
    }

    /**
     * Constructor
     *
     * @param type    an integer value which represents the type of the message.
     * @param address a String value which represents the device logical address
     *                (UUID for Bluetooth and IP for Wi-Fi).
     * @param mac     a String value which represents the device MAC address.
     * @param label   a String value which represents the label used to identify device.
     * @param name    a String value which represents the device name.
     */
    public Header(int type, String address, String mac, String label, String name) {
        this.type = type;
        this.address = address;
        this.mac = mac;
        this.label = label;
        this.name = name;
    }

    /**
     * Constructor
     *
     * @param type       an integer value which represents the type of the message.
     * @param mac        a String value which represents the device MAC address.
     * @param label      a String value which represents the label used to identify device.
     * @param name       a String value which represents the device name.
     * @param deviceType an integer which represents the type of the device (Bluetooth or Wi-Fi)
     */
    public Header(int type, String mac, String label, String name, int deviceType) {
        this.type = type;
        this.mac = mac;
        this.label = label;
        this.name = name;
        this.deviceType = deviceType;
    }

    /**
     * Method allowing to set the device type.
     *
     * @param type an integer value which represents the type of the message.
     */
    public void setType(int type) {
        this.type = type;
    }


    /**
     * Method allowing to get the type of the message.
     *
     * @return a integer value which represents the type of the message.
     */
    public int getType() {
        return type;
    }

    /**
     * Method allowing to get the device type.
     *
     * @return an integer which represents the type of the device (Bluetooth or Wi-Fi)
     */
    public String getLabel() {
        return label;
    }

    /**
     * Method allowing to get the device name.
     *
     * @return a String value which represents the device name.
     */
    public String getName() {
        return name;
    }

    /**
     * Method allowing to get the device address.
     *
     * @return a String value which represents the device address.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Method allowing to get the device MAC address.
     *
     * @return a String value which represents the device MAC address.
     */
    public String getMac() {
        return mac;
    }

    /**
     * Method allowing to get the device type.
     *
     * @return an integer which represents the type of the device (Bluetooth or Wi-Fi)
     */
    public int getDeviceType() {
        return deviceType;
    }

    @Override
    public String toString() {
        return "Header{" +
                "type=" + type +
                ", label='" + label + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", mac='" + mac + '\'' +
                '}';
    }
}