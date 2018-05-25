package com.montefiore.gaulthiergain.adhoclibrary.network.exceptions;

/**
 * <p>This class signals that a AODV Message exception has occurred during processing message.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class AodvMessageException extends AodvAbstractException {

    /**
     * Constructor
     *
     * @param message a String values which represents the cause of the exception
     */
    public AodvMessageException(String message) {
        super(message);
    }
}
