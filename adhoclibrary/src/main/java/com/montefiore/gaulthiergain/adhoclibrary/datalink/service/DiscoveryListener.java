package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import java.util.HashMap;

/**
 * <p>This interface allows to define callback functions for the bluetooth discovery process.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface DiscoveryListener {

    void onDeviceDiscovered(AdHocDevice device);

    /**
     * Method allowing to notify when the discovery is finished.
     *
     * @param mapNameDevice a HashMap<String, AdHocDevice> which contains a mapping between the name
     *                      of a device and a {@link AdHocDevice} object.
     */
    void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapNameDevice);

    /**
     * Method allowing to notify when the discovery started.
     */
    void onDiscoveryStarted();

    /**
     * Method allowing to notify when the discovery fails.
     *
     * @param exception an Exception object which contains the exception which has been caught.
     */
    void onDiscoveryFailed(Exception exception);
}
