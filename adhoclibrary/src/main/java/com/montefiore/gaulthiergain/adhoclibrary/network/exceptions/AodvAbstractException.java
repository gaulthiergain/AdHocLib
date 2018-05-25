package com.montefiore.gaulthiergain.adhoclibrary.network.exceptions;

/**
 * <p>This class signals that an AODV exception has occurred.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public abstract class AodvAbstractException extends Exception {

    /**
     * Constructor
     *
     * @param message a String values which represents the cause of the exception
     */
    AodvAbstractException(String message) {
        super(message);
    }
}
