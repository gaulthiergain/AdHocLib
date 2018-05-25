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
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static android.net.wifi.p2p.WifiP2pManager.BUSY;
import static android.net.wifi.p2p.WifiP2pManager.ERROR;
import static android.net.wifi.p2p.WifiP2pManager.P2P_UNSUPPORTED;
import static android.os.Looper.getMainLooper;

/**
 * <p>This class manages the Wi-Fi discovery and the pairing with other Wi-FI devices.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class WifiAdHocManager implements WifiP2pManager.ChannelListener {

    private static String TAG = "[AdHoc][WifiManager]";
    private static final String TXTRECORD_SERVER_PORT = "server_port";
    private static final String SERVICE_INSTANCE = "_adhoclibrary";
    private static final String SERVICE_REG_TYPE = "_presence._tcp";
    private static final int MAX_TIMEOUT_CONNECT = 20000;
    private static final byte DISCOVERY_COMPLETED = 0;
    private static final byte DISCOVERY_FAILED = 1;

    public static final int MAX_SERVICE_DISC_TIME_OUT = 5000;
    public static final int DISCOVERY_TIME = 10000;

    private final boolean v;
    private final int serverPort;
    private Context context;
    private Channel channel;
    private String initialName;
    private String currentAdapterName;
    private WifiP2pManager wifiP2pManager;
    private ConnectionWifiListener connectionWifiListener;
    private final HashMap<String, AdHocDevice> mapMacDevices;

    private Timer serviceTimer;
    private Timer connectionTimer;
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
     * @param verbose                a boolean value to set the debug/verbose mode.
     * @param context                a Context object which gives global information about an application
     *                               environment.
     * @param serverPort             an integer value to set the listening port number.
     * @param connectionWifiListener a ConnectionWifiListener object which contains callback functions.
     * @param listenerDeviceInfos    a ConnectionWifiListener object which contains callback functions.
     */
    public WifiAdHocManager(boolean verbose, final Context context, int serverPort,
                            ConnectionWifiListener connectionWifiListener,
                            final WifiDeviceInfosListener listenerDeviceInfos) {

        this.v = verbose;
        this.context = context;
        this.serverPort = serverPort;
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
                    discoveryListener.onDiscoveryFailed(new Exception("Connection timeout. Try to disable then Enable Wifi."));
                    break;
            }
        }
    };

    /**
     * Method allowing to discovery other Wi-Fi Direct devices.
     *
     * @param discoveryListener a discoveryListener object which contains callback functions.
     */
    public void discovery(final DiscoveryListener discoveryListener) {

        if (serviceTimer != null) {
            serviceTimer.cancel();
            serviceTimer = null;
        }

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

        // Launch timer to stop the discovery process
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.obtainMessage(DISCOVERY_COMPLETED).sendToTarget();
            }
        }, DISCOVERY_TIME);

        // register to broadcast receiver
        registerDiscovery(intentFilter, peerListListener);
    }

    /**
     * Method allowing to connect to a remote wifi Direct peer.
     *
     * @param remoteAddress a String value which represents the IP address of the remote peer.
     */
    public void connect(final String remoteAddress) throws DeviceException {

        // Get The device from its address
        final WifiAdHocDevice device = (WifiAdHocDevice) mapMacDevices.get(remoteAddress);
        if (device == null) {
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
                timeoutConnection();
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

    /**
     * Method allowing to remove a existing P2P group.
     *
     * @param listenerAction a ListenerAction object which contains callback functions.
     */
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

    /**
     * Method allowing to cancel a connection.
     *
     * @param listenerAction a ListenerAction object which contains callback functions.
     */
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

    /**
     * Method allowing to disable the Wi-Fi adapter.
     */
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

        // Unregister broadcast receivers
        if (discoveryRegistered) {
            unregisterDiscovery();
        }

        if (connectionRegistered) {
            unregisterConnection();
        }

        wifiAdapterState(false);
    }

    /**
     * Method allowing to enable the Wi-Fi adapter.
     */
    public void enable() {
        wifiAdapterState(true);
    }

    /**
     * Method allowing to reset the name of the device adapter.
     */
    public void resetDeviceName() {
        if (initialName != null) {
            updateDeviceName(initialName);
        }
    }

    /**
     * Method allowing to update the device local adapter name.
     *
     * @param name a String value which represents the new name of the  device adapter.
     * @return true if the name was set, false otherwise.
     */
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

    /**
     * Method allowing to get the Wi-Fi adapter name.
     *
     * @return a String value which represents the name of the Wi-Fi adapter.
     */
    public String getAdapterName() {
        return currentAdapterName;
    }

    /**
     * Method allowing to update the Group Owner value to influence the choice of the Group Owner
     * negotiation.
     *
     * @param valueGroupOwner an integer value between 0 and 15 where 0 indicates the least
     *                        inclination to be a group owner and 15 indicates the highest inclination
     *                        to be a group owner. A value of -1 indicates the system can choose
     *                        an appropriate value.
     */
    public void setValueGroupOwner(int valueGroupOwner) {
        this.valueGroupOwner = valueGroupOwner;
    }

    /**
     * Method allowing to leave a Wi-Fi group.
     */
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

    /**
     * Method allowing to unregister the connection broadcast.
     */
    public void unregisterConnection() {
        if (connectionRegistered) {
            if (v) Log.d(TAG, "unregisterConnection()");
            context.unregisterReceiver(wifiDirectBroadcastConnection);
            connectionRegistered = false;
        }
    }

    /**
     * Method allowing to unregister the discovery broadcast.
     */
    public void unregisterDiscovery() {
        if (discoveryRegistered) {
            if (v) Log.d(TAG, "unregisterDiscovery()");
            context.unregisterReceiver(wiFiDirectBroadcastDiscovery);
            discoveryRegistered = false;
        }
    }

    /**
     * Method allowing to update the context of the current class.
     *
     * @param context a Context object which gives global information about an application
     *                environment.
     */
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

    /**
     * Method allowing to notify if the Wi-Fi adapter has been enabled.
     *
     * @param listenerAdapter a listenerAdapter object which contains callback functions.
     */
    public void onEnableWifi(final ListenerAdapter listenerAdapter) {

        @SuppressLint("HandlerLeak") final Handler handlerEnable = new Handler(Looper.getMainLooper()) {
            // Used handler to avoid updating views in other threads than the main thread
            public void handleMessage(Message msg) {
                listenerAdapter.onEnableWifi((boolean) msg.obj);
            }
        };

        Thread t = new Thread() {
            @Override
            public void run() {

                try {
                    // Check if connected
                    while (!isConnected(context)) {
                        //Wait to connect
                        Thread.sleep(1000);
                    }

                    // Use handler to avoid using runOnUiThread in main app
                    handlerEnable.obtainMessage(1, true).sendToTarget();
                } catch (InterruptedException e) {
                    handlerEnable.obtainMessage(1, false).sendToTarget();
                }
            }
        };
        t.start();
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

    /**
     * Method allowing to add a local Wi-Fi service to the device.
     */
    public void startRegistration() {
        Map<String, String> record = new HashMap<>();
        record.put(TXTRECORD_SERVER_PORT, String.valueOf(serverPort));
        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        wifiP2pManager.addLocalService(channel, service, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (v) Log.d(TAG, "Added Local Service");
            }

            @Override
            public void onFailure(int reasonCode) {
                if (v) Log.e(TAG, "Failed to add a service: " + errorCode(reasonCode));
            }
        });
    }

    /**
     * Method allowing to discover a remote service from a peer device.
     *
     * @param serviceListener a ServiceDiscoverListener object which contains callback functions.
     */
    public void discoverService(final ServiceDiscoverListener serviceListener) {

        /*
         * Register listeners for DNS-SD services.
         * These are callbacks invoked by the system when a service is actually discovered.
         */
        wifiP2pManager.setDnsSdResponseListeners(channel,
                new WifiP2pManager.DnsSdServiceResponseListener() {
                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                                                        String registrationType, WifiP2pDevice srcDevice) {
                        // A service has been discovered.
                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
                            WiFiP2pService service = new WiFiP2pService();
                            service.device = srcDevice;
                            service.instanceName = instanceName;
                            service.serviceRegistrationType = registrationType;

                            if (v) Log.d(TAG, "onBonjourServiceAvailable "
                                    + instanceName);

                        }
                    }
                }, new WifiP2pManager.DnsSdTxtRecordListener() {
                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {

                        serviceTimer.cancel();

                        try {
                            int port = Integer.valueOf(record.get(TXTRECORD_SERVER_PORT));
                            serviceListener.onServiceCompleted(port);
                        } catch (Exception e) {
                            serviceListener.onServiceCompleted(serverPort);
                        }
                    }
                });
        // After attaching listeners, create a service request and initiate discovery.
        WifiP2pDnsSdServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        wifiP2pManager.addServiceRequest(channel, serviceRequest,
                new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        if (v) Log.d(TAG, "Added service discovery request");
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        if (v)
                            Log.e(TAG, "Failed adding service discovery request: " + errorCode(reasonCode));
                    }
                });
        wifiP2pManager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                if (v) Log.d(TAG, "Service discovery initiated");
            }

            @Override
            public void onFailure(int reasonCode) {
                if (v) Log.e(TAG, "Service discovery failed: " + errorCode(reasonCode));
            }
        });

        serviceTimer = new Timer();
        serviceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Launch timer to notify that service is completed
                serviceListener.onServiceCompleted(serverPort);
            }
        }, MAX_SERVICE_DISC_TIME_OUT);
    }

    public interface WifiDeviceInfosListener {
        void getDeviceInfos(String name, String mac);
    }

    public interface ListenerPeer {
        void discoverPeers();
    }

    public interface ServiceDiscoverListener {
        void onServiceCompleted(int port);
    }

    /**
     * Method allowing to update the state of the Wi-Fi adapter.
     *
     * @param state a boolean value which represents the state of the Wi-Fi adapter.
     */
    private void wifiAdapterState(boolean state) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            wifi.setWifiEnabled(state);
        }
    }

    /**
     * Method allowing to check if the status of the Wi-Fi adapter.
     *
     * @param context a Context object which gives global information about an application
     *                environment.
     * @return a boolean value which represents the state of the Wi-Fi adapter.
     */
    private static boolean isConnected(Context context) {

        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifi != null && wifi.isWifiEnabled();
    }

    /**
     * Method allowing to get the String message associated to an integer error.
     *
     * @param reasonCode an integer value which represents the reason for failure.
     * @return a String value which represents the reason for failure.
     */
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

    /**
     * Method allowing to register to a broadcast receiver for managing Wi-Fi discovery.
     *
     * @param intentFilter     an IntentFilter object to subscribe to specific events.
     * @param peerListListener an interface for callback invocation when peer list is
     *                         available.
     */
    private void registerDiscovery(IntentFilter intentFilter, WifiP2pManager.PeerListListener peerListListener) {
        wiFiDirectBroadcastDiscovery = new WiFiDirectBroadcastDiscovery(v, wifiP2pManager, channel,
                peerListListener);
        discoveryRegistered = true;
        context.registerReceiver(wiFiDirectBroadcastDiscovery, intentFilter);
    }

    /**
     * Method allowing to register to a broadcast receiver for managing Wi-Fi connections.
     *
     * @param intentFilter              an IntentFilter object to subscribe to specific events.
     * @param onConnectionInfoAvailable an interface for callback invocation when connection info
     *                                  is available.
     * @param peerListListener          an interface for callback invocation when peer list is
     *                                  available.
     */
    private void registerConnection(IntentFilter intentFilter,
                                    WifiP2pManager.ConnectionInfoListener onConnectionInfoAvailable,
                                    WifiAdHocManager.ListenerPeer peerListListener) {
        wifiDirectBroadcastConnection = new WiFiDirectBroadcastConnection(v, wifiP2pManager, channel,
                wifiDeviceInfosListener, peerListListener, onConnectionInfoAvailable);
        connectionRegistered = true;
        context.registerReceiver(wifiDirectBroadcastConnection, intentFilter);
    }

    /**
     * Method allowing to get the current IP of the device.
     *
     * @return a byte array which represents the current IP of the device.
     * @throws SocketException signals that there is an error creating or accessing a Socket.
     */
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

    /**
     * Method allowing to get the current IP address of the device.
     *
     * @param ipAddr a byte array which represents the current IP address of the device.
     * @return a String value which the current IP address of the device.
     */
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

    /**
     * Method allowing to register to broadcast receiver for connection events.
     */
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

    /**
     * Method allowing to discover remote peers.
     */
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
     * Method allowing to set a time-out for a Wi-Fi connection.
     */
    private void timeoutConnection() {
        connected = false;
        if (connectionTimer != null) {
            // Cancel previous timer if any
            connectionTimer.cancel();
        }
        connectionTimer = new Timer();
        connectionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!connected) {
                    mHandler.obtainMessage(DISCOVERY_FAILED).sendToTarget();
                }
            }
        }, MAX_TIMEOUT_CONNECT);
    }

    /**
     * Inner class used for service discovery management.
     */
    private class WiFiP2pService {
        WifiP2pDevice device;
        String instanceName = null;
        String serviceRegistrationType = null;
    }
}
