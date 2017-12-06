package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.net.wifi.p2p.WifiP2pDevice;

import java.net.InetAddress;
import java.util.HashMap;

/**
 * Created by gaulthiergain on 26/10/17.
 */

public interface DiscoveryListener {
    void onDiscoveryStarted();

    void onDiscoveryFailed(int reasonCode);

    void onDiscoveryCompleted(HashMap<String, WifiP2pDevice> peers);
}
