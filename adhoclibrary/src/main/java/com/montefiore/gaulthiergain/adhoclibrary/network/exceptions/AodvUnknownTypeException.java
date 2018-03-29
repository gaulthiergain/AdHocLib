package com.montefiore.gaulthiergain.adhoclibrary.network.exceptions;

/**
 * <p>This class signals that a AODV unknown type exception has occurred.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class AodvUnknownTypeException extends AodvAbstractException {
    /**
     * Constructor
     *
     * @param message a String values which represents the cause of the exception
     */
    public AodvUnknownTypeException(String message) {
        super(message);
    }
}
