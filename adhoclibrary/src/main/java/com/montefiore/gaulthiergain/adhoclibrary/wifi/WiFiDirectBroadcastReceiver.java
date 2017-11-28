package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;


public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private final boolean v;
    private final String TAG = "[AdHoc][" + getClass().getName() + "]";
    private WifiP2pManager manager;
    private Channel channel;
    private PeerListListener peerListListener;
    private WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel, PeerListListener peerListListener,
                                       WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable, boolean verbose) {
        super();
        this.v = verbose;
        this.manager = manager;
        this.channel = channel;
        this.peerListListener = peerListListener;
        this.onConnectionInfoAvailable = onConnectionInfoAvailable;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                if(v) Log.d(TAG, "P2P state enabled: " + state);
            } else {
                if(v) Log.d(TAG, "P2P state disabled: " + state);
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            if (manager != null) {
                manager.requestPeers(channel, peerListListener);
                Log.d(TAG, "P2P peers changed");
            }


        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            Log.d(TAG, "P2P WIFI_P2P_CONNECTION_CHANGED_ACTION");
            if (manager == null) {
                Log.d(TAG, "P2P WIFI_P2P_CONNECTION_CHANGED_ACTION NULL");
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                Log.d(TAG, "P2P WIFI_P2P_CONNECTION_CHANGED_ACTION networkInfo.isConnected()");
                // we are connected with the other device, request connection
                // info to find group owner IP

                manager.requestConnectionInfo(channel, onConnectionInfoAvailable);
            } else {
                Log.d(TAG, "P2P WIFI_P2P_CONNECTION_CHANGED_ACTION disconnect()");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

            Log.d(TAG, "P2P WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
        }
    }
}
