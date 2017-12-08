package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.os.Looper.getMainLooper;

public class WifiManager {

    private final boolean v;
    private final Context context;
    private final String TAG = "[AdHoc][WifiManager]";
    private final WifiP2pManager wifiP2pManager;
    private final Channel channel;
    private final HashMap<String, WifiP2pDevice> peers;

    private boolean discoveryRegistered = false;
    private boolean connectionRegistered = false;
    private WiFiDirectBroadcastDiscovery wiFiDirectBroadcastDiscovery;
    private WiFiDirectBroadcastConnection wifiDirectBroadcastConnection;

    /**
     * Constructor
     *
     * @param verbose a boolean value to set the debug/verbose mode.
     * @param context a Context object which gives global information about an application
     *                environment.
     */
    public WifiManager(boolean verbose, final Context context) {
        this.v = verbose;
        this.context = context;
        this.wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = wifiP2pManager != null ? wifiP2pManager.initialize(context, getMainLooper(), null) : null;
        this.peers = new HashMap<>();
    }

    /**
     * Method allowing to discover other wifi Direct peers.
     *
     * @param discoveryListener a discoveryListener object which serves as callback functions.
     */
    public void discover(final DiscoveryListener discoveryListener) {

        final IntentFilter intentFilter = new IntentFilter();

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {

                if (v) Log.d(TAG, "onPeersAvailable()");

                List<WifiP2pDevice> refreshedPeers = new ArrayList<>();
                refreshedPeers.addAll(peerList.getDeviceList());

                for (WifiP2pDevice wifiP2pDevice : refreshedPeers) {
                    if (!peers.containsKey(wifiP2pDevice.deviceAddress)) {
                        peers.put(wifiP2pDevice.deviceAddress, wifiP2pDevice);
                        if (v) Log.d(TAG, "Size: " + peers.size());
                        if (v)
                            Log.d(TAG, "Devices found: " +
                                    peers.get(wifiP2pDevice.deviceAddress).deviceName);
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

        wiFiDirectBroadcastDiscovery = new WiFiDirectBroadcastDiscovery(wifiP2pManager, channel,
                peerListListener, v);
        discoveryRegistered = true;
        context.registerReceiver(wiFiDirectBroadcastDiscovery, intentFilter);

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

    /**
     * Method allowing to connect to a remote wifi Direct peer.
     *
     * @param address            a String value which represents the address of the remote wifi
     *                           Direct peer.
     * @param connectionListener a connectionListener object which serves as callback functions.
     */
    public void connect(String address, final ConnectionListener connectionListener) {

        final IntentFilter intentFilter = new IntentFilter();

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                if (v) {
                    Log.d(TAG, "onConnectionInfoAvailable");
                    Log.d(TAG, "Addr groupOwner:" + String.valueOf(info.groupOwnerAddress.getHostAddress()));
                }

                if (info.isGroupOwner) {
                    connectionListener.onGroupOwner(info.groupOwnerAddress);
                } else {
                    connectionListener.onClient(info.groupOwnerAddress);
                }
            }
        };

        wifiDirectBroadcastConnection = new WiFiDirectBroadcastConnection(wifiP2pManager, channel,
                onConnectionInfoAvailable, v);
        connectionRegistered = true;
        context.registerReceiver(wifiDirectBroadcastConnection, intentFilter);

        // Get The device from its address
        WifiP2pDevice device = peers.get(address);
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

    /**
     * Method allowing to enable the wifi Direct adapter.
     *
     * @return a boolean value which represents the state of the wifi Direct.
     */
    public boolean isEnabled() {
        return (wifiP2pManager != null && channel != null);
    }

    /**
     * Method allowing to enable the wifi Direct adapter.
     */
    public void enable() {
        Intent discoverableIntent =
                new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        context.startActivity(discoverableIntent);
    }

    /**
     * Method allowing to unregister the connection broadcast.
     */
    public void unregisterConnection() {
        if (v) Log.d(TAG, "unregisterConnection()");
        if (connectionRegistered) {
            context.unregisterReceiver(wifiDirectBroadcastConnection);
            connectionRegistered = false;
        }
    }

    /**
     * Method allowing to unregister the discovery broadcast.
     */
    public void unregisterDiscovery() {
        if (v) Log.d(TAG, "unregisterDiscovery()");
        if (discoveryRegistered) {
            context.unregisterReceiver(wiFiDirectBroadcastDiscovery);
            discoveryRegistered = false;
        }
    }
}
