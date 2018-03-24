package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;

import java.net.InetAddress;

class WifiDirectBroadcastGroupOwner extends BroadcastReceiver {

    private WifiP2pManager manager;
    private Channel channel;
    private WifiAdHocManager.ListenerWifiGroupOwner listenerWifiGroupOwner;

    public WifiDirectBroadcastGroupOwner(WifiP2pManager manager, Channel channel,
                                         WifiAdHocManager.ListenerWifiGroupOwner listenerWifiGroupOwner) {
        this.manager = manager;
        this.channel = channel;
        this.listenerWifiGroupOwner = listenerWifiGroupOwner;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {

                    @Override
                    public void onConnectionInfoAvailable(WifiP2pInfo info) {

                        InetAddress groupOwnerAddress = info.groupOwnerAddress;
                        listenerWifiGroupOwner.getGroupOwner(groupOwnerAddress.getHostAddress());
                    }
                });
            }
        }
    }
}
