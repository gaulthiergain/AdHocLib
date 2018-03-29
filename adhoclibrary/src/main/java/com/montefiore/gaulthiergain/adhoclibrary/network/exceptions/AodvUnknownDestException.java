package com.montefiore.gaulthiergain.adhoclibrary.network.exceptions;

/**
 * <p>This class signals that a AODV unknown destination route exception has occurred.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class AodvUnknownDestException extends AodvAbstractException {
    /**
     * Constructor
     *
     * @param message a String values which represents the cause of the exception
     */
    public AodvUnknownDestException(String message) {
        super(message);
    }
}