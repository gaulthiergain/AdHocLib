package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

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


public class WrapperHybridWifi extends WrapperWifi {

    private static final String TAG = "[AdHoc][WrapperWifiHy]";

    private String label;
    private HashMap<String, String> mapLabelMac;
    private HashMap<String, DiscoveredDevice> mapAddressDevice;
    private Hashtable<String, NetworkObject> mapIpNetwork;
    private ListenerConnection listenerConnection;
    private boolean finishDiscovery = false;


    public WrapperHybridWifi(boolean v, Context context, short nbThreads, int serverPort,
                             String label, ActiveConnections activeConnections,
                             HashMap<String, DiscoveredDevice> mapAddressDevice,
                             final ListenerAodv listenerAodv, ListenerDataLinkAodv listenerDataLinkAodv)
            throws DeviceException, IOException {
        super(v, context, activeConnections, listenerAodv, listenerDataLinkAodv);

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
        });
        if (wifiAdHocManager.isEnabled()) {
            init(serverPort);
            this.label = label;
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
        wifiServiceServer.listen(nbThreads, serverPort);
    }

    private void _connect() {
        final WifiServiceClient wifiServiceClient = new WifiServiceClient(v, context, true,
                groupOwnerAddr, serverPort, 10000, ATTEMPTS, new MessageListener() {
            @Override
            public void onConnectionClosed(AbstractRemoteDevice remoteDevice) {
                listenerAodv.onConnectionClosed(remoteDevice);
                //TODO update aodv code with good ip
            }

            @Override
            public void onConnection(AbstractRemoteDevice remoteDevice) {
                //TODO update aodv code with good ip
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

    public void connect() {
        for (Map.Entry<String, WifiP2pDevice> deviceEntry : peers.entrySet()) {
            Log.d(TAG, "Remote Address" + deviceEntry.getValue().deviceAddress);
            wifiAdHocManager.connect(deviceEntry.getValue().deviceAddress);
        }
    }

    public void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {
        Log.d(TAG, "Message rcvd " + message.toString());
        switch (message.getHeader().getType()) {
            case "CONNECT_SERVER":

                final NetworkObject networkObject = wifiServiceServer.getActiveConnections().get(message.getPdu().toString());
                if (networkObject != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkObject);

                    Log.d(TAG, "Add couple: " + networkObject.getISocket().getRemoteSocketAddress()
                            + " " + message.getHeader().getSenderAddr());

                    // Add mapping MAC - label
                    mapLabelMac.put(networkObject.getISocket().getRemoteSocketAddress(),
                            message.getHeader().getSenderAddr());
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
                                            new Header("CONNECT_CLIENT", label, ownName), ownIpAddress));

                                    if (listenerConnection != null) {
                                        listenerConnection.onConnect();
                                    }

                                } catch (IOException e) {
                                    listenerAodv.catchException(e);
                                }
                            }
                        }
                    });
                }
                break;
            case "CONNECT_CLIENT":
                NetworkObject networkObjectServer = mapIpNetwork.get(message.getPdu().toString());
                if (networkObjectServer != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkObjectServer);

                    Log.d(TAG, "Add couple: " + networkObjectServer.getISocket().getRemoteSocketAddress()
                            + " " + message.getHeader().getSenderAddr());

                    // Add mapping MAC - label
                    mapLabelMac.put(networkObjectServer.getISocket().getRemoteSocketAddress(),
                            message.getHeader().getSenderAddr());

                    if (listenerConnection != null) {
                        listenerConnection.onConnect();
                    }
                }

                break;
            default:
                // Handle messages in protocol scope
                listenerDataLinkAodv.processMsgReceived(message);
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
                    if (!peers.containsKey(entry.getValue().deviceAddress)) {
                        peers.put(entry.getValue().deviceAddress, entry.getValue());
                        if (v) Log.d(TAG, "Add " + entry.getValue().deviceName + " into peers");
                        mapAddressDevice.put(entry.getValue().deviceAddress,
                                new DiscoveredDevice(entry.getValue().deviceAddress,
                                        entry.getValue().deviceName, DiscoveredDevice.WIFI));
                    }
                }

                finishDiscovery = true;

                wifiAdHocManager.unregisterDiscovery();
            }
        });
    }

    public void updateName(String name) {
        if (isWifiEnabled()) {
            wifiAdHocManager.updateName(name);
        }
    }

    public void setListenerConnection(ListenerConnection listenerConnection) {
        this.listenerConnection = listenerConnection;
    }

    public interface ListenerConnection {
        void onConnect();
    }

    public boolean isFinishDiscovery() {
        return finishDiscovery;
    }

    public void setFinishDiscovery(boolean finishDiscovery) {
        this.finishDiscovery = finishDiscovery;
    }
}
