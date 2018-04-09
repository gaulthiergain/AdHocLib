package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.WifiDiscoveryException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

import static android.net.wifi.p2p.WifiP2pManager.BUSY;
import static android.net.wifi.p2p.WifiP2pManager.ERROR;
import static android.net.wifi.p2p.WifiP2pManager.P2P_UNSUPPORTED;
import static android.os.Looper.getMainLooper;

public class WifiAdHocManager {

    public static String TAG = "[AdHoc][WifiManager]";

    private boolean v;
    private Context context;
    private Channel channel;
    private String initialName;
    private String currentAdapterName;
    private BroadcastWifi broadcastWifi;
    private WifiP2pManager wifiP2pManager;
    private ConnectionListener connectionListener;
    private HashMap<String, AdHocDevice> hashMapWifiDevices;

    private static final int DISCOVERY_TIME = 10000;
    private int valueGroupOwner = -1;

    /**
     * Constructor
     *
     * @param verbose            a boolean value to set the debug/verbose mode.
     * @param context            a Context object which gives global information about an application
     *                           environment.
     * @param connectionListener a connectionListener object which serves as callback functions.
     */
    public WifiAdHocManager(boolean verbose, final Context context,
                            final ConnectionListener connectionListener) throws DeviceException {

        this.wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (wifiP2pManager == null) {
            // Device does not support Wifi Direct
            throw new DeviceException("Error device does not support Wifi Direct");
        } else {
            // Device supports Wifi Direct
            this.v = verbose;
            this.channel = wifiP2pManager.initialize(context, getMainLooper(), null);
            this.context = context;
            this.broadcastWifi = new BroadcastWifi();
            this.hashMapWifiDevices = new HashMap<>();
            if (connectionListener != null) {
                this.connectionListener = connectionListener;
                this.registerConnection();
            }
        }
    }

    private void registerConnection() {
        final IntentFilter intentFilter = new IntentFilter();

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(final WifiP2pInfo info) {
                if (v) Log.d(TAG, "Address groupOwner:"
                        + String.valueOf(info.groupOwnerAddress.getHostAddress()));

                if (info.isGroupOwner) {
                    connectionListener.onGroupOwner(info.groupOwnerAddress);
                } else {
                    try {
                        byte[] addr = getLocalIPAddress();
                        if (addr != null) {
                            connectionListener.onClient(info.groupOwnerAddress,
                                    InetAddress.getByName(getDottedDecimalIP(addr)));
                        } else {
                            connectionListener.onConnectionFailed(
                                    new NoConnectionException("Unknown IP address"));
                        }
                    } catch (UnknownHostException | SocketException e) {
                        connectionListener.onConnectionFailed(e);
                    } catch (IOException e) {
                        connectionListener.onConnectionFailed(e);
                    }
                }
            }
        };

        broadcastWifi.registerConnection(intentFilter, onConnectionInfoAvailable);
    }

    public String getDeviceName() {
        return currentAdapterName;
    }

    public void getAdapterName(final ListenerWifiDeviceName listenerWifiDeviceName) {
        final IntentFilter intentFilter = new IntentFilter();

        //  Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        // Indicates this device's details are available
        intentFilter.addAction(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

        broadcastWifi.registerName(intentFilter, new ListenerWifiDeviceName() {

            @Override
            public void getDeviceName(String name) {
                initialName = currentAdapterName = name;
                if (listenerWifiDeviceName != null) {
                    listenerWifiDeviceName.getDeviceName(name);
                }

                unregisterInitName();
            }
        });
    }

    /**
     * Method allowing to discovery other wifi Direct hashMapWifiDevices.
     *
     * @param discoveryListener a discoveryListener object which serves as callback functions.
     */
    public void discovery(final DiscoveryListener discoveryListener) {

        final IntentFilter intentFilter = new IntentFilter();

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        //  Indicates a change in the list of available hashMapWifiDevices.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Listener onDiscoveryStarted
        discoveryListener.onDiscoveryStarted();

        WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {

                if (v) Log.d(TAG, "onPeersAvailable()");

                List<WifiP2pDevice> refreshedPeers = new ArrayList<>(peerList.getDeviceList());

                for (WifiP2pDevice wifiP2pDevice : refreshedPeers) {

                    WifiAdHocDevice device = new WifiAdHocDevice(wifiP2pDevice.deviceAddress.toUpperCase(),
                            wifiP2pDevice.deviceName);

                    if (!hashMapWifiDevices.containsKey(device.getMacAddress())) {
                        hashMapWifiDevices.put(device.getMacAddress(), device);
                        if (v) Log.d(TAG, "Devices added: " + device.getDeviceName());
                    } else {
                        if (v) Log.d(TAG, "Device " + device.getDeviceName() + " already present");
                    }

                    // Listener onDiscoveryStarted
                    discoveryListener.onDeviceDiscovered(device);
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(DISCOVERY_TIME);
                } catch (InterruptedException e) {
                    discoveryListener.onDiscoveryFailed(e);
                }
                discoveryListener.onDiscoveryCompleted(hashMapWifiDevices);
            }
        }).start();

        broadcastWifi.registerDiscovery(intentFilter, discoveryListener, peerListListener);
    }

    /**
     * Method allowing to connect to a remote wifi Direct peer.
     *
     * @param address a String value which represents the address of the remote wifi
     *                Direct peer.
     */
    public void connect(final String address) {

        // Get The device from its address
        final WifiAdHocDevice device = (WifiAdHocDevice) hashMapWifiDevices.get(address);
        final WifiP2pConfig config = new WifiP2pConfig();

        if (config.groupOwnerIntent != -1) {
            config.groupOwnerIntent = valueGroupOwner;
        }
        config.deviceAddress = device.getMacAddress().toLowerCase();
        config.wps.setup = WpsInfo.PBC;

        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (v) Log.d(TAG, "Start connecting Wifi Direct (onSuccess)");
                connectionListener.onConnectionStarted();
            }

            @Override
            public void onFailure(int reasonCode) {
                if (v)
                    Log.e(TAG, "Error during connecting Wifi Direct (onFailure): " + errorCode(reasonCode));

                connectionListener.onConnectionFailed(new NoConnectionException(errorCode(reasonCode)));
            }
        });
    }

    public void cancelConnection() {
        if (wifiP2pManager != null) {
            wifiP2pManager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    if (v) Log.d(TAG, "Cancel connecting Wifi Direct (onSuccess)");
                }

                @Override
                public void onFailure(int reasonCode) {
                    if (v) Log.e(TAG, "Error during canceling connection Wifi Direct " +
                            "(onFailure): " + reasonCode);
                }
            });
        }
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
        broadcastWifi.unregisterConnection();
    }

    /**
     * Method allowing to unregister the discovery broadcast.
     */
    public void unregisterDiscovery() {
        broadcastWifi.unregisterDiscovery();
    }

    private void unregisterInitName() {
        broadcastWifi.unregisterInitName();

    }

    public void unregisterGroupOwner() {
        broadcastWifi.unregisterGroupOwner();
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

    private byte[] getLocalIPAddress() throws SocketException {
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
            NetworkInterface intf = en.nextElement();
            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                InetAddress inetAddress = enumIpAddr.nextElement();
                if (!inetAddress.isLoopbackAddress() && inetAddress.toString().contains("192.168.49")) {
                    if (inetAddress instanceof Inet4Address) {
                        return inetAddress.getAddress();
                    }
                }
            }
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

    public void resetDeviceName() {
        if (initialName != null) {
            updateDeviceName(initialName);
        }
    }

    public boolean updateDeviceName(String name) {
        try {
            Method m = wifiP2pManager.getClass().getMethod("setDeviceName", new Class[]{channel.getClass(), String.class,
                    WifiP2pManager.ActionListener.class});
            m.invoke(wifiP2pManager, channel, name, null);

            currentAdapterName = name;

            return true;
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public void requestGO(final ListenerWifiGroupOwner listenerWifiGroupOwner) {
        if (listenerWifiGroupOwner != null) {
            final IntentFilter intentFilter = new IntentFilter();

            //  Indicates this device's details have changed.
            intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

            broadcastWifi.registerGroupOwner(intentFilter, listenerWifiGroupOwner);
        }
    }

    public void setValueGroupOwner(int valueGroupOwner) {
        this.valueGroupOwner = valueGroupOwner;
    }

    public void leaveWifiP2PGroup() {
        if (wifiP2pManager != null && channel != null) {
            wifiP2pManager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && wifiP2pManager != null && channel != null
                            && group.isGroupOwner()) {
                        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "removeGroup onSuccess -");
                            }

                            @Override
                            public void onFailure(int reason) {
                                Log.d(TAG, "removeGroup onFailure -" + reason);
                            }
                        });
                    }
                }
            });
        }
    }

    public void onEnableWifi(final ListenerAdapter listenerAdapter) {

        @SuppressLint("HandlerLeak") final Handler mHandler = new Handler(Looper.getMainLooper()) {
            // Used handler to avoid updating views in other threads than the main thread
            public void handleMessage(Message msg) {
                listenerAdapter.onEnableWifi((boolean) msg.obj);
            }
        };

        Thread t = new Thread() {
            @Override
            public void run() {

                try {
                    //check if connected!
                    while (!isConnected(context)) {
                        //Wait to connect
                        Thread.sleep(1000);
                    }

                    // Use handler to avoid using runOnUiThread in main app
                    mHandler.obtainMessage(1, true).sendToTarget();
                } catch (InterruptedException e) {
                    mHandler.obtainMessage(1, false).sendToTarget();
                }
            }
        };
        t.start();
    }

    private static boolean isConnected(Context context) {

        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifi != null && wifi.isWifiEnabled();
    }

    public interface ListenerWifiDeviceName {
        void getDeviceName(String name);
    }

    public interface ListenerWifiGroupOwner {
        void getGroupOwner(String address);
    }

    private String errorCode(int reasonCode) {
        switch (reasonCode) {
            case ERROR:
                return "P2P internal error";
            case P2P_UNSUPPORTED:
                return "P2P is not supported";
            case BUSY:
                return "P2P is busy";
        }

        return "Unknown error";
    }

    class BroadcastWifi {
        private boolean discoveryRegistered = false;
        private boolean connectionRegistered = false;
        private boolean nameRegistered = false;
        private boolean groupOwnerRegistered = false;

        private WiFiDirectBroadcastDiscovery wiFiDirectBroadcastDiscovery;
        private WiFiDirectBroadcastConnection wifiDirectBroadcastConnection;
        private WifiDirectBroadcastGroupOwner wifiDirectBroadcastGroupOwner;
        private WifiDirectBroadcastName wifiDirectBroadcastName;

        BroadcastWifi() {

        }

        void registerName(IntentFilter intentFilter,
                          final ListenerWifiDeviceName listenerWifiDeviceName) {
            wifiDirectBroadcastName = new WifiDirectBroadcastName(new ListenerWifiDeviceName() {
                @Override
                public void getDeviceName(String name) {
                    listenerWifiDeviceName.getDeviceName(name);
                }
            });
            nameRegistered = true;
            context.registerReceiver(wifiDirectBroadcastName, intentFilter);
        }

        void registerDiscovery(IntentFilter intentFilter, final DiscoveryListener discoveryListener,
                               WifiP2pManager.PeerListListener peerListListener) {
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
                    discoveryListener.onDiscoveryFailed(
                            new WifiDiscoveryException(errorCode(reasonCode)));
                }
            });
        }

        void registerConnection(IntentFilter intentFilter, WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable) {
            wifiDirectBroadcastConnection = new WiFiDirectBroadcastConnection(wifiP2pManager, channel,
                    onConnectionInfoAvailable, v);
            connectionRegistered = true;
            context.registerReceiver(wifiDirectBroadcastConnection, intentFilter);
        }

        void registerGroupOwner(IntentFilter intentFilter, final ListenerWifiGroupOwner listenerWifiGroupOwner) {
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

        void unregisterConnection() {
            if (connectionRegistered) {
                if (v) Log.d(TAG, "unregisterConnection()");
                context.unregisterReceiver(wifiDirectBroadcastConnection);
                connectionRegistered = false;
            }
        }

        void unregisterDiscovery() {
            if (discoveryRegistered) {
                if (v) Log.d(TAG, "unregisterDiscovery()");
                context.unregisterReceiver(wiFiDirectBroadcastDiscovery);
                discoveryRegistered = false;
            }
        }

        void unregisterInitName() {
            if (nameRegistered) {
                if (v) Log.d(TAG, "unregisterName()");
                context.unregisterReceiver(wifiDirectBroadcastName);
                nameRegistered = false;
            }
        }

        void unregisterGroupOwner() {
            if (groupOwnerRegistered) {
                if (v) Log.d(TAG, "unregisterGroupOwner()");
                context.unregisterReceiver(wifiDirectBroadcastGroupOwner);
                groupOwnerRegistered = false;
            }
        }

    }

}
