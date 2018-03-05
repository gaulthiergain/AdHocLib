package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import java.net.InetAddress;

/**
 * <p>This interface allows to define callback functions for the wifi connection process.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface ConnectionListener {
    /**
     * Callback when the connection is started.
     */
    void onConnectionStarted(boolean isGroupOwner);

    /**
     * Callback when the connection fails.
     *
     * @param reasonCode an integer value which represents the status of the discovery.
     */
    void onConnectionFailed(int reasonCode);

    /**
     * Callback when the device is the groupOwner.
     *
     * @param groupOwnerAddress an InetAddress object which represents the current address'
     *                          group owner.
     */
    void onGroupOwner(InetAddress groupOwnerAddress);

    /**
     * Callback when the device is a client.
     *
     * @param groupOwnerAddress an InetAddress object which represents the current address'
     *                          group owner.
     */
    void onClient(InetAddress groupOwnerAddress, InetAddress ownAddress);
}
