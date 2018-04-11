package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import java.io.IOException;
import java.net.InetAddress;

/**
 * <p>This interface allows to define callback functions for the wifi connection process.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface ConnectionWifiListener {
    /**
     * Callback when the connection is started.
     */
    void onConnectionStarted();

    /**
     * Callback when the connection fails.
     */
    void onConnectionFailed(Exception e);

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
     * @param ownAddress        an InetAddress object which represents the address of the device.
     */
    void onClient(InetAddress groupOwnerAddress, InetAddress ownAddress) throws IOException;
}
