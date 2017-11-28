package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.wifiListener.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.wifiListener.DiscoveryListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.os.Looper.getMainLooper;


public class WifiManager {


    private final boolean v;
    private final Context context;
    private final String TAG = "[AdHoc][" + getClass().getName() + "]";
    private boolean registered = false;

    private final DiscoveryListener discoveryListener;
    private final ConnectionListener connectionListener;
    private WiFiDirectBroadcastReceiver wiFiDirectBroadcastReceiver;

    private WifiP2pManager wifiP2pManager;
    private Channel channel;

    private HashMap<String, WifiP2pDevice> peers = new HashMap<String, WifiP2pDevice>();

    public WifiManager(final Context context, boolean verbose,
                       final DiscoveryListener discoveryListener,
                       final ConnectionListener connectionListener) {
        this.v = verbose;
        this.context = context;
        this.discoveryListener = discoveryListener;
        this.connectionListener = connectionListener;
        this.wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = wifiP2pManager.initialize(context, getMainLooper(), null);

        initWifi();
    }

    public void discover() {

        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                discoveryListener.onDiscoveryStarted();
            }

            @Override
            public void onFailure(int reasonCode) {
                discoveryListener.onDiscoveryFailed(reasonCode);
            }
        });
    }


    //@Override
    public void connect(String addr) {

        // Get The device from its address
        WifiP2pDevice device = peers.get(addr);
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                connectionListener.onConnectionStarted();
            }

            @Override
            public void onFailure(int reasonCode) {
                connectionListener.onConnectionFailed(reasonCode);
            }
        });
    }

    public void unregister() {
        if (v) Log.d(TAG, "unregisterDiscovery()");
        if (registered) {
            context.unregisterReceiver(wiFiDirectBroadcastReceiver);
            registered = false;
        }
    }


    private void initWifi() {

        final IntentFilter intentFilter = new IntentFilter();

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {

                if (v) Log.d(TAG, "onPeersAvailable");

                List<WifiP2pDevice> refreshedPeers = new ArrayList<>();
                refreshedPeers.addAll(peerList.getDeviceList());

                for (WifiP2pDevice wifiP2pDevice : refreshedPeers) {
                    if (!peers.containsKey(wifiP2pDevice.deviceAddress)) {
                        peers.put(wifiP2pDevice.deviceAddress, wifiP2pDevice);
                        if (v) Log.d(TAG, "Size: " + peers.size());
                        if (v)
                            Log.d(TAG, "Devices found: " + peers.get(wifiP2pDevice.deviceAddress).deviceName);
                        discoveryListener.onDiscoveryCompleted(peers);
                    } else {
                        if (v) Log.d(TAG, "Device already present");
                    }
                }


                if (peers.size() == 0) {
                    if (v) Log.d(TAG, "No devices found");
                }
            }
        };

        WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                if (v) Log.d(TAG, "onConnectionInfoAvailable");
                if (v) Log.d(TAG, "isgroupFormed: " + info.groupFormed);
                if (v) Log.d(TAG, "isGroupOwner: " + info.isGroupOwner);
                if (v)
                    Log.d(TAG, "Addr groupOwner:" + String.valueOf(info.groupOwnerAddress.getHostAddress()));

                if (info.isGroupOwner) {
                    /*new ServerTask(context, new OnReceiveListener() {
                        @Override
                        public void OnReceive(Context context, String str) {
                            Message message = handler.obtainMessage(1, str);
                            message.sendToTarget();
                        }
                    }).execute();*/
                    connectionListener.onGroupOwner(info.groupOwnerAddress);
                } else {
                    /*
                    //new ClientTask(context, info.groupOwnerAddress.getHostAddress()).execute();
                    Message message = handler.obtainMessage(2, info.groupOwnerAddress.getHostAddress());
                    message.sendToTarget();*/
                    //listener
                    connectionListener.onClient(info.groupOwnerAddress);
                }

            }
        };

        wiFiDirectBroadcastReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, channel,
                peerListListener, onConnectionInfoAvailable, v);
        registered = true;
        context.registerReceiver(wiFiDirectBroadcastReceiver, intentFilter);
    }

}
