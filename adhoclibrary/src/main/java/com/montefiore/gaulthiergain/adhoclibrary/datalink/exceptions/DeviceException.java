package com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions;

/**
 * <p>This class signals that a Device Exception exception has occurred.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class DeviceException extends Exception {
    /**
     * Constructor
     *
     * @param message a String values which represents the cause of the exception
     */
    public DeviceException(String message) {
        super(message);
    }
}
