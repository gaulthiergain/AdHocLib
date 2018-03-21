package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.AbstractRemoteConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.RemoteWifiConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ActiveConnections;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DiscoveredDevice;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ListenerAodv;
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


public class WrapperHybridWifi extends AbstractWrapper {

    private static final String TAG = "[AdHoc][WrapperWifi]";
    private boolean wifiEnabled;

    private int serverPort;
    private String label;
    private String ownIpAddress;
    private String groupOwnerAddr;
    private WifiServiceServer wifiServiceServer;

    private HashMap<String, String> mapLabelMac;
    private Hashtable<String, NetworkManager> mapIpNetwork;
    private HashMap<String, DiscoveredDevice> mapAddressDevice;

    private WifiAdHocManager wifiAdHocManager;

    public WrapperHybridWifi(boolean v, Context context, short nbThreads, int serverPort,
                             String label, ActiveConnections activeConnections,
                             HashMap<String, DiscoveredDevice> mapAddressDevice,
                             final ListenerAodv listenerAodv, ListenerDataLinkAodv listenerDataLinkAodv)
            throws IOException {

        super(v, context, activeConnections, listenerAodv, listenerDataLinkAodv);

        try {
            ConnectionListener connectionListener = new ConnectionListener() {
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
                        wifiServiceServer.stopListening();
                    } catch (IOException e) {
                        listenerAodv.catchException(e);
                    }

                    _connect();

                }
            };
            this.wifiAdHocManager = new WifiAdHocManager(v, context, connectionListener);
            if (wifiAdHocManager.isEnabled()) {
                this.wifiEnabled = true;
                this.label = label;
                this.ownMac = wifiAdHocManager.getOwnMACAddress().toLowerCase();
                this.serverPort = serverPort;
                this.mapAddressDevice = mapAddressDevice;
                this.mapLabelMac = new HashMap<>();
                this.mapIpNetwork = new Hashtable<>();
                this.wifiAdHocManager.getDeviceName(new WifiAdHocManager.ListenerWifiDeviceName() {

                    @Override
                    public void getDeviceName(String name) {
                        // Update ownName
                        ownName = name;
                        wifiAdHocManager.unregisterInitName();
                    }
                });
                this.listenServer(nbThreads);
            } else {
                wifiEnabled = false;
            }
        } catch (DeviceException e) {
            wifiEnabled = false;
        }
    }

    public void listenServer(short nbThreads) throws IOException {
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
            public void onConnectionClosed(AbstractRemoteConnection remoteDevice) {

                //Get label from ip
                String remoteLabel = mapLabelMac.get(remoteDevice.getDeviceAddress());

                if (v) Log.d(TAG, "Server broken with " + remoteLabel);

                try {
                    remoteDevice.setDeviceAddress(remoteLabel);
                    listenerDataLinkAodv.brokenLink(remoteLabel);
                } catch (IOException | NoConnectionException e) {
                    listenerAodv.catchException(e);
                }

                listenerAodv.onConnectionClosed(remoteDevice);
            }

            @Override
            public void onConnection(AbstractRemoteConnection remoteDevice) {

            }
        });

        // Start the wifi server listening process
        wifiServiceServer.listen(nbThreads, serverPort);
    }

    private void _connect() {
        final WifiServiceClient wifiServiceClient = new WifiServiceClient(v, context, true,
                groupOwnerAddr, serverPort, 10000, ATTEMPTS, new MessageListener() {
            @Override
            public void onConnectionClosed(AbstractRemoteConnection remoteDevice) {

                //Get label from ip
                String remoteLabel = mapLabelMac.get(remoteDevice.getDeviceAddress());

                if (v) Log.d(TAG, "Client broken with " + remoteLabel);

                try {
                    remoteDevice.setDeviceAddress(remoteLabel);
                    listenerDataLinkAodv.brokenLink(remoteLabel);
                } catch (IOException | NoConnectionException e) {
                    listenerAodv.catchException(e);
                }

                listenerAodv.onConnectionClosed(remoteDevice);
            }

            @Override
            public void onConnection(AbstractRemoteConnection remoteDevice) {


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
            public void connected(String remoteAddress, NetworkManager network) throws IOException,
                    NoConnectionException {

                // Add mapping MAC - network
                mapIpNetwork.put(remoteAddress, network);

                // Send CONNECT message to establish the pairing
                wifiServiceClient.send(new MessageAdHoc(
                        new Header("CONNECT_SERVER", label, ownName), ownIpAddress));

            }
        });

        // Start the wifiServiceClient thread
        new Thread(wifiServiceClient).start();

    }

    public void connect(DiscoveredDevice device) {
        Log.d(TAG, "Remote Address" + device.getAddress());
        wifiAdHocManager.connect(device.getAddress());
    }

    public void processMsgReceived(final MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {
        Log.d(TAG, "Message rcvd " + message.toString());
        switch (message.getHeader().getType()) {
            case "CONNECT_SERVER":

                final NetworkManager networkManager = wifiServiceServer.getActiveConnections().get(message.getPdu().toString());
                if (networkManager != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkManager);

                    Log.d(TAG, "Add couple: " + networkManager.getISocket().getRemoteSocketAddress()
                            + " " + message.getHeader().getSenderAddr());

                    // Add mapping MAC - label
                    mapLabelMac.put(networkManager.getISocket().getRemoteSocketAddress(),
                            message.getHeader().getSenderAddr());

                }

                if (ownIpAddress == null) {
                    wifiAdHocManager.requestGO(new WifiAdHocManager.ListenerWifiGroupOwner() {
                        @Override
                        public void getGroupOwner(String address) {
                            Log.d(TAG, ">>>> GO: " + address);
                            ownIpAddress = address;
                            wifiAdHocManager.unregisterGroupOwner();
                            sendConnectClient(message, networkManager);
                        }
                    });
                } else {
                    sendConnectClient(message, networkManager);
                }
                break;
            case "CONNECT_CLIENT":
                NetworkManager networkManagerServer = mapIpNetwork.get(message.getPdu().toString());
                if (networkManagerServer != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkManagerServer);

                    Log.d(TAG, "Add couple: " + networkManagerServer.getISocket().getRemoteSocketAddress()
                            + " " + message.getHeader().getSenderAddr());

                    // Add mapping MAC - label
                    mapLabelMac.put(networkManagerServer.getISocket().getRemoteSocketAddress(),
                            message.getHeader().getSenderAddr());

                    // callback connection
                    listenerAodv.onConnection(new RemoteWifiConnection(message.getHeader().getSenderAddr(),
                            message.getHeader().getSenderName(), ownIpAddress));
                }

                break;
            default:
                // Handle messages in protocol scope
                listenerDataLinkAodv.processMsgReceived(message);
        }
    }

    @Override
    public void stopListening() throws IOException {
        wifiServiceServer.stopListening();
    }

    @Override
    public void getPaired() {

    }

    @Override
    public boolean isEnabled() {
        return wifiEnabled;
    }

    private void sendConnectClient(MessageAdHoc message, NetworkManager networkManager) {
        if (networkManager != null) {
            // Send CONNECT message to establish the pairing
            try {
                networkManager.sendMessage(new MessageAdHoc(
                        new Header("CONNECT_CLIENT", label, ownName), ownIpAddress));

                // callback connection
                listenerAodv.onConnection(new RemoteWifiConnection(message.getHeader().getSenderAddr(),
                        message.getHeader().getSenderName(), ownIpAddress));

            } catch (IOException e) {
                listenerAodv.catchException(e);
            }
        }
    }

    public void discovery() {
        wifiAdHocManager.discovery(new DiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {
                if (v) Log.d(TAG, "onDiscoveryStarted");
            }

            @Override
            public void onDiscoveryFailed(int reasonCode) {
                Log.d(TAG, "onDiscoveryFailed"); //todo exception here
            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, WifiP2pDevice> peerslist) {
                if (v) Log.d(TAG, "onDiscoveryCompleted");

                // Add devices into the peers
                for (Map.Entry<String, WifiP2pDevice> entry : peerslist.entrySet()) {
                    if (!mapAddressDevice.containsKey(entry.getValue().deviceAddress)) {
                        if (v) Log.d(TAG, "Add " + entry.getValue().deviceName + " into peers");
                        mapAddressDevice.put(entry.getValue().deviceAddress,
                                new DiscoveredDevice(entry.getValue().deviceAddress,
                                        entry.getValue().deviceName, DiscoveredDevice.WIFI));
                    }
                }

                if (discoveryListener != null) {
                    listenerAodv.onDiscoveryCompleted(mapAddressDevice);
                }

                discoveryCompleted = true;

                wifiAdHocManager.unregisterDiscovery();
            }
        });
    }

    @Override
    public void disconnect() {
        //todo add client into list and disconnect it
    }

    public void updateName(String name) {
        wifiAdHocManager.updateName(name);

    }

    public void unregisterConnection() {
        wifiAdHocManager.unregisterConnection();
    }
}
