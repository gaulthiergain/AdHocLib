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

public class WifiAdHocManager implements WifiP2pManager.ChannelListener {

    public static String TAG = "[AdHoc][WifiManager]";

    private static final int MAX_TIMEOUT_CONNECT = 15000;
    private static final int DISCOVERY_TIME = 12000;
    private static final byte DISCOVERY_COMPLETED = 0;
    private static final byte DISCOVERY_FAILED = 1;

    private final boolean v;
    private Context context;
    private Channel channel;
    private String initialName;
    private String currentAdapterName;
    private WifiP2pManager wifiP2pManager;
    private ConnectionWifiListener connectionWifiListener;
    private final HashMap<String, AdHocDevice> mapMacDevices;

    private Timer timer;
    private boolean connected;
    private boolean discoveryRegistered = false;
    private boolean connectionRegistered = false;
    private int valueGroupOwner = -1;
    private DiscoveryListener discoveryListener;
    private WifiDeviceInfosListener wifiDeviceInfosListener;
    private WiFiDirectBroadcastDiscovery wiFiDirectBroadcastDiscovery;
    private WiFiDirectBroadcastConnection wifiDirectBroadcastConnection;

    /**
     * Constructor
     *
     * @param verbose a boolean value to set the debug/verbose mode.
     * @param context a Context object which gives global information about an application
     *                environment.
     */
    public WifiAdHocManager(boolean verbose, final Context context,
                            ConnectionWifiListener connectionWifiListener,
                            final WifiDeviceInfosListener listenerDeviceInfos) {

        this.v = verbose;
        this.context = context;
        this.wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = wifiP2pManager.initialize(context, getMainLooper(), null);
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

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        // Used handler to avoid updating views in other threads than the main thread
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DISCOVERY_COMPLETED:
                    discoveryListener.onDiscoveryCompleted(mapMacDevices);
                    break;
                case DISCOVERY_FAILED:
                    discoveryListener.onDiscoveryFailed(new Exception("Connection timeout"));
            }
        }
    };

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
        // Indicates a change in the list of available mapMacDevices.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Update discovery listener
        this.discoveryListener = discoveryListener;

        // Listener onDiscoveryStarted
        discoveryListener.onDiscoveryStarted();

        // Call discoverPeer method
        discoverPeer();

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
                        if (v)
                            Log.d(TAG, "Device " + device.getDeviceName() + " already present");
                    }

                    // Listener onDiscoveryStarted
                    discoveryListener.onDeviceDiscovered(device);
                }
            }
        };

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.obtainMessage(DISCOVERY_COMPLETED).sendToTarget();
            }
        }, DISCOVERY_TIME);


        registerDiscovery(intentFilter, peerListListener);
    }

    /**
     * Method allowing to connect to a remote wifi Direct peer.
     *
     * @param address a String value which represents the address of the remote wifi
     *                Direct peer.
     */
    public void connect(final String address) throws DeviceException {

        // Get The device from its address
        final WifiAdHocDevice device = (WifiAdHocDevice) mapMacDevices.get(address);
        if(device == null){
            throw new DeviceException("Discovery is required before connecting");
        }

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
                // Launch timeout to cancel connection
                timeout();
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

    private void timeout() {
        connected = false;
        if (timer != null) {
            // Cancel previous timer if any
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!connected) {
                    mHandler.obtainMessage(DISCOVERY_FAILED).sendToTarget();
                }
            }
        }, MAX_TIMEOUT_CONNECT);
    }


    public void removeGroup(final ListenerAction listenerAction) {
        if (wifiP2pManager != null && channel != null) {
            wifiP2pManager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && wifiP2pManager != null && channel != null
                            && group.isGroupOwner()) {
                        wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                listenerAction.onSuccess();
                            }

                            @Override
                            public void onFailure(int reason) {
                                listenerAction.onFailure(new Exception(errorCode(reason)));
                            }
                        });
                    }
                }
            });
        }
    }

    public void cancelConnection(final ListenerAction listenerAction) {
        if (wifiP2pManager != null) {
            wifiP2pManager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    listenerAction.onSuccess();
                }

                @Override
                public void onFailure(int reason) {
                    listenerAction.onFailure(new Exception(errorCode(reason)));
                }
            });
        }
    }

    public void disable() {
        wifiP2pManager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (v) Log.d(TAG, "onSuccess() stopPeerDiscovery");
            }

            @Override
            public void onFailure(int reason) {
                if (v) Log.e(TAG, "onFailure() stopPeerDiscovery" + errorCode(reason));
            }
        });

        if (discoveryRegistered) {
            unregisterDiscovery();
        }

        if (connectionRegistered) {
            unregisterConnection();
        }

        wifiAdapterState(false);
    }

    /**
     * Method allowing to enable the wifi adapter.
     */
    public void enable() {
        wifiAdapterState(true);
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
                                if (v) Log.d(TAG, "removeGroup onSuccess");
                            }

                            @Override
                            public void onFailure(int reason) {
                                if (v) Log.e(TAG, "removeGroup onFailure: " + errorCode(reason));
                            }
                        });
                    }
                }
            });
        }
    }

    public void updateContext(Context context) {
        if (v) Log.d(TAG, "Updating context");

        if (connectionRegistered) {
            unregisterConnection();
        }

        this.wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = wifiP2pManager.initialize(context, getMainLooper(), null);

        this.context = context;
        this.registerConnection();

    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (wifiP2pManager != null) {
            if (v) Log.w(TAG, "Channel lost, update wifi manager");
            this.channel = wifiP2pManager.initialize(context, getMainLooper(), this);
        }
    }


    public interface WifiDeviceInfosListener {
        void getDeviceInfos(String name, String mac);
    }

    public interface ListenerPeer {
        void discoverPeers();
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

    private void wifiAdapterState(boolean state) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            wifi.setWifiEnabled(state);
        }
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

    private void registerDiscovery(IntentFilter intentFilter, WifiP2pManager.PeerListListener peerListListener) {
        wiFiDirectBroadcastDiscovery = new WiFiDirectBroadcastDiscovery(v, wifiP2pManager, channel,
                peerListListener);
        discoveryRegistered = true;
        context.registerReceiver(wiFiDirectBroadcastDiscovery, intentFilter);
    }

    private void registerConnection(IntentFilter intentFilter,
                                    WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable,
                                    WifiAdHocManager.ListenerPeer listenerPeer) {
        wifiDirectBroadcastConnection = new WiFiDirectBroadcastConnection(v, wifiP2pManager, channel,
                wifiDeviceInfosListener, listenerPeer, onConnectionInfoAvailable);
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

    private void registerConnection() {

        // Define listener to update the status of the peer
        ListenerPeer listenerPeer = new ListenerPeer() {
            @Override
            public void discoverPeers() {
                // Call discoverPeer method
                discoverPeer();
            }
        };

        final IntentFilter intentFilter = new IntentFilter();
        // Indicates a change in the Wi-Fi P2P status.
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

                // Device is connected to remote node
                connected = true;

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

        // Register to receiver to get connection details
        registerConnection(intentFilter, onConnectionInfoAvailable, listenerPeer);
    }

    private void discoverPeer() {
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (v) Log.d(TAG, "Start discoveryPeers");
            }

            @Override
            public void onFailure(int reasonCode) {
                if (v)
                    Log.e(TAG, "Error start discoveryPeers (onFailure): " + errorCode(reasonCode));
                if (discoveryListener != null) {
                    discoveryListener.onDiscoveryFailed(new Exception(errorCode(reasonCode)));
                }
            }
        });
    }

    /**
     * Method allowing to enable the wifi Direct adapter.
     *
     * @return a boolean value which represents the state of the wifi Direct.
     */
    public static boolean isWifiEnabled(Context context) {
        WifiManager mng = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return mng != null && mng.isWifiEnabled();
    }

}
