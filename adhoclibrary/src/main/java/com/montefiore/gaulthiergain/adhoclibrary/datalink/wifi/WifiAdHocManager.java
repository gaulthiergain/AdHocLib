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

import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAction;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.net.wifi.p2p.WifiP2pManager.BUSY;
import static android.net.wifi.p2p.WifiP2pManager.ERROR;
import static android.net.wifi.p2p.WifiP2pManager.P2P_UNSUPPORTED;
import static android.os.Looper.getMainLooper;

public class WifiAdHocManager {

    public static String TAG = "[AdHoc][WifiManager]";

    public static final int DISCOVERY_TIME = 10000;
    private static final byte DISCOVERY_COMPLETED = 0;
    private static final byte DISCOVERY_FAILED = 1;

    private boolean v;
    private Context context;
    private Channel channel;
    private String initialName;
    private String currentAdapterName;
    private WifiP2pManager wifiP2pManager;
    private ConnectionWifiListener connectionWifiListener;
    private HashMap<String, AdHocDevice> mapMacDevices;

    private int valueGroupOwner = -1;
    private WifiDeviceInfosListener wifiDeviceInfosListener;

    private WiFiDirectBroadcastDiscovery wiFiDirectBroadcastDiscovery;
    private WiFiDirectBroadcastConnection wifiDirectBroadcastConnection;
    private boolean discoveryRegistered = false;
    private boolean connectionRegistered = false;

    /**
     * Constructor
     *
     * @param verbose                a boolean value to set the debug/verbose mode.
     * @param context                a Context object which gives global information about an application
     *                               environment.
     * @param connectionWifiListener a connectionWifiListener object which serves as callback functions.
     */
    public WifiAdHocManager(boolean verbose, final Context context,
                            final WifiDeviceInfosListener listenerDeviceInfos,
                            final ConnectionWifiListener connectionWifiListener) throws DeviceException {

        this.wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        if (wifiP2pManager == null) {

            // Device does not support Wifi Direct
            throw new DeviceException("Error device does not support Wifi Direct");
        } else {
            // Device supports Wifi Direct
            this.v = verbose;
            this.channel = wifiP2pManager.initialize(context, getMainLooper(), null);
            this.context = context;
            this.mapMacDevices = new HashMap<>();
            this.wifiDeviceInfosListener = new WifiDeviceInfosListener() {

                @Override
                public void getDeviceInfos(String name, String mac) {
                    initialName = currentAdapterName = name;
                    if (listenerDeviceInfos != null) {
                        listenerDeviceInfos.getDeviceInfos(name, mac);
                    }
                }
            };
            if (connectionWifiListener != null) {
                this.connectionWifiListener = connectionWifiListener;
                this.registerConnection();
            }
        }
    }

    public String getAdapterName() {
        return currentAdapterName;
    }

    /**
     * Method allowing to discovery other wifi Direct mapMacDevices.
     *
     * @param discoveryListener a discoveryListener object which serves as callback functions.
     */
    public void discovery(final DiscoveryListener discoveryListener) {

        final IntentFilter intentFilter = new IntentFilter();

        //  Indicates a change in the list of available mapMacDevices.
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

                    if (!mapMacDevices.containsKey(device.getMacAddress())) {
                        mapMacDevices.put(device.getMacAddress(), device);
                        if (v) Log.d(TAG, "Devices added: " + device.getDeviceName());
                    } else {
                        if (v) Log.d(TAG, "Device " + device.getDeviceName() + " already present");
                    }

                    // Listener onDiscoveryStarted
                    discoveryListener.onDeviceDiscovered(device);
                }
            }
        };

        @SuppressLint("HandlerLeak") final Handler mHandler = new Handler(Looper.getMainLooper()) {
            // Used handler to avoid updating views in other threads than the main thread
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case DISCOVERY_COMPLETED:
                        discoveryListener.onDiscoveryCompleted(mapMacDevices);
                        break;
                    case DISCOVERY_FAILED:
                        discoveryListener.onDiscoveryFailed((Exception) msg.obj);
                        break;
                }
            }
        };

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.obtainMessage(DISCOVERY_COMPLETED).sendToTarget();
            }
        }, DISCOVERY_TIME);


        registerDiscovery(intentFilter, discoveryListener, peerListListener);
    }

    /**
     * Method allowing to connect to a remote wifi Direct peer.
     *
     * @param address a String value which represents the address of the remote wifi
     *                Direct peer.
     */
    public void connect(final String address) {

        // Get The device from its address
        final WifiAdHocDevice device = (WifiAdHocDevice) mapMacDevices.get(address);
        final WifiP2pConfig config = new WifiP2pConfig();

        if (valueGroupOwner != -1) {
            config.groupOwnerIntent = valueGroupOwner;
        }
        config.deviceAddress = device.getMacAddress().toLowerCase();
        config.wps.setup = WpsInfo.PBC;

        wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (v) Log.d(TAG, "Start connecting Wifi Direct (onSuccess)");
                connectionWifiListener.onConnectionStarted();
            }

            @Override
            public void onFailure(int reasonCode) {
                if (v)
                    Log.e(TAG, "Error during connecting Wifi Direct (onFailure): " + errorCode(reasonCode));
                connectionWifiListener.onConnectionFailed(new NoConnectionException(errorCode(reasonCode)));
            }
        });
    }


    public void removeGroup(final ListenerAction listenerAction) {
        if (wifiP2pManager != null && channel != null) {
            wifiP2pManager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && wifiP2pManager != null && channel != null
                            && group.isGroupOwner()) {
                        wifiP2pManager.removeGroup(channel, listenerAction);
                    }
                }
            });
        }
    }

    public void cancelConnection(ListenerAction listenerAction) {
        if (wifiP2pManager != null) {
            wifiP2pManager.cancelConnect(channel, listenerAction);
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

    public void setValueGroupOwner(int valueGroupOwner) {
        this.valueGroupOwner = valueGroupOwner;
    }

    public void unregisterConnection() {
        if (connectionRegistered) {
            if (v) Log.d(TAG, "unregisterConnection()");
            context.unregisterReceiver(wifiDirectBroadcastConnection);
            connectionRegistered = false;
        }
    }

    public void unregisterDiscovery() {
        if (discoveryRegistered) {
            if (v) Log.d(TAG, "unregisterDiscovery()");
            context.unregisterReceiver(wiFiDirectBroadcastDiscovery);
            discoveryRegistered = false;
        }
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

    public void updateContext(Context context) {
        this.context = context;
    }

    public interface WifiDeviceInfosListener {
        void getDeviceInfos(String name, String mac);
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

    private void registerConnection() {

        final IntentFilter intentFilter = new IntentFilter();

        //  Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        // Indicates this device's details are available
        intentFilter.addAction(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);

        WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(final WifiP2pInfo info) {
                if (v) Log.d(TAG, "Address groupOwner:"
                        + String.valueOf(info.groupOwnerAddress.getHostAddress()));


                if (info.isGroupOwner) {
                    connectionWifiListener.onGroupOwner(info.groupOwnerAddress);
                } else {
                    try {
                        byte[] addr = getLocalIPAddress();
                        if (addr != null) {
                            connectionWifiListener.onClient(info.groupOwnerAddress,
                                    InetAddress.getByName(getDottedDecimalIP(addr)));
                        } else {
                            connectionWifiListener.onConnectionFailed(
                                    new NoConnectionException("Unknown IP address"));
                        }
                    } catch (UnknownHostException | SocketException e) {
                        connectionWifiListener.onConnectionFailed(e);
                    } catch (IOException e) {
                        connectionWifiListener.onConnectionFailed(e);
                    }
                }
            }
        };

        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (v) Log.d(TAG, "Start discoveryPeers");
            }

            @Override
            public void onFailure(int reasonCode) {
                if (v)
                    Log.e(TAG, "Error start discoveryPeers (onFailure): " + errorCode(reasonCode));
            }
        });

        registerConnection(intentFilter, onConnectionInfoAvailable);
    }

    private static boolean isConnected(Context context) {

        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifi != null && wifi.isWifiEnabled();
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

    private void registerDiscovery(IntentFilter intentFilter, final DiscoveryListener discoveryListener,
                                   WifiP2pManager.PeerListListener peerListListener) {
        wiFiDirectBroadcastDiscovery = new WiFiDirectBroadcastDiscovery(v, wifiP2pManager, channel,
                peerListListener);
        discoveryRegistered = true;
        context.registerReceiver(wiFiDirectBroadcastDiscovery, intentFilter);
    }

    private void registerConnection(IntentFilter intentFilter, WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable) {
        wifiDirectBroadcastConnection = new WiFiDirectBroadcastConnection(wifiP2pManager, channel, wifiDeviceInfosListener,
                onConnectionInfoAvailable, v);
        connectionRegistered = true;
        context.registerReceiver(wifiDirectBroadcastConnection, intentFilter);
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
}
