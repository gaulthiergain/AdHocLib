package com.montefiore.gaulthiergain.adhoclib.wifi;

import android.net.wifi.p2p.WifiP2pDevice;

import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothAdHocDevice;

import java.util.HashMap;

/**
 * Created by gaulthiergain on 26/10/17.
 *
 */

public interface OnDiscoveryCompleteListener {
    void OnDiscoveryComplete(HashMap<String, WifiP2pDevice> peers);
}
