package com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions;

public class DeviceAlreadyConnectedException extends Exception {
    /**
     * Constructor
     *
     * @param message a String values which represents the cause of the exception
     */
    public DeviceAlreadyConnectedException(String message) {
        super(message);
    }
}
