package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by gaulthiergain on 10/03/18.
 */

public interface ListenerApp extends ListenerAutoApp {

    void onDeviceDiscovered(AdHocDevice device);

    /**
     * Callback when the discovery started.
     */
    void onDiscoveryStarted();

    void onDiscoveryFailed(Exception exception);

    /**
     * Callback when the discovery is completed.
     *
     * @param mapAddressDevice
     */
    void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice);
}
