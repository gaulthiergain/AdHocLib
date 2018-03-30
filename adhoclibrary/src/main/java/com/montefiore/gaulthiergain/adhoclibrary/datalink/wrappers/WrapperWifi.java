package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.RemoteConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.Neighbors;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvAbstractException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownTypeException;
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

    public WrapperWifi(boolean verbose, Context context, Config config, Neighbors neighbors,
                       HashMap<String, AdHocDevice> mapAddressDevice,
                       final ListenerApp listenerApp, ListenerDataLink listenerDataLink)
            throws IOException {

        super(verbose, context, config.isJson(), config.isBackground(), config.getLabel(),
                mapAddressDevice, neighbors, listenerApp, listenerDataLink);

        try {
            ConnectionListener connectionListener = new ConnectionListener() {
                @Override
                public void onConnectionStarted() {
                    if (v) Log.d(TAG, "Connection Started");
                }

                @Override
                public void onConnectionFailed(int reasonCode) {
                    if (v) Log.d(TAG, "Connection Failed: " + reasonCode);
                    wifiAdHocManager.cancelConnection();
                }

                @Override
                public void onGroupOwner(InetAddress groupOwnerAddress) {
                    ownIpAddress = groupOwnerAddress.getHostAddress();
                    if (v) Log.d(TAG, "onGroupOwner: " + ownIpAddress);
                }

                @Override
                public void onClient(final InetAddress groupOwnerAddress, final InetAddress address) {

                    groupOwnerAddr = groupOwnerAddress.getHostAddress();
                    ownIpAddress = address.getHostAddress();

                    if (v)
                        Log.d(TAG, "onClient groupOwner Address: " + groupOwnerAddress.getHostAddress());
                    if (v) Log.d(TAG, "OWN IP address: " + ownIpAddress);

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
                this.serverPort = config.getServerPort();
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
                this.listenServer(config.getNbThreadWifi());
            } else {
                enabled = false;
            }
        } catch (DeviceException e) {
            enabled = false;
        }
    }

    /*-------------------------------------Override methods---------------------------------------*/

    @Override
    public void connect(AdHocDevice device) {
        wifiAdHocManager.connect(device.getAddress());
    }

    @Override
    public void stopListening() throws IOException {
        wifiServiceServer.stopListening();
    }

    @Override
    public void discovery() {
        wifiAdHocManager.discovery(new DiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {
                if (v) Log.d(TAG, "onDiscoveryStarted");
            }

            @Override
            public void onDiscoveryFailed(int reasonCode) {
                if (v) Log.d(TAG, "onDiscoveryFailed"); //todo exception here
            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, WifiP2pDevice> peerslist) {
                if (v) Log.d(TAG, "onDiscoveryCompleted");

                // Add devices into the peers
                for (Map.Entry<String, WifiP2pDevice> entry : peerslist.entrySet()) {
                    if (!mapAddressDevice.containsKey(entry.getValue().deviceAddress)) {
                        if (v) Log.d(TAG, "Add " + entry.getValue().deviceName + " into peers");
                        mapAddressDevice.put(entry.getValue().deviceAddress,
                                new AdHocDevice(entry.getValue().deviceAddress,
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
    public HashMap<String, AdHocDevice> getPaired() {
        // Not used in wifi context
        return null;
    }

    @Override
    public void unregisterConnection() {
        wifiAdHocManager.unregisterConnection();
    }

    @Override
    public void enable(int duration) {
        wifiAdHocManager.enable();
    }

    @Override
    public void disconnect() {
        //todo add client into list and disconnect it
    }

    @Override
    public void updateName(String name) {
        wifiAdHocManager.updateName(name);
    }

    /*--------------------------------------Public methods----------------------------------------*/

    public void setGroupOwnerValue(int valueGroupOwner) throws GroupOwnerBadValue {

        if (valueGroupOwner < 0 || valueGroupOwner > 15) {
            throw new GroupOwnerBadValue("GroupOwner value must be ");
        }

        wifiAdHocManager.setValueGroupOwner(valueGroupOwner);
    }

    /*--------------------------------------Private methods---------------------------------------*/

    private void _connect() {
        final WifiServiceClient wifiServiceClient = new WifiServiceClient(v, context, json, background,
                groupOwnerAddr, serverPort, 10000, ATTEMPTS, new MessageListener() {
            @Override
            public void onConnectionClosed(RemoteConnection remoteDevice) {

                //Get label from ip
                String remoteLabel = mapLabelAddr.get(remoteDevice.getDeviceAddress());

                if (v) Log.d(TAG, "Client broken with " + remoteLabel);

                try {
                    remoteDevice.setDeviceAddress(remoteLabel);
                    remoteDevice.setDeviceName(mapLabelRemoteDeviceName.get(remoteLabel));
                    neighbors.getNeighbors().remove(remoteLabel);
                    listenerDataLink.brokenLink(remoteLabel);
                } catch (IOException | NoConnectionException e) {
                    listenerApp.catchException(e);
                }

                listenerApp.onConnectionClosed(remoteLabel, remoteDevice.getDeviceName());
            }

            @Override
            public void onConnection(RemoteConnection remoteDevice) {


            }

            @Override
            public void onConnectionFailed(RemoteConnection remoteDevice) {
                listenerApp.onConnectionFailed(remoteDevice.getDeviceName());
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

    private void listenServer(short nbThreads) throws IOException {
        wifiServiceServer = new WifiServiceServer(v, context, json, new MessageListener() {
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
                    neighbors.getNeighbors().remove(remoteLabel);
                    listenerDataLink.brokenLink(remoteLabel);
                } catch (IOException | NoConnectionException e) {
                    listenerApp.catchException(e);
                }

                listenerApp.onConnectionClosed(remoteLabel, remoteDevice.getDeviceName());
            }

            @Override
            public void onConnection(RemoteConnection remoteDevice) {

            }

            @Override
            public void onConnectionFailed(RemoteConnection remoteDevice) {
                listenerApp.onConnectionFailed(remoteDevice.getDeviceName());
            }
        });

        // Start the wifi server listening process
        wifiServiceServer.listen(nbThreads, serverPort);
    }

    private void processMsgReceived(final MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {
        if (v) Log.d(TAG, "Message rcvd " + message.toString());
        switch (message.getHeader().getType()) {
            case CONNECT_SERVER: {
                final NetworkManager networkManager = wifiServiceServer.getActiveConnections().get(message.getPdu().toString());
                if (networkManager != null) {

                    if (v)
                        Log.d(TAG, "Add mapping: " + networkManager.getISocket().getRemoteSocketAddress()
                                + " " + message.getHeader().getSenderAddr());

                    // Add mapping MAC - label
                    mapLabelAddr.put(networkManager.getISocket().getRemoteSocketAddress(),
                            message.getHeader().getSenderAddr());

                }

                // If ownIP address is not known, request it by event
                if (ownIpAddress == null) {
                    wifiAdHocManager.requestGO(new WifiAdHocManager.ListenerWifiGroupOwner() {
                        @Override
                        public void getGroupOwner(String address) {
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

                    if (v)
                        Log.d(TAG, "Add mapping: " + networkManager.getISocket().getRemoteSocketAddress()
                                + " " + message.getHeader().getSenderAddr());

                    // Add mapping MAC - label
                    mapLabelAddr.put(networkManager.getISocket().getRemoteSocketAddress(),
                            message.getHeader().getSenderAddr());

                    // Add mapping label - remoteConnection
                    mapLabelRemoteDeviceName.put(message.getHeader().getSenderAddr(),
                            message.getHeader().getSenderName());

                    if (!neighbors.getNeighbors().containsKey(message.getHeader().getSenderAddr())) {
                        // Callback connection
                        listenerApp.onConnection(message.getHeader().getSenderAddr(),
                                message.getHeader().getSenderName());
                    }

                    // Add the active connection into the autoConnectionActives object
                    neighbors.addNeighbors(message.getHeader().getSenderAddr(),
                            new NetworkObject(type, networkManager));
                }
                break;
            }
            default:
                // Handle messages in protocol scope
                listenerDataLink.processMsgReceived(message);
        }
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

                if (!neighbors.getNeighbors().containsKey(message.getHeader().getSenderAddr())) {
                    // Callback connection
                    listenerApp.onConnection(message.getHeader().getSenderAddr(),
                            message.getHeader().getSenderName());
                }

                // Add the active connection into the autoConnectionActives object
                neighbors.addNeighbors(message.getHeader().getSenderAddr(),
                        new NetworkObject(type, networkManager));

            } catch (IOException e) {
                listenerApp.catchException(e);
            }
        }
    }
}
