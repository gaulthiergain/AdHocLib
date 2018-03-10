package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.AbstractRemoteConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.RemoteWifiConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
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
import java.util.Map;

public class WrapperWifi extends AbstractWrapper {

    private static final String TAG = "[AdHoc][WrapperWifi]";

    int serverPort;
    String ownIpAddress;
    String groupOwnerAddr;
    WifiAdHocManager wifiAdHocManager;
    WifiServiceServer wifiServiceServer;

    public WrapperWifi(boolean v, Context context, boolean enable, short nbThreads, int serverPort,
                       ActiveConnections activeConnections, final ListenerAodv listenerAodv,
                       final ListenerDataLinkAodv listenerDataLinkAodv)
            throws DeviceException, IOException {
        super(v, context, activeConnections, listenerAodv, listenerDataLinkAodv);

        // Enable wifi adapter
        this.wifiAdHocManager = new WifiAdHocManager(v, context, new ConnectionListener() {
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

                listenerDataLinkAodv.getDeviceAddress(ownIpAddress);
            }

            @Override
            public void onClient(final InetAddress groupOwnerAddress, final InetAddress address) {

                groupOwnerAddr = groupOwnerAddress.getHostAddress();
                ownIpAddress = address.getHostAddress();

                listenerDataLinkAodv.getDeviceAddress(ownIpAddress);

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
        this.ownMac = wifiAdHocManager.getOwnMACAddress().toLowerCase();
        this.serverPort = serverPort;

        this.wifiAdHocManager.getDeviceName(new WifiAdHocManager.ListenerWifiDeviceName() {

            @Override
            public void getDeviceName(String name) {
                // Update ownName
                ownName = name;
                listenerDataLinkAodv.getDeviceName(ownName);
                wifiAdHocManager.unregisterInitName();
            }
        });
        this.listenServer(nbThreads);

    }

    WrapperWifi(boolean v, Context context,
                ActiveConnections activeConnections, final ListenerAodv listenerAodv,
                final ListenerDataLinkAodv listenerDataLinkAodv) throws DeviceException, IOException {
        super(v, context, activeConnections, listenerAodv, listenerDataLinkAodv);
    }

    protected void listenServer(short nbThreads) throws IOException {
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

                RemoteWifiConnection remoteWifiDevice = (RemoteWifiConnection) remoteDevice;

                if (v) Log.d(TAG, "Server broken with " + remoteWifiDevice.getDeviceAddress());

                try {
                    listenerDataLinkAodv.brokenLink(remoteDevice.getDeviceAddress());
                } catch (IOException | NoConnectionException e) {
                    listenerAodv.catchException(e);
                }

                listenerAodv.onConnectionClosed(remoteDevice);
            }

            @Override
            public void onConnection(AbstractRemoteConnection remoteDevice) {
                listenerAodv.onConnection(remoteDevice);
            }
        });

        // Start the  wifi server listening process
        wifiServiceServer.listen(nbThreads, serverPort);
    }

    private void _connect() {
        final WifiServiceClient wifiServiceClient = new WifiServiceClient(v, context, true, groupOwnerAddr, serverPort,
                10000, ATTEMPTS, new MessageListener() {
            @Override
            public void onConnectionClosed(AbstractRemoteConnection remoteDevice) {
                RemoteWifiConnection remoteWifiDevice = (RemoteWifiConnection) remoteDevice;

                if (v) Log.d(TAG, "Client broken with " + remoteWifiDevice.getDeviceAddress());

                try {
                    listenerDataLinkAodv.brokenLink(remoteDevice.getDeviceAddress());
                } catch (IOException | NoConnectionException e) {
                    listenerAodv.catchException(e);
                }

                listenerAodv.onConnectionClosed(remoteDevice);
            }

            @Override
            public void onConnection(AbstractRemoteConnection remoteDevice) {
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

                // Add the active connection into the autoConnectionActives object
                activeConnections.addConnection(groupOwnerAddr, network);

                // Send CONNECT message to establish the pairing
                wifiServiceClient.send(new MessageAdHoc(
                        new Header("CONNECT", ownIpAddress, ownName), ownMac));

            }
        });

        // Start the wifiServiceClient thread
        new Thread(wifiServiceClient).start();

    }


    @Override
    public void stopListening() throws IOException {
        wifiServiceServer.stopListening();
    }

    @Override
    public void getPaired() {

    }

    @Override
    public void connect(DiscoveredDevice device) {
        Log.d(TAG, "Remote Address" + device.getAddress());
        wifiAdHocManager.connect(device.getAddress());
    }

    @Override
    public void discovery() {
        wifiAdHocManager.discovery(new DiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {

            }

            @Override
            public void onDiscoveryFailed(int reasonCode) {

            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, WifiP2pDevice> peerslist) {

                HashMap<String, DiscoveredDevice> mapAddressDevice = new HashMap<>();

                // Add no paired devices into the hashMapDevices
                for (Map.Entry<String, WifiP2pDevice> entry : peerslist.entrySet()) {
                    if (!mapAddressDevice.containsKey(entry.getValue().deviceAddress)) {
                        if (v) Log.d(TAG, "Add no paired " + entry.getValue().deviceAddress
                                + " into hashMapDevices");

                        mapAddressDevice.put(entry.getValue().deviceAddress,
                                new DiscoveredDevice(entry.getValue().deviceAddress,
                                        entry.getValue().deviceName, DiscoveredDevice.WIFI));
                    }
                }

                wifiAdHocManager.unregisterDiscovery();
                listenerAodv.onDiscoveryCompleted(mapAddressDevice);
            }
        });
    }

    @Override
    void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException, AodvUnknownTypeException, AodvUnknownDestException {
        Log.d(TAG, "Message rcvd " + message.toString());
        switch (message.getHeader().getType()) {
            case "CONNECT":
                NetworkObject networkObject = wifiServiceServer.getActiveConnections().get(message.getHeader().getSenderAddr());
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
                            listenerDataLinkAodv.getDeviceAddress(address);
                            wifiAdHocManager.unregisterGroupOwner();
                        }
                    });
                }

                break;
            default:
                // Handle messages in protocol scope
                listenerDataLinkAodv.processMsgReceived(message);
        }
    }

    public void unregisterConnection() {
        wifiAdHocManager.unregisterConnection();
    }
}
