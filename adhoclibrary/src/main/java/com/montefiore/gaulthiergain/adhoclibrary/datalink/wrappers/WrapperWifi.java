package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.applayer.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.RemoteConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ActiveConnections;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DiscoveredDevice;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvAbstractException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class WrapperWifi extends AbstractWrapper {

    private static final String TAG = "[AdHoc][WrapperWifi]";

    private int serverPort;
    private String ownIpAddress;
    private String groupOwnerAddr;
    private WifiAdHocManager wifiAdHocManager;
    private WifiServiceServer wifiServiceServer;

    private HashMap<String, NetworkManager> mapIpNetwork;
    private HashMap<String, String> mapLabelRemoteDeviceName;

    public WrapperWifi(boolean v, Context context, short nbThreads, int serverPort,
                       String label, ActiveConnections activeConnections,
                       HashMap<String, DiscoveredDevice> mapAddressDevice,
                       final ListenerApp listenerApp, ListenerDataLink listenerDataLink)
            throws IOException {

        super(v, context, label, mapAddressDevice, activeConnections, listenerApp, listenerDataLink);

        try {
            ConnectionListener connectionListener = new ConnectionListener() {
                @Override
                public void onConnectionStarted() {
                    Log.d(TAG, "Connection Started");
                }

                @Override
                public void onConnectionFailed(int reasonCode) {
                    Log.d(TAG, "Connection Failed: " + reasonCode);
                    wifiAdHocManager.cancelConnection();
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
                        listenerApp.catchException(e);
                    }

                    _connect();
                }
            };
            this.wifiAdHocManager = new WifiAdHocManager(v, context, connectionListener);
            if (wifiAdHocManager.isEnabled()) {
                this.type = DataLinkManager.WIFI;
                this.ownMac = wifiAdHocManager.getOwnMACAddress().toLowerCase();
                this.serverPort = serverPort;
                this.mapIpNetwork = new HashMap<>();
                this.mapLabelRemoteDeviceName = new HashMap<>();
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
                enabled = false;
            }
        } catch (DeviceException e) {
            enabled = false;
        }
    }

    public void listenServer(short nbThreads) throws IOException {
        wifiServiceServer = new WifiServiceServer(v, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException | NoConnectionException | AodvAbstractException e) {
                    listenerApp.catchException(e);
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
                listenerApp.catchException(e);
            }

            @Override
            public void onConnectionClosed(RemoteConnection remoteDevice) {

                //Get label from ip
                String remoteLabel = mapLabelAddr.get(remoteDevice.getDeviceAddress());

                if (v) Log.d(TAG, "Server broken with " + remoteLabel);

                try {
                    remoteDevice.setDeviceAddress(remoteLabel);
                    remoteDevice.setDeviceName(mapLabelRemoteDeviceName.get(remoteLabel));
                    activeConnections.getActivesConnections().remove(remoteLabel);
                    listenerDataLink.brokenLink(remoteLabel);
                } catch (IOException | NoConnectionException e) {
                    listenerApp.catchException(e);
                }

                listenerApp.onConnectionClosed(remoteDevice.getDeviceName(), remoteLabel);
            }

            @Override
            public void onConnection(RemoteConnection remoteDevice) {

            }
        });

        // Start the wifi server listening process
        wifiServiceServer.listen(nbThreads, serverPort);
    }

    private void _connect() {
        final WifiServiceClient wifiServiceClient = new WifiServiceClient(v, context, true,
                groupOwnerAddr, serverPort, 10000, ATTEMPTS, new MessageListener() {
            @Override
            public void onConnectionClosed(RemoteConnection remoteDevice) {

                //Get label from ip
                String remoteLabel = mapLabelAddr.get(remoteDevice.getDeviceAddress());

                if (v) Log.d(TAG, "Client broken with " + remoteLabel);

                try {
                    remoteDevice.setDeviceAddress(remoteLabel);
                    remoteDevice.setDeviceName(mapLabelRemoteDeviceName.get(remoteLabel));
                    activeConnections.getActivesConnections().remove(remoteLabel);
                    listenerDataLink.brokenLink(remoteLabel);
                } catch (IOException | NoConnectionException e) {
                    listenerApp.catchException(e);
                }

                listenerApp.onConnectionClosed(remoteDevice.getDeviceName(), remoteLabel);
            }

            @Override
            public void onConnection(RemoteConnection remoteDevice) {


            }

            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException | NoConnectionException | AodvAbstractException e) {
                    listenerApp.catchException(e);
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
                listenerApp.catchException(e);
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
                        new Header(CONNECT_SERVER, label, ownName), ownIpAddress));

            }
        });

        // Start the wifiServiceClient thread
        new Thread(wifiServiceClient).start();

    }

    public void connect(DiscoveredDevice device) {
        Log.d(TAG, "Remote Address" + device.getAddress());
        wifiAdHocManager.connect(device.getAddress());
    }

    private void processMsgReceived(final MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {
        Log.d(TAG, "Message rcvd " + message.toString());
        switch (message.getHeader().getType()) {
            case CONNECT_SERVER: {
                final NetworkManager networkManager = wifiServiceServer.getActiveConnections().get(message.getPdu().toString());
                if (networkManager != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(),
                            new NetworkObject(type, networkManager));

                    Log.d(TAG, "Add couple: " + networkManager.getISocket().getRemoteSocketAddress()
                            + " " + message.getHeader().getSenderAddr());

                    // Add mapping MAC - label
                    mapLabelAddr.put(networkManager.getISocket().getRemoteSocketAddress(),
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
            }
            case CONNECT_CLIENT: {
                NetworkManager networkManager = mapIpNetwork.get(message.getPdu().toString());
                if (networkManager != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(),
                            new NetworkObject(type, networkManager));

                    Log.d(TAG, "Add couple: " + networkManager.getISocket().getRemoteSocketAddress()
                            + " " + message.getHeader().getSenderAddr());

                    // Add mapping MAC - label
                    mapLabelAddr.put(networkManager.getISocket().getRemoteSocketAddress(),
                            message.getHeader().getSenderAddr());

                    // Add mapping label - remoteConnection
                    mapLabelRemoteDeviceName.put(message.getHeader().getSenderAddr(),
                            message.getHeader().getSenderName());

                    // Callback connection
                    listenerApp.onConnection(message.getHeader().getSenderAddr(),
                            message.getHeader().getSenderName());
                }
                break;
            }
            default:
                // Handle messages in protocol scope
                listenerDataLink.processMsgReceived(message);
        }
    }

    @Override
    public void stopListening() throws IOException {
        wifiServiceServer.stopListening();
    }

    @Override
    public void getPaired() {
        // Not used in wifi context
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private void sendConnectClient(MessageAdHoc message, NetworkManager networkManager) {
        if (networkManager != null) {
            // Send CONNECT message to establish the pairing
            try {
                networkManager.sendMessage(new MessageAdHoc(
                        new Header(CONNECT_CLIENT, label, ownName), ownIpAddress));

                // Add mapping label - remoteConnection
                mapLabelRemoteDeviceName.put(message.getHeader().getSenderAddr(),
                        message.getHeader().getSenderName());

                // Callback connection
                listenerApp.onConnection(message.getHeader().getSenderAddr(),
                        message.getHeader().getSenderName());

            } catch (IOException e) {
                listenerApp.catchException(e);
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
                                        entry.getValue().deviceName, type));
                    }
                }

                if (discoveryListener != null) {
                    listenerApp.onDiscoveryCompleted(mapAddressDevice);
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

    @Override
    public void enable(int duration) {
        wifiAdHocManager.enable();
    }
}
