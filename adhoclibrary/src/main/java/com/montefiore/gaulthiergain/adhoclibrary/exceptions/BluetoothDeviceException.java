package com.montefiore.gaulthiergain.adhoclibrary.exceptions;

/**
 * <p>This class signals that a Bluetooth Device Exception exception has occurred.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class BluetoothDeviceException extends Exception {
    /**
     * Constructor
     *
     * @param message a String values which represents the cause of the exception
     */
    public BluetoothDeviceException(String message) {
        super(message);
    }
}
