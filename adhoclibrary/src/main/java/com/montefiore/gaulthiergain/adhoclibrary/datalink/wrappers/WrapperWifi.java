package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAction;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceMessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceConfig;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionWifiListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;

public class WrapperWifi extends WrapperConnOriented implements IWrapperWifi {

    private static final String TAG = "[AdHoc][WrapperWifi]";

    private int serverPort;
    private String ownIpAddress;
    private String groupOwnerAddr;
    private WifiAdHocManager wifiAdHocManager;
    private HashMap<String, String> mapAddrMac;

    public WrapperWifi(boolean verbose, Context context, Config config,
                       HashMap<String, AdHocDevice> mapAddressDevice,
                       final ListenerApp listenerApp, final ListenerDataLink listenerDataLink)
            throws IOException {

        super(verbose, config, config.getNbThreadWifi(), mapAddressDevice,
                listenerApp, listenerDataLink);

        try {
            this.type = Service.WIFI;
            this.wifiAdHocManager = new WifiAdHocManager(v, context, new WifiAdHocManager.WifiDeviceInfosListener() {
                @Override
                public void getDeviceInfos(String name, String mac) {
                    ownName = name;
                    ownMac = mac;
                    Log.d(TAG, "MAC: " + mac + " - Name: " + ownName);
                    listenerDataLink.initInfos(ownMac, ownName);
                }
            }, initConnectionListener());
            if (wifiAdHocManager.isEnabled()) {
                init(config, context);
            } else {
                enabled = false;
            }
        } catch (DeviceException e) {
            enabled = false;
        }
    }

    /*-------------------------------------Override methods---------------------------------------*/

    @Override
    public void init(Config config, Context context) throws IOException {
        this.mapAddrMac = new HashMap<>();
        this.serverPort = config.getServerPort();
        this.listenServer();
    }

    @Override
    public void connect(AdHocDevice device) {
        wifiAdHocManager.connect(device.getMacAddress());
    }

    @Override
    public void stopListening() throws IOException {
        serviceServer.stopListening();
    }

    @Override
    public void discovery(final DiscoveryListener discoveryListener) {
        wifiAdHocManager.discovery(new DiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {
                discoveryListener.onDiscoveryStarted();
            }

            @Override
            public void onDiscoveryFailed(Exception e) {
                discoveryListener.onDiscoveryFailed(e);
            }

            @Override
            public void onDeviceDiscovered(AdHocDevice device) {
                if (!mapMacDevices.containsKey(device.getMacAddress())) {
                    if (v)
                        Log.d(TAG, "Add " + device.getMacAddress() + " into mapMacDevices");
                    mapMacDevices.put(device.getMacAddress(), device);
                }

                discoveryListener.onDeviceDiscovered(device);
            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapNameDevice) {

                //todo refactor this

                // Add device into mapMacDevices
                for (AdHocDevice device : mapNameDevice.values()) {
                    if (!mapMacDevices.containsKey(device.getMacAddress())) {
                        if (v)
                            Log.d(TAG, "Add " + device.getMacAddress() + " into mapMacDevices");
                        mapMacDevices.put(device.getMacAddress(), device);
                    }
                }

                if (listenerBothDiscovery != null) {
                    discoveryListener.onDiscoveryCompleted(mapMacDevices);
                }

                discoveryCompleted = true;

                // Stop and unregister to the discovery process
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
    public void updateContext(Context context) {
        wifiAdHocManager.updateContext(context);
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

    @Override
    public String getAdapterName() {
        return wifiAdHocManager.getDeviceName();
    }

    /*--------------------------------------IWifi methods----------------------------------------*/

    @Override
    public void setGroupOwnerValue(int valueGroupOwner) throws GroupOwnerBadValue {

        if (valueGroupOwner < 0 || valueGroupOwner > 15) {
            throw new GroupOwnerBadValue("GroupOwner value must be between 0 and 15");
        }

        wifiAdHocManager.setValueGroupOwner(valueGroupOwner);
    }

    @Override
    public void removeGroup(ListenerAction listenerAction) {
        wifiAdHocManager.removeGroup(listenerAction);
    }

    @Override
    public void cancelConnect(ListenerAction listenerAction) {
        wifiAdHocManager.cancelConnection(listenerAction);
    }

    /*--------------------------------------Private methods---------------------------------------*/

    private void listenServer() throws IOException {
        serviceServer = new WifiServiceServer(v, json, new ServiceMessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException e) {
                    listenerApp.processMsgException(e);
                }
            }

            @Override
            public void onConnectionClosed(String remoteAddress) {
                try {
                    // Get MAC address from IP address
                    connectionClosed(mapAddrMac.get(remoteAddress));
                    mapAddrMac.remove(remoteAddress);
                } catch (IOException | NoConnectionException e) {
                    listenerApp.onConnectionClosedFailed(e);
                }
            }

            @Override
            public void onConnection(String remoteAddress) {

            }

            @Override
            public void onConnectionFailed(Exception e) {
                listenerApp.onConnectionFailed(e);
            }

            @Override
            public void onMsgException(Exception e) {
                listenerApp.processMsgException(e);
            }

        });

        // Start the wifi server listening process
        serviceServer.listen(new ServiceConfig(nbThreads, serverPort));
    }

    private void _connect() {
        final WifiServiceClient wifiServiceClient = new WifiServiceClient(v, json, background,
                groupOwnerAddr, serverPort, 10000, attemps, new ServiceMessageListener() {
            @Override
            public void onConnectionClosed(String remoteAddress) {
                try {
                    // Get MAC address from IP address
                    connectionClosed(mapAddrMac.get(remoteAddress));
                    mapAddrMac.remove(remoteAddress);
                } catch (IOException | NoConnectionException e) {
                    listenerApp.onConnectionClosedFailed(e);
                }
            }

            @Override
            public void onConnection(String remoteAddress) {
                //ignored
            }

            @Override
            public void onConnectionFailed(Exception e) {
                listenerApp.onConnectionFailed(e);
            }

            @Override
            public void onMsgException(Exception e) {
                listenerApp.processMsgException(e);
            }

            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException e) {
                    listenerApp.processMsgException(e);
                }
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
                        new Header(CONNECT_SERVER, ownIpAddress, ownMac, label, ownName), remoteAddress));
            }
        });

        // Start the wifiServiceClient thread
        new Thread(wifiServiceClient).start();
    }

    private void processMsgReceived(final MessageAdHoc message) throws IOException {

        switch (message.getHeader().getType()) {
            case CONNECT_SERVER: {

                final SocketManager socketManager = serviceServer.getActiveConnections().get(
                        message.getHeader().getAddress());
                if (socketManager != null) {

                    // Add mapping IP - MAC
                    mapAddrMac.put(message.getHeader().getAddress(), message.getHeader().getMac());

                    // If ownIP address is not known, request it by event
                    if (ownIpAddress == null) {
                        // Get IP from remote pair
                        ownIpAddress = (String) message.getPdu();

                        // Send CONNECT message to establish the pairing
                        socketManager.sendMessage(new MessageAdHoc(
                                new Header(CONNECT_CLIENT, ownIpAddress, ownMac, label, ownName)));

                        receivedPeerMsg(message.getHeader(), socketManager);
                    } else {
                        // Send CONNECT message to establish the pairing
                        socketManager.sendMessage(new MessageAdHoc(
                                new Header(CONNECT_CLIENT, ownIpAddress, ownMac, label, ownName)));

                        receivedPeerMsg(message.getHeader(), socketManager);
                    }
                }
                break;
            }
            case CONNECT_CLIENT: {

                // Get socketManager from IP
                SocketManager socketManager = mapAddrNetwork.get(message.getHeader().getAddress());
                if (socketManager != null) {

                    // Add mapping IP - MAC
                    mapAddrMac.put(message.getHeader().getAddress(), message.getHeader().getMac());

                    receivedPeerMsg(message.getHeader(), socketManager);
                }
                break;
            }
            case CONNECT_BROADCAST: {
                if (checkFloodEvent(message)) {

                    // Get Messsage Header
                    Header header = message.getHeader();

                    // Remote connection happens in other node
                    listenerApp.onConnection(new AdHocDevice(header.getLabel(), header.getMac(),
                            header.getName(), type, false));
                }

                break;
            }
            case DISCONNECT_BROADCAST: {
                if (checkFloodEvent(message)) {

                    // Get Messsage Header
                    Header header = message.getHeader();

                    // Remote connection is closed in other node
                    listenerApp.onConnectionClosed(new AdHocDevice(header.getLabel(), header.getMac(),
                            header.getName(), type, false));
                }
                break;
            }
            case BROADCAST: {
                // Get Messsage Header
                Header header = message.getHeader();

                listenerApp.onReceivedData(new AdHocDevice(header.getLabel(), header.getMac(),
                        header.getName(), type), message.getPdu());
                break;
            }
            default:
                // Handle messages in protocol scope
                listenerDataLink.processMsgReceived(message);
        }
    }

    private ConnectionWifiListener initConnectionListener() {
        return new ConnectionWifiListener() {
            @Override
            public void onConnectionStarted() {
                if (v) Log.d(TAG, "Connection Started");
            }

            @Override
            public void onConnectionFailed(Exception e) {
                listenerApp.onConnectionFailed(e);
            }

            @Override
            public void onGroupOwner(InetAddress groupOwnerAddress) {
                ownIpAddress = groupOwnerAddress.getHostAddress();
                if (v) Log.d(TAG, "GroupOwner IP: " + ownIpAddress);
            }

            @Override
            public void onClient(final InetAddress groupOwnerAddress, final InetAddress address) throws IOException {

                groupOwnerAddr = groupOwnerAddress.getHostAddress();
                ownIpAddress = address.getHostAddress();

                if (v) Log.d(TAG, "GroupOwner IP: " + groupOwnerAddress.getHostAddress());
                if (v) Log.d(TAG, "Own IP: " + ownIpAddress);

                serviceServer.stopListening();

                _connect();
            }
        };
    }


}
