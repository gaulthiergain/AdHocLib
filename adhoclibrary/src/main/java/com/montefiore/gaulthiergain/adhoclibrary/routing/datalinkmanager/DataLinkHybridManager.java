package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothDisabledException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice.AbstractRemoteDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice.RemoteWifiDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvAbstractException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class DataLinkHybridManager implements IDataLink {

    public static final String ID_APP = "#e091#";
    private static final String TAG = "[AdHoc][DataLinkHybrid]";

    private final boolean v;
    private final Context context;
    private final ActiveConnections activeConnections;
    private final ListenerAodv listenerAodv;
    private final ListenerDataLinkAodv listenerDataLinkAodv;
    private final String ownName;
    private final String ownStringUUID;

    //Wifi
    private int serverPort;
    private String ownIpAddress;
    private String groupOwnerAddr;
    private WifiAdHocManager wifiAdHocManager;
    private WifiServiceServer wifiServiceServer;
    private Hashtable<String, WifiP2pDevice> peers;
    private Hashtable<String, NetworkObject> hashmapIpNetwork;
    private boolean wifiEnabled = false;

    //Bt
    private boolean secure;
    private BluetoothManager bluetoothManager;
    private BluetoothServiceServer bluetoothServiceServer;

    private DataLinkHybridManager(boolean v, Context context, ListenerAodv listenerAodv,
                                  ListenerDataLinkAodv listenerDataLinkAodv) {
        this.v = v;
        this.context = context;
        this.listenerAodv = listenerAodv;
        this.listenerDataLinkAodv = listenerDataLinkAodv;
        this.activeConnections = new ActiveConnections();

        //todo update this
        this.ownName = BluetoothUtil.getCurrentName();
        this.ownStringUUID = BluetoothUtil.getCurrentMac(context).replace(":", "").toLowerCase();

        this.listenerDataLinkAodv.getDeviceName(ownName);
        this.listenerDataLinkAodv.getDeviceAddress(ownStringUUID);
    }

    public DataLinkHybridManager(boolean v, Context context, short nbThreadsWifi,
                                 int serverPort, boolean secure, short nbThreadsBt, short duration,
                                 ListenerAodv listenerAodv, final ListenerDataLinkAodv listenerDataLinkAodv) throws DeviceException, IOException, BluetoothDisabledException, BluetoothBadDuration {
        this(v, context, listenerAodv, listenerDataLinkAodv);

        //Wifi
        this.wifiAdHocManager = new WifiAdHocManager(v, context);
        if (wifiAdHocManager.isEnabled()) {
            this.wifiEnabled = true;
            this.serverPort = serverPort;
            this.peers = new Hashtable<>();
            this.hashmapIpNetwork = new Hashtable<>();
            this.listenServerWifi(nbThreadsWifi);
        }

        //Bt
        this.secure = secure;
        this.bluetoothManager = new BluetoothManager(v, context);

        // Check if the bluetooth adapter is enabled
        if (!bluetoothManager.isEnabled()) {
            if (!bluetoothManager.enable()) {
                throw new BluetoothDisabledException("Unable to enable Bluetooth adapter");
            }
            bluetoothManager.enableDiscovery(duration);
        }

        //todo listener sever bluetooth

    }

    /************WIFI**************/

    private void listenServerWifi(short nbThreadsWifi) throws IOException {
        wifiServiceServer = new WifiServiceServer(v, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException | NoConnectionException | AodvAbstractException e) {
                    listenerAodv.catchException(e);
                }
            }

            @Override
            public void onMessageSent(MessageAdHoc message) {
                if (v) Log.d(TAG, "Message sent: " + message.getPdu().toString());
            }

            @Override
            public void onForward(MessageAdHoc message) {
                if (v) Log.d(TAG, "OnForward: " + message.getPdu().toString());
            }

            @Override
            public void catchException(Exception e) {
                listenerAodv.catchException(e);
            }

            @Override
            public void onConnectionClosed(AbstractRemoteDevice remoteDevice) {

                RemoteWifiDevice remoteWifiDevice = (RemoteWifiDevice) remoteDevice;

                if (v) Log.d(TAG, "Server broken with " + remoteWifiDevice.getDeviceAddress());

                try {
                    listenerDataLinkAodv.brokenLink(remoteDevice.getDeviceAddress());
                } catch (IOException | NoConnectionException e) {
                    listenerAodv.catchException(e);
                }

                listenerAodv.onConnectionClosed(remoteDevice);
            }

            @Override
            public void onConnection(AbstractRemoteDevice remoteDevice) {
                listenerAodv.onConnection(remoteDevice);
            }
        });

        // Start the wifi server listening process
        wifiServiceServer.listen(nbThreadsWifi, serverPort);
    }

    private void _connect() {
        final WifiServiceClient wifiServiceClient = new WifiServiceClient(v, context, true,
                groupOwnerAddr, serverPort,
                10000, 3, new MessageListener() {
            @Override
            public void onConnectionClosed(AbstractRemoteDevice remoteDevice) {
                listenerAodv.onConnectionClosed(remoteDevice);
                //TODO update aodv code
            }

            @Override
            public void onConnection(AbstractRemoteDevice remoteDevice) {
                listenerAodv.onConnection(remoteDevice);
            }

            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException | NoConnectionException | AodvAbstractException e) {
                    listenerAodv.catchException(e);
                }
            }

            @Override
            public void onMessageSent(MessageAdHoc message) {
                if (v) Log.d(TAG, "Message sent: " + message.getPdu().toString());
            }

            @Override
            public void onForward(MessageAdHoc message) {
                if (v) Log.d(TAG, "Message forward: " + message.getPdu().toString());
            }

            @Override
            public void catchException(Exception e) {
                listenerAodv.catchException(e);
            }
        });

        wifiServiceClient.setListenerAutoConnect(new WifiServiceClient.ListenerAutoConnect() {
            @Override
            public void connected(String remoteAddress, NetworkObject network) throws IOException,
                    NoConnectionException {

                // Add network to temporary hashmap
                hashmapIpNetwork.put(remoteAddress, network);

                // Send CONNECT message to establish the pairing
                wifiServiceClient.send(new MessageAdHoc(
                        new Header("CONNECT_WIFI_SERVER", ownStringUUID, ownName), ownIpAddress));

            }
        });

        // Start the wifiServiceClient thread
        new Thread(wifiServiceClient).start();

    }



    /*--------------------------HYBRID----------------------------------*/

    private void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {
        Log.d(TAG, "Message rcvd " + message.toString());
        switch (message.getHeader().getType()) {
            case "CONNECT_WIFI_SERVER":


                final NetworkObject networkObject = wifiServiceServer.getActiveConnections().get(message.getPdu().toString());
                if (networkObject != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkObject);

                }

                if (ownIpAddress == null) {
                    wifiAdHocManager.requestGO(new WifiAdHocManager.ListenerWifiGroupOwner() {
                        @Override
                        public void getGroupOwner(String address) {
                            Log.d(TAG, ">>>> GO: " + address);
                            ownIpAddress = address;
                            wifiAdHocManager.unregisterGroupOwner();

                            if (networkObject != null) {
                                // Send CONNECT message to establish the pairing
                                try {
                                    networkObject.sendObjectStream(new MessageAdHoc(
                                            new Header("CONNECT_WIFI_CLIENT", ownStringUUID, ownName), ownIpAddress));
                                } catch (IOException e) {
                                    listenerAodv.catchException(e);
                                }

                            }
                        }
                    });
                }

                break;
            case "CONNECT_WIFI_CLIENT":
                NetworkObject networkObjectServer = hashmapIpNetwork.get(message.getPdu().toString());
                if (networkObjectServer != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkObjectServer);
                }

                break;
            default:
                // Handle messages in protocol scope
                listenerDataLinkAodv.processMsgReceived(message);
        }
    }

    @Override
    public void connect() {
        connectWifi();
    }

    private void connectWifi() {
        for (Map.Entry<String, WifiP2pDevice> deviceEntry : peers.entrySet()) {
            Log.d(TAG, "Remote Address" + deviceEntry.getValue().deviceAddress);
            wifiAdHocManager.connect(deviceEntry.getValue().deviceAddress, new ConnectionListener() {
                @Override
                public void onConnectionStarted() {
                    Log.d(TAG, "Connection Started");
                }

                @Override
                public void onConnectionFailed(int reasonCode) {
                    Log.d(TAG, "Connection Failed: " + reasonCode);
                }

                @Override
                public void onGroupOwner(InetAddress groupOwnerAddress) {
                    ownIpAddress = groupOwnerAddress.getHostAddress();
                    Log.d(TAG, "onGroupOwner: " + ownIpAddress);
                }

                @Override
                public void onClient(final InetAddress groupOwnerAddress, final InetAddress address) {

                    groupOwnerAddr = groupOwnerAddress.getHostAddress();
                    ownIpAddress = address.getHostAddress();

                    Log.d(TAG, "onClient groupOwner Address: " + groupOwnerAddress.getHostAddress());
                    Log.d(TAG, "OWN IP address: " + ownIpAddress);

                    try {
                        stopListening();
                    } catch (IOException e) {
                        listenerAodv.catchException(e);
                    }

                    _connect();

                }
            });
        }
    }

    @Override
    public void stopListening() throws IOException {
        if (wifiEnabled) {
            wifiServiceServer.stopListening();
        }
        //bluetoothServiceServer.stopListening();
    }

    @Override
    public void sendMessage(MessageAdHoc message, String address) throws IOException {

        NetworkObject networkObject = activeConnections.getActivesConnections().get(address);
        networkObject.sendObjectStream(message);
        if (v) Log.d(TAG, "Send directly to " + address);
    }

    @Override
    public boolean isDirectNeighbors(String address) {
        return activeConnections.getActivesConnections().containsKey(address);
    }

    @Override
    public void broadcastExcept(String originateAddr, MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : activeConnections.getActivesConnections().entrySet()) {
            if (!entry.getKey().equals(originateAddr)) {
                entry.getValue().sendObjectStream(message);
                if (v)
                    Log.d(TAG, "Broadcast Message to " + entry.getKey());
            }
        }
    }

    @Override
    public void broadcast(MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : activeConnections.getActivesConnections().entrySet()) {
            entry.getValue().sendObjectStream(message);
            if (v)
                Log.d(TAG, "Broadcast Message to " + entry.getKey());
        }
    }

    @Override
    public void discovery() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                discoveryBt();
            }
        }).start();

        if (wifiEnabled) {
            discoveryWifi();
        }
    }

    private void discoveryBt() {
        bluetoothManager.discovery(new com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.DiscoveryListener() {
            @Override
            public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                listenerAodv.onDiscoveryCompleted();
                // Stop and unregister to the discovery process
                bluetoothManager.unregisterDiscovery();
            }

            @Override
            public void onDiscoveryStarted() {

            }

            @Override
            public void onDeviceFound(BluetoothDevice device) {

            }

            @Override
            public void onScanModeChange(int currentMode, int oldMode) {

            }
        });
    }

    private void discoveryWifi() {
        wifiAdHocManager.discover(new DiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {

            }

            @Override
            public void onDiscoveryFailed(int reasonCode) {

            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, WifiP2pDevice> peerslist) {
                // Add no paired devices into the hashMapDevices
                for (Map.Entry<String, WifiP2pDevice> entry : peerslist.entrySet()) {
                    if (entry.getValue().deviceName != null &&
                            entry.getValue().deviceName.contains(DataLinkBtManager.ID_APP)) {
                        peers.put(entry.getValue().deviceName, entry.getValue());
                        if (v) Log.d(TAG, "Add " + entry.getValue().deviceName
                                + " into hashMapDevices");
                    }
                }

                wifiAdHocManager.unregisterDiscovery();

            }
        });
    }

    @Override
    public void getPaired() {

    }
}
