package com.montefiore.gaulthiergain.adhoclibrary.appframework.exceptions;

/**
 * <p>This class signals that the number of thread is incorrect.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class MaxThreadReachedException extends Exception {

    /**
     * Constructor
     *
     * @param message a String values which represents the cause of the exception
     */
    public MaxThreadReachedException(String message) {
        super(message);
    }
}
