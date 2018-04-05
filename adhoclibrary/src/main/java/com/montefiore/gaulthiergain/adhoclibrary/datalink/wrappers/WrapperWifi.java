package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.RemoteConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceConfig;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvAbstractException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class WrapperWifi extends WrapperConnOriented {

    private static final String TAG = "[AdHoc][WrapperWifi]";

    private int serverPort;
    private String ownIpAddress;
    private String groupOwnerAddr;
    private WifiAdHocManager wifiAdHocManager;

    private HashMap<String, String> mapLabelRemoteDeviceName;

    public WrapperWifi(boolean verbose, Context context, Config config,
                       HashMap<String, AdHocDevice> mapAddressDevice,
                       final ListenerApp listenerApp, ListenerDataLink listenerDataLink)
            throws IOException {

        super(verbose, context, config.isJson(), config.getNbThreadWifi(), config.isBackground(),
                config.getLabel(), mapAddressDevice, listenerApp, listenerDataLink);

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
                        serviceServer.stopListening();
                    } catch (IOException e) {
                        listenerApp.traceException(e);
                    }

                    _connect();
                }
            };
            this.wifiAdHocManager = new WifiAdHocManager(v, context, connectionListener);
            if (wifiAdHocManager.isEnabled()) {
                init(config);
            } else {
                enabled = false;
            }
        } catch (DeviceException e) {
            enabled = false;
        }
    }

    /*-------------------------------------Override methods---------------------------------------*/

    @Override
    public void init(Config config) throws IOException {
        this.ownMac = wifiAdHocManager.getOwnMACAddress().toLowerCase();
        this.serverPort = config.getServerPort();
        this.mapLabelRemoteDeviceName = new HashMap<>();
        this.wifiAdHocManager.getDeviceName(new WifiAdHocManager.ListenerWifiDeviceName() {

            @Override
            public void getDeviceName(String name) {
                // Update ownName
                ownName = name;
                wifiAdHocManager.unregisterInitName();
            }
        });
        this.listenServer();
    }

    @Override
    public void connect(AdHocDevice device) {
        wifiAdHocManager.connect(device.getDeviceAddress());
    }

    @Override
    public void stopListening() throws IOException {
        serviceServer.stopListening();
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
            public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapNameDevice) {
                if (v) Log.d(TAG, "onDiscoveryCompleted");

                // Add devices into the peers
                for (Map.Entry<String, AdHocDevice> entry : mapNameDevice.entrySet()) {

                    WifiAdHocDevice wifiDevice = (WifiAdHocDevice) entry.getValue();
                    if (!mapMacDevice.containsKey(wifiDevice.getDeviceAddress())) {
                        if (v) Log.d(TAG, "Add " + wifiDevice.getDeviceName() + " into peers");
                        mapMacDevice.put(wifiDevice.getDeviceAddress(), wifiDevice);
                    }
                }

                if (discoveryListener != null) {
                    listenerApp.onDiscoveryCompleted(mapMacDevice);
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
    public void enable(int duration, ListenerAdapter listenerAdapter) {
        wifiAdHocManager.enable();
        wifiAdHocManager.onEnableWifi(listenerAdapter);
        enabled = true;
    }

    @Override
    public void disable() {
        wifiAdHocManager.disable();
        enabled = false;
    }


    @Override
    public void unregisterAdapter() {
        // Not used in wifi context
    }

    @Override
    public void resetDeviceName() {
        wifiAdHocManager.resetDeviceName();
    }

    @Override
    public boolean updateDeviceName(String name) {
        return wifiAdHocManager.updateDeviceName(name);
    }


    /*--------------------------------------Public methods----------------------------------------*/

    public void setGroupOwnerValue(int valueGroupOwner) throws GroupOwnerBadValue {

        if (valueGroupOwner < 0 || valueGroupOwner > 15) {
            throw new GroupOwnerBadValue("GroupOwner value must be ");
        }

        wifiAdHocManager.setValueGroupOwner(valueGroupOwner);
    }

    /*--------------------------------------Private methods---------------------------------------*/

    private void listenServer() throws IOException {
        serviceServer = new WifiServiceServer(v, context, json, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException | NoConnectionException | AodvAbstractException e) {
                    listenerApp.traceException(e);
                }
            }

            @Override
            public void catchException(Exception e) {
                listenerApp.traceException(e);
            }

            @Override
            public void onConnectionClosed(RemoteConnection remoteDevice) {
                connectionClosed(remoteDevice);
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
        serviceServer.listen(new ServiceConfig(nbThreads, serverPort));
    }

    private void _connect() {
        final WifiServiceClient wifiServiceClient = new WifiServiceClient(v, context, json, background,
                groupOwnerAddr, serverPort, 10000, ATTEMPTS, new MessageListener() {
            @Override
            public void onConnectionClosed(RemoteConnection remoteDevice) {
                connectionClosed(remoteDevice);
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
                    listenerApp.traceException(e);
                }
            }

            @Override
            public void catchException(Exception e) {
                listenerApp.traceException(e);
            }
        });

        wifiServiceClient.setListenerAutoConnect(new WifiServiceClient.ListenerAutoConnect()

        {
            @Override
            public void connected(String remoteAddress, SocketManager network) throws
                    IOException,
                    NoConnectionException {

                // Add mapping IP - network
                mapAddrNetwork.put(remoteAddress, network);

                // Send CONNECT message to establish the pairing
                wifiServiceClient.send(new MessageAdHoc(
                        new Header(CONNECT_SERVER, label, ownName), ownIpAddress));

            }
        });

        // Start the wifiServiceClient thread
        new Thread(wifiServiceClient).start();
    }

    private void connectionClosed(RemoteConnection remoteDevice) {
        //Get label from ip
        String remoteLabel = mapAddrLabel.get(remoteDevice.getDeviceAddress());
        if (remoteLabel != null) {
            if (v) Log.d(TAG, "Client broken with " + remoteLabel);

            try {

                remoteDevice.setDeviceAddress(remoteLabel);
                remoteDevice.setDeviceName(mapLabelRemoteDeviceName.get(remoteLabel));
                neighbors.getNeighbors().remove(remoteLabel);
                mapLabelRemoteDeviceName.remove(remoteLabel);
                mapAddrLabel.remove(remoteDevice.getDeviceAddress());
                listenerDataLink.brokenLink(remoteLabel);

            } catch (IOException | NoConnectionException e) {
                listenerApp.traceException(e);
            }

            listenerApp.onConnectionClosed(remoteLabel, remoteDevice.getDeviceName());
        } else {
            listenerApp.traceException(new NoConnectionException("Error while closing connection"));
        }
    }

    private void processMsgReceived(final MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {

        if (v) Log.d(TAG, "Message rcvd " + message.toString());

        switch (message.getHeader().getType()) {
            case CONNECT_SERVER: {

                final String remoteLabel = message.getHeader().getSenderAddr();
                String ip = message.getPdu().toString();

                final SocketManager socketManager = serviceServer.getActiveConnections().get(ip);
                if (socketManager != null) {

                    if (v) Log.d(TAG, "Add mapping: " + ip + " " + remoteLabel);

                    // Add mapping MAC - label
                    mapAddrLabel.put(ip, remoteLabel);

                    // If ownIP address is not known, request it by event
                    if (ownIpAddress == null) {
                        wifiAdHocManager.requestGO(new WifiAdHocManager.ListenerWifiGroupOwner() {
                            @Override
                            public void getGroupOwner(String address) {
                                ownIpAddress = address;
                                wifiAdHocManager.unregisterGroupOwner();
                                sendConnectClient(remoteLabel, message.getHeader().getSenderName(),
                                        socketManager);
                            }
                        });
                    } else {
                        sendConnectClient(remoteLabel, message.getHeader().getSenderName(),
                                socketManager);
                    }
                }
                break;
            }
            case CONNECT_CLIENT: {

                String remoteLabel = message.getHeader().getSenderAddr();
                String ip = message.getPdu().toString();

                SocketManager socketManager = mapAddrNetwork.get(ip);
                if (socketManager != null) {

                    if (v) Log.d(TAG, "Add mapping: " + ip + " " + label);

                    // Add mapping MAC - label
                    mapAddrLabel.put(ip, remoteLabel);

                    // Add mapping label - remoteConnection
                    mapLabelRemoteDeviceName.put(remoteLabel, message.getHeader().getSenderName());

                    if (!neighbors.getNeighbors().containsKey(remoteLabel)) {
                        // Callback connection
                        listenerApp.onConnection(remoteLabel, message.getHeader().getSenderName(), 0);
                    }

                    // Add the active connection into the autoConnectionActives object
                    neighbors.addNeighbors(remoteLabel, socketManager);
                }
                break;
            }
            default:
                // Handle messages in protocol scope
                listenerDataLink.processMsgReceived(message);
        }
    }

    private void sendConnectClient(String remoteLabel, String name, SocketManager socketManager) {


        // Send CONNECT message to establish the pairing
        try {
            socketManager.sendMessage(new MessageAdHoc(
                    new Header(CONNECT_CLIENT, label, ownName), ownIpAddress));

            // Add mapping label - remoteConnection
            mapLabelRemoteDeviceName.put(remoteLabel, name);

            if (!neighbors.getNeighbors().containsKey(remoteLabel)) {
                // Callback connection
                listenerApp.onConnection(remoteLabel, name, 0);
            }

            // Add the active connection into the autoConnectionActives object
            neighbors.addNeighbors(remoteLabel, socketManager);

        } catch (IOException e) {
            listenerApp.traceException(e);
        }

    }
}
