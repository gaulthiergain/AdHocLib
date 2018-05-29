package com.montefiore.gaulthiergain.adhoclibrary.appframework.exceptions;

/**
 * <p>This class signals that the listening port of the server is incorrect.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class BadServerPortException extends Exception {

    /**
     * Constructor
     *
     * @param message a String values which represents the cause of the exception
     */
    public BadServerPortException(String message) {
        super(message);
    }
}
