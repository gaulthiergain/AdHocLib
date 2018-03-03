package com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions;

/**
 * <p>This class signals that a Bluetooth Disabled exception has occurred.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

public class BluetoothDisabledException extends Exception{
    /**
     * Constructor
     *
     * @param message a String values which represents the cause of the exception
     */
    public BluetoothDisabledException(String message) {
        super(message);
    }
}
