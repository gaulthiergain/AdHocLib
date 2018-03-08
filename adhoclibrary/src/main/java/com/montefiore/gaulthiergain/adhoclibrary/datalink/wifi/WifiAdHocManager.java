package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Build;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import static android.os.Looper.getMainLooper;

public class WifiAdHocManager {

    private boolean v;
    private Context context;
    private String TAG = "[AdHoc][WifiManager]";
    private WifiP2pManager wifiP2pManager;
    private Channel channel;
    private HashMap<String, WifiP2pDevice> hashMapWifiDevices;

    private boolean discoveryRegistered = false;
    private boolean connectionRegistered = false;
    private boolean nameRegistered = false;
    private boolean groupOwnerRegistered = false;

    private WiFiDirectBroadcastDiscovery wiFiDirectBroadcastDiscovery;
    private WiFiDirectBroadcastConnection wifiDirectBroadcastConnection;
    private WifiDirectBroadcastGroupOwner wifiDirectBroadcastGroupOwner;
    private WifiDirectBroadcastName wifiDirectBroadcastName;


    /**
     * Constructor
     *
     * @param verbose a boolean value to set the debug/verbose mode.
     * @param context a Context object which gives global information about an application
     *                environment.
     */
    public WifiAdHocManager(boolean verbose, final Context context) throws DeviceException {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null && wifi.isP2pSupported()) {
                init(verbose, context);
            } else {
                // Device does not support Wifi Direct
                throw new DeviceException("Error device does not support Wifi Direct");
            }
        } else {
            init(verbose, context);
        }
    }

    private void init(boolean verbose, Context context) throws DeviceException {
        this.wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (wifiP2pManager == null) {
            // Device does not support Wifi Direct
            throw new DeviceException("Error device does not support Wifi Direct");
        } else {
            // Device supports Wifi Direct
            this.channel = wifiP2pManager.initialize(context, getMainLooper(), null);
            this.v = verbose;
            this.context = context;
            this.hashMapWifiDevices = new HashMap<>();
        }
    }

    public void getDeviceName(final ListenerWifiManager listenerWifiManager) {
        final IntentFilter intentFilter = new IntentFilter();

        //  Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        //  Update name
        intentFilter.addAction(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

        wifiDirectBroadcastName = new WifiDirectBroadcastName(new ListenerWifiManager() {
            @Override
            public void setDeviceName(String name) {
                listenerWifiManager.setDeviceName(name);
            }
        });
        nameRegistered = true;
        context.registerReceiver(wifiDirectBroadcastName, intentFilter);
    }

    /**
     * Method allowing to discover other wifi Direct hashMapWifiDevices.
     *
     * @param discoveryListener a discoveryListener object which serves as callback functions.
     */
    public void discover(final DiscoveryListener discoveryListener) {

        final IntentFilter intentFilter = new IntentFilter();

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        //  Indicates a change in the list of available hashMapWifiDevices.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {

                if (v) Log.d(TAG, "onPeersAvailable()");

                List<WifiP2pDevice> refreshedPeers = new ArrayList<>();
                refreshedPeers.addAll(peerList.getDeviceList());

                for (WifiP2pDevice wifiP2pDevice : refreshedPeers) {
                    if (!hashMapWifiDevices.containsKey(wifiP2pDevice.deviceAddress)) {
                        hashMapWifiDevices.put(wifiP2pDevice.deviceAddress, wifiP2pDevice);
                        if (v) Log.d(TAG, "Size: " + hashMapWifiDevices.size());
                        if (v)
                            Log.d(TAG, "Devices found: " +
                                    hashMapWifiDevices.get(wifiP2pDevice.deviceAddress).deviceName);
                        discoveryListener.onDiscoveryCompleted(hashMapWifiDevices);
                    } else {
                        if (v) Log.d(TAG, "Device already present");
                    }
                }

                if (hashMapWifiDevices.size() == 0) {
                    if (v) Log.d(TAG, "No devices found");
                }
            }
        };

        wiFiDirectBroadcastDiscovery = new WiFiDirectBroadcastDiscovery(v, wifiP2pManager, channel,
                peerListListener);
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
            public void onConnectionInfoAvailable(final WifiP2pInfo info) {
                if (v) {
                    Log.d(TAG, "onConnectionInfoAvailable");
                    Log.d(TAG, "Addr groupOwner:" + String.valueOf(info.groupOwnerAddress.getHostAddress()));
                }

                if (info.isGroupOwner) {
                    connectionListener.onGroupOwner(info.groupOwnerAddress);
                } else {
                    try {
                        connectionListener.onClient(info.groupOwnerAddress,
                                InetAddress.getByName(getDottedDecimalIP(getLocalIPAddress())));
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        wifiDirectBroadcastConnection = new WiFiDirectBroadcastConnection(wifiP2pManager, channel,
                onConnectionInfoAvailable, v);
        connectionRegistered = true;
        context.registerReceiver(wifiDirectBroadcastConnection, intentFilter);

        // Get The device from its address
        final WifiP2pDevice device = hashMapWifiDevices.get(address);
        final WifiP2pConfig config = new WifiP2pConfig();
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

        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifi != null && wifi.isWifiEnabled();
    }

    public void disable() {
        wifiAdapterState(false);
    }

    /**
     * Method allowing to enable the wifi adapter.
     */
    public void enable() {
        wifiAdapterState(true);
    }

    private void wifiAdapterState(boolean state) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            wifi.setWifiEnabled(state);
        }
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

    public void unregisterInitName() {
        if (v) Log.d(TAG, "unregisterName()");
        if (nameRegistered) {
            context.unregisterReceiver(wifiDirectBroadcastName);
            nameRegistered = false;
        }
    }

    public void unregisterGroupOwner() {
        if (v) Log.d(TAG, "unregisterGroupOwner()");
        if (groupOwnerRegistered) {
            context.unregisterReceiver(wifiDirectBroadcastGroupOwner);
            groupOwnerRegistered = false;
        }
    }

    public String getOwnMACAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface ntwInterface : interfaces) {

                if (ntwInterface.getName().equalsIgnoreCase("p2p0")) {
                    byte[] byteMac = ntwInterface.getHardwareAddress();
                    if (byteMac == null) {
                        return null;
                    }
                    StringBuilder strBuilder = new StringBuilder();
                    for (byte aByteMac : byteMac) {
                        strBuilder.append(String.format("%02X:", aByteMac));
                    }

                    if (strBuilder.length() > 0) {
                        strBuilder.deleteCharAt(strBuilder.length() - 1);
                    }

                    return strBuilder.toString();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return null;
    }

    private byte[] getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.toString().contains("192.168.49")) {
                        if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
                            return inetAddress.getAddress();
                        }
                        //return inetAddress.getHostAddress().toString(); // Galaxy Nexus returns IPv6
                    }
                }
            }
        } catch (SocketException ex) {
            //Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        } catch (NullPointerException ex) {
            //Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        }
        return null;
    }

    private String getDottedDecimalIP(byte[] ipAddr) {
        //convert to dotted decimal notation:
        StringBuilder ipAddrStr = new StringBuilder();
        for (int i = 0; i < ipAddr.length; i++) {
            if (i > 0) {
                ipAddrStr.append(".");
            }
            ipAddrStr.append(ipAddr[i] & 0xFF);
        }
        return ipAddrStr.toString();
    }

    public void updateName(String name) {
        try {
            Method method = wifiP2pManager.getClass().getMethod("setDeviceName",
                    WifiP2pManager.Channel.class, String.class, WifiP2pManager.ActionListener.class);

            method.invoke(wifiP2pManager, channel, "New Device Name", new WifiP2pManager.ActionListener() {
                public void onSuccess() {
                }

                public void onFailure(int reason) {
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestGO(final ListenerWifiGroupOwner listenerWifiGroupOwner) {
        if (listenerWifiGroupOwner != null) {
            final IntentFilter intentFilter = new IntentFilter();

            //  Indicates this device's details have changed.
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

            wifiDirectBroadcastGroupOwner = new WifiDirectBroadcastGroupOwner(wifiP2pManager, channel,
                    new ListenerWifiGroupOwner() {
                        @Override
                        public void getGroupOwner(String address) {
                            listenerWifiGroupOwner.getGroupOwner(address);
                        }
                    });
            groupOwnerRegistered = true;
            context.registerReceiver(wifiDirectBroadcastGroupOwner, intentFilter);
        }
    }

    public interface ListenerWifiManager {
        void setDeviceName(String name);
    }

    public interface ListenerWifiGroupOwner {
        void getGroupOwner(String address);
    }
}
