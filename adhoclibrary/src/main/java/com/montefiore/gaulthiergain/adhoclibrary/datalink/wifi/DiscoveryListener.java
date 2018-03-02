package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.net.wifi.p2p.WifiP2pDevice;

import java.util.HashMap;

/**
 * <p>This interface allows to define callback functions for the wifi discovery process.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface DiscoveryListener {
    /**
     * Callback when the discovery is started.
     */
    void onDiscoveryStarted();

    /**
     * Callback when the discovery fails.
     *
     * @param reasonCode an integer value which represents the status of the discovery.
     */
    void onDiscoveryFailed(int reasonCode);

    /**
     * Callback when the discovery is finished.
     *
     * @param peers a HashMap<String, WifiP2pDevice> which contains a
     *              mapping between the name of a device and a
     *              {@link WifiP2pDevice} object.
     */
    void onDiscoveryCompleted(HashMap<String, WifiP2pDevice> peers);
}
