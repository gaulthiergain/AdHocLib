package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

public class WiFiDirectBroadcastConnection extends BroadcastReceiver {

    private final boolean v;
    private final String TAG = "[AdHoc][" + getClass().getName() + "]";
    private WifiP2pManager manager;
    private Channel channel;
    private WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable;

    public WiFiDirectBroadcastConnection(WifiP2pManager manager, Channel channel,
                                         WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable, boolean verbose) {
        super();
        this.v = verbose;
        this.manager = manager;
        this.channel = channel;
        this.onConnectionInfoAvailable = onConnectionInfoAvailable;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // State of Wifi P2P has change
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                if (v) Log.d(TAG, "P2P state enabled: " + state);
            } else {
                if (v) Log.d(TAG, "P2P state disabled: " + state);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            // Connection has changed
            if (v) Log.d(TAG, "P2P WIFI_P2P_CONNECTION_CHANGED_ACTION");

            if (manager != null) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    // Connected with the other device, request connection info to find group owner
                    Log.d(TAG, "P2P WIFI_P2P_CONNECTION_CHANGED_ACTION networkInfo.isConnected()");
                    manager.requestConnectionInfo(channel, onConnectionInfoAvailable);
                } else {
                    Log.d(TAG, "P2P WIFI_P2P_CONNECTION_CHANGED_ACTION disconnect()");
                }
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            if (v) Log.d(TAG, "P2P WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
        }
    }
}
