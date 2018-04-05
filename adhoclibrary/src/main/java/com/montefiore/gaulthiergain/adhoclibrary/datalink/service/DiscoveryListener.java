package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import java.util.HashMap;

/**
 * <p>This interface allows to define callback functions for the bluetooth discovery process.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface DiscoveryListener {
    /**
     * Callback when the discovery is finished.
     *
     * @param mapNameDevice a HashMap<String, AdHocDevice> which contains a mapping between the name
     *                      of a device and a {@link AdHocDevice} object.
     */
    void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapNameDevice);

    /**
     * Callback when the discovery started.
     */
    void onDiscoveryStarted();

    /**
     * Callback when the discovery fails.
     *
     * @param reasonCode an integer value which represents the status of the discovery.
     */
    void onDiscoveryFailed(int reasonCode);
}
