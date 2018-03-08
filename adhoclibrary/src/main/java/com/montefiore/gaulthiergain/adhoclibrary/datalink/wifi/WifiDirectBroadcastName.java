package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

public class WifiDirectBroadcastName extends BroadcastReceiver {

    private WifiAdHocManager.ListenerWifiManager listenerWifiManager;

    public WifiDirectBroadcastName(WifiAdHocManager.ListenerWifiManager listenerWifiManager) {
        this.listenerWifiManager = listenerWifiManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            listenerWifiManager.getDeviceName(device.deviceName);
        }
    }
}
