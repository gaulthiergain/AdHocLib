package com.montefiore.gaulthiergain.adhoclib.wifi;

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
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
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
    private WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable;
    private Handler mHandler;

    private HashMap<String, WifiP2pDevice> peers = new HashMap<String, WifiP2pDevice>();

    public WifiP2P(final Context context, final Handler mHandler) {
        this.context = context;
        this.mHandler = mHandler;
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

                Log.d(TAG, "onPeersAvailable");

                List<WifiP2pDevice> refreshedPeers = new ArrayList<>();
                refreshedPeers.addAll(peerList.getDeviceList());

                for (WifiP2pDevice wifiP2pDevice : refreshedPeers){
                    if(!peers.containsKey(wifiP2pDevice.deviceAddress)){
                        peers.put(wifiP2pDevice.deviceAddress, wifiP2pDevice);
                        Log.d(TAG, "Size: " + peers.size());
                        Log.d(TAG, "Devices found: " + peers.get(wifiP2pDevice.deviceAddress).deviceName);
                        connect(wifiP2pDevice.deviceAddress);
                    }else{
                        Log.d(TAG, "Device already present");
                    }
                }


                if (peers.size() == 0) {
                    Log.d(TAG, "No devices found");
                }
            }
        };

        onConnectionInfoAvailable = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                Log.d(TAG, "onConnectionInfoAvailable");
                Log.d(TAG, "isgroupFormed: " + info.groupFormed);
                Log.d(TAG, "isGroupOwner: " + info.isGroupOwner);
                Log.d(TAG, "Addr groupOwner:" + String.valueOf(info.groupOwnerAddress.getHostAddress()));

                if(info.isGroupOwner){
                    new ServerTask(context, new OnReceiveListener() {
                        @Override
                        public void OnReceive(Context context, String str) {
                            Message message = mHandler.obtainMessage(1, str);
                            message.sendToTarget();
                        }
                    }).execute();
                }else{
                    new ClientTask(context, info.groupOwnerAddress.getHostAddress()).execute();
                }

            }
        };

        wiFiDirectBroadcastReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, peerListListener, onConnectionInfoAvailable);
        context.registerReceiver(wiFiDirectBroadcastReceiver, intentFilter);
    }

    public void discover(final OnDiscoveryCompleteListener onDiscoveryCompleteListener){



        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                onDiscoveryCompleteListener.OnDiscoveryComplete(peers);
                // Code for when the discovery initiation is successful goes here.
                // No services have actually been discovered yet, so this method
                // can often be left blank.  Code for peer discovery goes in the
                // onReceive method, detailed below.
                Log.d(TAG, "Discovery Initiated");
            }

            @Override
            public void onFailure(int reasonCode) {
                // Code for when the discovery initiation fails goes here.
                // Alert the user that something went wrong.
                Log.d(TAG,  "Discovery Failed : " + reasonCode);
            }
        });
    }


    //@Override
    public void connect(String addr) {
        // Picking the first device found on the network.
        WifiP2pDevice device = peers.get(addr);

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        Log.d(TAG,  "NAME: " + device.deviceName);
        Log.d(TAG,  "ADDR: " + device.deviceAddress);
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                Log.d(TAG,  "Connect onSuccess");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG,  "Connect failed. Retry." + reasonCode);
            }
        });
    }

    public void unregister(){
        context.unregisterReceiver(wiFiDirectBroadcastReceiver);
    }

}
