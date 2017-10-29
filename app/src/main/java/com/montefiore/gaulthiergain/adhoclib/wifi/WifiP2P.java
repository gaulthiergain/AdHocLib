package com.montefiore.gaulthiergain.adhoclib.wifi;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.os.Looper.getMainLooper;

/**
 * Created by gaulthiergain on 29/10/17.
 *
 */

public class WifiP2P {

    private static final String TAG = "[AdHoc]";
    private Context context;
    private WifiP2pManager mManager;
    private Channel mChannel;
    private WifiP2pManager.PeerListListener peerListListener;
    private WiFiDirectBroadcastReceiver wiFiDirectBroadcastReceiver;
    private final IntentFilter intentFilter = new IntentFilter();

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    public WifiP2P(Context context) {
        this.context = context;
        mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(context, getMainLooper(), null);

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {

                Log.d(TAG, "PEERListListener");

                List<WifiP2pDevice> refreshedPeers = new ArrayList<>();
                refreshedPeers.addAll(peerList.getDeviceList());

                if (!refreshedPeers.equals(peers)) {
                    peers.clear();
                    peers.addAll(refreshedPeers);
                }

                if (peers.size() == 0) {
                    Log.d(TAG, "No devices found");
                }else {
                    Log.d(TAG, "Devices found" + peers.size());
                    Log.d(TAG, "SIZE" + peers.get(0).deviceName);
                }
            }
        };

        wiFiDirectBroadcastReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, peerListListener);
        context.registerReceiver(wiFiDirectBroadcastReceiver, intentFilter);
    }

    public void discover(){


        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this method
                // can often be left blank.  Code for peer discovery goes in the
                // onReceive method, detailed below.
                Toast.makeText(context, "Discovery Initiated",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
                Toast.makeText(context, "Discovery Failed : " + reasonCode,
                        Toast.LENGTH_SHORT).show();
            }
        });



    }



}
