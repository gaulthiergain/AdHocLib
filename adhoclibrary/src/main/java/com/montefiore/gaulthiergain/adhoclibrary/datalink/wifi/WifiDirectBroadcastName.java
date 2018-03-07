package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class WifiDirectBroadcastName extends BroadcastReceiver {

    private final boolean v;
    private final static String TAG = "[AdHoc][WifiName]";
    private WifiAdHocManager.ListenerWifiManager listenerWifiManager;

    public WifiDirectBroadcastName(boolean v, WifiAdHocManager.ListenerWifiManager listenerWifiManager) {
        this.v = v;
        this.listenerWifiManager = listenerWifiManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            if (v) Log.d(TAG, "P2P WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            listenerWifiManager.setDeviceName(device.deviceName);
        }
    }
}
