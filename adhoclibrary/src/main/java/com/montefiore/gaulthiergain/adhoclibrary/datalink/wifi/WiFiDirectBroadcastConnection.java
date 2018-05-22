package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

/**
 * <p>This class defines a BroadcastReceiver that notifies of important wifi p2p events.</p>
 */
public class WiFiDirectBroadcastConnection extends BroadcastReceiver {

    private final String TAG = "[AdHoc][BroadcastConn]";

    private final boolean v;
    private WifiP2pManager manager;
    private Channel channel;
    private WifiAdHocManager.WifiDeviceInfosListener listenerWifiDeviceInfo;
    private WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable;
    private WifiAdHocManager.ListenerPeer peerListListener;

    /**
     * @param verbose                   a boolean value to set the debug/verbose mode.
     * @param manager                   a WifiP2pManager object which represents a system service.
     * @param channel                   a Wifi p2p Channel which represents the channel that connects
     *                                  the application to the Wifi p2p framework.
     * @param listenerWifiDeviceInfo    an interface for callback invocation when device information
     *                                  is available.
     * @param peerListListener          an interface for callback invocation when peer list is
     *                                  available.
     * @param onConnectionInfoAvailable an interface for callback invocation when connection
     *                                  information is available.
     */
    public WiFiDirectBroadcastConnection(boolean verbose, WifiP2pManager manager, Channel channel,
                                         WifiAdHocManager.WifiDeviceInfosListener listenerWifiDeviceInfo,
                                         WifiAdHocManager.ListenerPeer peerListListener,
                                         WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable) {
        super();
        this.v = verbose;
        this.manager = manager;
        this.channel = channel;
        this.listenerWifiDeviceInfo = listenerWifiDeviceInfo;
        this.peerListListener = peerListListener;
        this.onConnectionInfoAvailable = onConnectionInfoAvailable;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            if (listenerWifiDeviceInfo != null) {
                listenerWifiDeviceInfo.getDeviceInfos(device.deviceName, device.deviceAddress.toUpperCase());
            }
        } else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            // State of Wifi P2P has change
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                if (v) Log.d(TAG, "P2P state enabled: " + state);
                if (peerListListener != null) {
                    peerListListener.discoverPeers();
                }
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
                    if (v)
                        Log.d(TAG, "P2P WIFI_P2P_CONNECTION_CHANGED_ACTION networkInfo.isConnected()");

                    manager.requestConnectionInfo(channel, onConnectionInfoAvailable);

                } else {
                    if (v) Log.d(TAG, "P2P WIFI_P2P_CONNECTION_CHANGED_ACTION disconnect()");
                }
            }
        }
    }
}
