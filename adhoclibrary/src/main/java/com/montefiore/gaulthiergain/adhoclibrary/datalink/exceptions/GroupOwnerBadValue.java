package com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions;

/**
 * <p>This class signals that a Group Owner Bad value exception has occurred.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class GroupOwnerBadValue extends Exception {

    /**
     * Constructor
     *
     * @param message a String values which represents the cause of the exception
     */
    public GroupOwnerBadValue(String message) {
        super(message);
    }
}
