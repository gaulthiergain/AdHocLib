package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

public class WiFiDirectBroadcastDiscovery extends BroadcastReceiver {

    private final boolean v;
    private final static String TAG = "[AdHoc][WifiDiscovery]";
    private WifiP2pManager manager;
    private Channel channel;
    private PeerListListener peerListListener;
    private WifiManager.ListenerWifiManager listenerWifiManager;

    public WiFiDirectBroadcastDiscovery(boolean verbose, WifiP2pManager manager, Channel channel,
                                        PeerListListener peerListListener,
                                        WifiManager.ListenerWifiManager listenerWifiManager) {
        super();
        this.v = verbose;
        this.manager = manager;
        this.channel = channel;
        this.peerListListener = peerListListener;
        this.listenerWifiManager = listenerWifiManager;
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

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            // Peers have changed
            if (manager != null) {
                // Update the list of peers
                manager.requestPeers(channel, peerListListener);
                if (v) Log.d(TAG, "P2P peers changed");
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            if (v) Log.d(TAG, "P2P WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            listenerWifiManager.setDeviceName(device.deviceName);
        }
    }
}
