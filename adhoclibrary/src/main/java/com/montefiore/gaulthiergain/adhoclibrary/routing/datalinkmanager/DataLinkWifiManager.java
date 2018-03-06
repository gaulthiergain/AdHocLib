package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class DataLinkWifiManager implements IDataLink {


    public static final String ID_APP = "#e091#";
    private static final String TAG = "[AdHoc][DataLinkWifi]";
    private static final int NB_THREAD = 8;
    private final boolean v;
    private final Context context;

    private String ownIpAddress;
    private String ownMacAddress;
    private String groupOwnerAddr;


    private final int serverPort;
    private final ListenerAodv listenerAodv;
    private final ListenerDataLinkAodv listenerDataLinkAodv;

    private final WifiManager wifiManager;

    private final Hashtable<String, WifiP2pDevice> peers;
    private WifiServiceServer wifiServiceServer;
    private ActiveConnections activeConnections;
    private String ownName = "name";

    /**
     * Constructor
     *
     * @param verbose              a boolean value to set the debug/verbose mode.
     * @param context              a Context object which gives global information about an application
     *                             environment.
     * @param listenerAodv         a ListenerAodv object which serves as callback functions.
     * @param listenerDataLinkAodv a ListenerDataLinkAodv object which serves as callback functions.
     */
    public DataLinkWifiManager(boolean verbose, Context context, int serverPort,
                               ListenerAodv listenerAodv, ListenerDataLinkAodv listenerDataLinkAodv)
            throws DeviceException {
        this.v = verbose;
        this.context = context;
        this.serverPort = serverPort;
        this.listenerAodv = listenerAodv;
        this.listenerDataLinkAodv = listenerDataLinkAodv;
        this.wifiManager = new WifiManager(v, context);
        this.activeConnections = new ActiveConnections();
        this.ownMacAddress = wifiManager.getOwnMACAddress().toLowerCase();
        this.peers = new Hashtable<>();
    }


    /**
     * Method allowing to listen for incoming bluetooth connections.
     *
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void listenServer() throws IOException {
        wifiServiceServer = new WifiServiceServer(v, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException e) {
                    listenerAodv.IOException(e);
                } catch (NoConnectionException e) {
                    listenerAodv.NoConnectionException(e);
                } catch (AodvUnknownTypeException e) {
                    listenerAodv.AodvUnknownTypeException(e);
                } catch (AodvUnknownDestException e) {
                    listenerAodv.AodvUnknownDestException(e);
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
            public void onConnectionClosed(String deviceName, String deviceAddress) {

                if (v) Log.d(TAG, "Server broken with " + deviceAddress + " - " + deviceAddress);

                /*try {
                    listenerDataLinkAodv.brokenLink(remoteUuid);
                } catch (IOException e) {
                    listenerAodv.IOException(e);
                } catch (NoConnectionException e) {
                    listenerAodv.NoConnectionException(e);
                }*/
            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                if (v)
                    Log.d(TAG, "Sever connected to client: " + deviceAddress + " - " + deviceName + " - " + localAddress);
            }
        });

        // Start the bluetoothServiceServer listening process
        wifiServiceServer.listen(NB_THREAD, serverPort);
    }

    /**
     * Method allowing to process received messages.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException              Signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException    Signals that a No Connection Exception exception has occurred.
     * @throws AodvUnknownTypeException Signals that a Unknown AODV type has been caught.
     * @throws AodvUnknownDestException Signals that a Unknown route has found.
     */
    private void processMsgReceived(final MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {


        Log.d(TAG, "Message rcvd " + message.toString());
        switch (message.getHeader().getType()) {
            case "CONNECT":
                NetworkObject networkObject = wifiServiceServer.getActiveConnections().get(message.getHeader().getSenderAddr());
                if (networkObject != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkObject);
                }
                break;
            default:
                // Handle messages in protocol scope
                listenerDataLinkAodv.processMsgReceived(message);
        }
    }

    private void _connect() {
        final WifiServiceClient wifiServiceClient = new WifiServiceClient(v, context, true, groupOwnerAddr, serverPort, 10000, 3, new MessageListener() {
            @Override
            public void onConnectionClosed(String deviceName, String deviceAddress) {
                Log.d(TAG, "Client break with server " + deviceAddress + " - " + deviceName);
            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                Log.d(TAG, "Client connected with server " + deviceAddress + " - " + deviceName + " - " + localAddress);
            }

            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException e) {
                    listenerAodv.IOException(e);
                } catch (NoConnectionException e) {
                    listenerAodv.NoConnectionException(e);
                } catch (AodvUnknownTypeException e) {
                    listenerAodv.AodvUnknownTypeException(e);
                } catch (AodvUnknownDestException e) {
                    listenerAodv.AodvUnknownDestException(e);
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
        });

        wifiServiceClient.setListenerAutoConnect(new WifiServiceClient.ListenerAutoConnect() {
            @Override
            public void connected(String remoteAddress, NetworkObject network) throws IOException,
                    NoConnectionException {

                // Add the active connection into the autoConnectionActives object
                activeConnections.addConnection(groupOwnerAddr, network);

                // Send CONNECT message to establish the pairing
                wifiServiceClient.send(new MessageAdHoc(
                        new Header("CONNECT", ownIpAddress, ownName), ownMacAddress));

            }
        });

        // Start the wifiServiceClient thread
        new Thread(wifiServiceClient).start();

    }

    @Override
    public void connect() {
        for (Map.Entry<String, WifiP2pDevice> deviceEntry : peers.entrySet()) {
            Log.d(TAG, "Remote Address" + deviceEntry.getValue().deviceAddress);
            wifiManager.connect(deviceEntry.getValue().deviceAddress, new ConnectionListener() {
                @Override
                public void onConnectionStarted(boolean isGroupOwner) {
                    Log.d(TAG, "Connection Started isGO: " + isGroupOwner);
                }

                @Override
                public void onConnectionFailed(int reasonCode) {
                    Log.d(TAG, "Connection Failed: " + reasonCode);
                }

                @Override
                public void onGroupOwner(InetAddress groupOwnerAddress) {
                    ownIpAddress = groupOwnerAddress.getHostAddress();
                    Log.d(TAG, "onGroupOwner: " + ownIpAddress);
                    try {
                        listenServer();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClient(final InetAddress groupOwnerAddress, final InetAddress address) {

                    groupOwnerAddr = groupOwnerAddress.getHostAddress();
                    ownIpAddress = address.getHostAddress();

                    Log.d(TAG, "onClient groupOwner Address: " + groupOwnerAddress.getHostAddress());
                    Log.d(TAG, "OWN IP address: " + ownIpAddress);

                    _connect();
                }
            });
        }
    }

    @Override
    public void stopListening() throws IOException {
        wifiServiceServer.stopListening();
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
        wifiManager.discover(new DiscoveryListener() {
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
                        peers.put(entry.getValue().deviceAddress, entry.getValue());
                        if (v) Log.d(TAG, "Add no paired " + entry.getValue().deviceAddress
                                + " into hashMapDevices");
                    }
                }
                wifiManager.unregisterDiscovery();
                listenerAodv.onDiscoveryCompleted();
            }
        });
    }

    @Override
    public void getPaired() {
    }

}
