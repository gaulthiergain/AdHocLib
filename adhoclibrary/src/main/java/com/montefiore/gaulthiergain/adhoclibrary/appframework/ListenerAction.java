package com.montefiore.gaulthiergain.adhoclibrary.appframework;

/**
 * <p>This interface allows to define callback functions to notify about the status of Group deletion
 * and connection process (for Wi-Fi Direct only).</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface ListenerAction {

    /**
     * This method is called if the operation has succeeded.
     */
    void onSuccess();

    /**
     * This method is called if the operation has failed.
     *
     * @param e an Exception object which represents the cause of the exception.
     */
    void onFailure(Exception e);
}