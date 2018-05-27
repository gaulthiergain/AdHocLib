package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

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
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceConfig;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceMessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionWifiListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager.ServiceDiscoverListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiServer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * <p>This class represents a wrapper and manages all communications related to TCP for Wi-FI.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
class WrapperWifi extends WrapperConnOriented implements IWrapperWifi {

    private static final String TAG = "[AdHoc][WrapperWifi]";

    private int serverPort;
    private String ownIpAddress;
    private String groupOwnerAddr;
    private boolean isGroupOwner;
    private WifiAdHocManager wifiAdHocManager;
    private HashMap<String, String> mapAddrMac;

    /**
     * Constructor
     *
     * @param verbose          a boolean value to set the debug/verbose mode.
     * @param context          a Context object which gives global information about an application
     *                         environment.
     * @param config           a Config object which contains specific configurations.
     * @param mapAddressDevice a HashMap<String, AdHocDevice> which maps a UUID address entry to an
     *                         AdHocDevice object.
     * @param listenerApp      a ListenerApp object which contains callback functions.
     * @param listenerDataLink a ListenerDataLink object which contains callback functions.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    WrapperWifi(boolean verbose, Context context, Config config,
                HashMap<String, AdHocDevice> mapAddressDevice,
                final ListenerApp listenerApp, final ListenerDataLink listenerDataLink)
            throws IOException {

        super(verbose, config, config.getNbThreadWifi(), mapAddressDevice,
                listenerApp, listenerDataLink);

        this.type = Service.WIFI;
        if (WifiAdHocManager.isWifiEnabled(context)) {
            this.wifiAdHocManager = new WifiAdHocManager(v, context, config.getServerPort(), initConnectionListener(),
                    new WifiAdHocManager.WifiDeviceInfosListener() {
                        @Override
                        public void getDeviceInfos(String name, String mac) {
                            ownName = name;
                            ownMac = mac;
                            listenerDataLink.initInfos(ownMac, ownName);
                        }
                    });
            this.init(config, context);
        } else {
            this.enabled = false;
        }
    }

    /*-------------------------------------Override methods---------------------------------------*/

    /**
     * Method allowing to initialize internal parameters.
     *
     * @param config  a Config object which contains specific configurations.
     * @param context a Context object which gives global information about an application
     *                environment.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    @Override
    void init(Config config, Context context) throws IOException {
        this.isGroupOwner = false;
        this.mapAddrMac = new HashMap<>();
        this.serverPort = config.getServerPort();
        this.listenServer();
    }

    /**
     * Method allowing to get a remote IP from a MAC address.
     *
     * @param mac a String value which represents the MAC address of a remote device.
     * @return a String value which represents the IP address of a remote device.
     */
    private String getIpByMac(String mac) {
        for (Map.Entry<String, String> entry : mapAddrMac.entrySet()) {
            if (mac.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Method allowing to connect to a remote peer.
     *
     * @param attempts    an integer value which represents the number of attempts to try to connect
     *                    to the remote peer.
     * @param adHocDevice an AdHocDevice object which represents the remote peer.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    @Override
    void connect(short attempts, AdHocDevice adHocDevice) throws DeviceException {

        String ip = getIpByMac(adHocDevice.getMacAddress());
        if (ip == null) {
            this.attempts = attempts;
            wifiAdHocManager.connect(adHocDevice.getMacAddress());
        } else {
            if (!serviceServer.getActiveConnections().containsKey(ip)) {
                this.attempts = attempts;
                wifiAdHocManager.connect(adHocDevice.getMacAddress());
            } else {
                throw new DeviceException(adHocDevice.getDeviceName()
                        + "(" + adHocDevice.getMacAddress() + ") is already connected");
            }
        }
    }

    /**
     * Method allowing to stop a listening on incoming connections.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    @Override
    void stopListening() throws IOException {
        serviceServer.stopListening();
    }

    /**
     * Method allowing to perform a discovery depending the technology used. If the Bluetooth and
     * Wi-Fi is enabled, the two discoveries are performed in parallel. A discovery stands for at
     * least 10/12 seconds.
     *
     * @param discoveryListener a DiscoveryListener object which contains callback function.
     */
    @Override
    void discovery(final DiscoveryListener discoveryListener) {
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

                if (wifiAdHocManager == null) {
                    discoveryListener.onDiscoveryFailed(
                            new DeviceException("Unable to complete the discovery due to wifi connectivity"));
                } else {
                    // Add device into mapMacDevices
                    for (AdHocDevice device : mapNameDevice.values()) {
                        if (!mapMacDevices.containsKey(device.getMacAddress())) {
                            if (v)
                                Log.d(TAG, "Add " + device.getMacAddress() + " into mapMacDevices");
                            mapMacDevices.put(device.getMacAddress(), device);
                        }
                    }

                    wifiAdHocManager.unregisterDiscovery();

                    if (listenerBothDiscovery != null) {
                        discoveryListener.onDiscoveryCompleted(mapMacDevices);
                    }

                    discoveryCompleted = true;


                }
            }
        });
    }

    /**
     * Method allowing to get all the Bluetooth devices which are already paired. It is not used
     * in this context.
     *
     * @return a HashMap<String, AdHocDevice> object which contains all paired Bluetooth devices.
     */
    @Override
    HashMap<String, AdHocDevice> getPaired() {
        // Not used in wifi context
        return null;
    }

    /**
     * Method allowing to unregister broadcast receivers for Bluetooth and Wi-FI adapters.
     */
    @Override
    void unregisterConnection() {
        wifiAdHocManager.unregisterConnection();
    }

    /**
     * Method allowing to enabled a particular technology.
     *
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param duration        an integer value which is used to set up the time of the bluetooth
     *                        discovery. It is not used in this context.
     * @param listenerAdapter a ListenerAdapter object which contains callback functions.
     */
    @Override
    void enable(Context context, int duration, ListenerAdapter listenerAdapter) {
        this.wifiAdHocManager = new WifiAdHocManager(v, context, serverPort, initConnectionListener(),
                new WifiAdHocManager.WifiDeviceInfosListener() {
                    @Override
                    public void getDeviceInfos(String name, String mac) {
                        ownName = name;
                        ownMac = mac;
                        listenerDataLink.initInfos(ownMac, ownName);
                    }
                });
        wifiAdHocManager.enable();
        wifiAdHocManager.onEnableWifi(listenerAdapter);
        enabled = true;
    }

    /**
     * Method allowing to disabled a particular technology.
     */
    @Override
    void disable() {
        // Clear data structure if adapter is disabled
        mapAddrMac.clear();
        mapAddrNetwork.clear();
        neighbors.clear();

        wifiAdHocManager.disable();
        wifiAdHocManager = null;
        enabled = false;
    }

    /**
     * Method allowing to update the current context.
     *
     * @param context a Context object which gives global information about an application
     *                environment.
     */
    @Override
    void updateContext(Context context) {
        wifiAdHocManager.updateContext(context);
    }

    /**
     * Method allowing to unregister broadcast receivers for Bluetooth and Wi-FI adapters.
     */
    @Override
    void unregisterAdapter() {
        // Not used in wifi context
    }

    /**
     * Method allowing to reset the adapter name of a particular technology.
     */
    @Override
    void resetDeviceName() {
        wifiAdHocManager.resetDeviceName();
    }

    /**
     * Method allowing to update the name of a particular technology.
     *
     * @param name a String value which represents the new name of the device adapter.
     * @return a boolean value which is true if the name was correctly updated. Otherwise, false.
     */
    @Override
    boolean updateDeviceName(String name) {
        return wifiAdHocManager.updateDeviceName(name);
    }

    /**
     * Method allowing to get a particular adapter name.
     *
     * @return a String which represents the name of a particular adapter name.
     */
    @Override
    String getAdapterName() {
        return wifiAdHocManager.getAdapterName();
    }

    /*--------------------------------------IWifi methods----------------------------------------*/

    /**
     * Method allowing to update the Group Owner value to influence the choice of the Group Owner
     * negotiation.
     *
     * @param valueGroupOwner an integer value between 0 and 15 where 0 indicates the least
     *                        inclination to be a group owner and 15 indicates the highest inclination
     *                        to be a group owner. A value of -1 indicates the system can choose
     *                        an appropriate value.
     * @throws GroupOwnerBadValue signals that the value for the Group Owner intent is invalid.
     */
    @Override
    public void setGroupOwnerValue(int valueGroupOwner) throws GroupOwnerBadValue {

        if (valueGroupOwner < 0 || valueGroupOwner > 15) {
            throw new GroupOwnerBadValue("GroupOwner value must be between 0 and 15");
        }

        wifiAdHocManager.setValueGroupOwner(valueGroupOwner);
    }

    /**
     * Method allowing to remove a current Wi-Fi group.
     *
     * @param listenerAction a ListenerAction object which contains callback functions.
     */
    @Override
    public void removeGroup(ListenerAction listenerAction) {
        wifiAdHocManager.removeGroup(listenerAction);
    }

    /**
     * Method allowing to cancel a Wi-Fi connection (during the Group Owner negotiation).
     *
     * @param listenerAction a ListenerAction object which contains callback functions.
     */
    @Override
    public void cancelConnect(ListenerAction listenerAction) {
        wifiAdHocManager.cancelConnection(listenerAction);
    }

    /**
     * Method allowing to check if the current device is the Group Owner.
     *
     * @return a boolean value which is true if the current device is the Group Owner. Otherwise, false.
     */
    @Override
    public boolean isWifiGroupOwner() {
        return isGroupOwner;
    }

    /*--------------------------------------Private methods---------------------------------------*/

    /**
     * Method allowing to launch a server to handle incoming connections in background.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    private void listenServer() throws IOException {
        serviceServer = new WifiServer(v, json, new ServiceMessageListener() {
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

    /**
     * Method allowing to connect to a remote node/
     *
     * @param remotePort an integer value which represents the port of the remote peer.
     */
    private void _connect(int remotePort) {
        final WifiClient wifiClient = new WifiClient(v, json,
                groupOwnerAddr, remotePort, timeout, attempts, new ServiceMessageListener() {
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

        wifiClient.setListenerAutoConnect(new WifiClient.ListenerAutoConnect()

        {
            @Override
            public void connected(String remoteAddress, SocketManager network) throws
                    IOException,
                    NoConnectionException {

                // Add mapping IP - network
                mapAddrNetwork.put(remoteAddress, network);

                // Send CONNECT message to establish the pairing
                wifiClient.send(new MessageAdHoc(
                        new Header(CONNECT_SERVER, ownIpAddress, ownMac, label, ownName), remoteAddress));
            }
        });

        // Start the wifiClient thread
        new Thread(wifiClient).start();
    }

    /**
     * Method allowing to process messages from remote nodes.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
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
                if (checkFloodEvent(((FloodMsg) message.getPdu()).getId())) {

                    // Re-broadcast message
                    broadcastExcept(message, message.getHeader().getLabel());

                    // Get Message info
                    HashSet<AdHocDevice> list = ((FloodMsg) message.getPdu()).getAdHocDevices();

                    // Remote connection(s) happen(s) in other node(s)
                    for (AdHocDevice adHocDevice : list) {
                        if (!adHocDevice.getLabel().equals(label) && !setRemoteDevices.contains(adHocDevice)
                                && !isDirectNeighbors(adHocDevice.getLabel())) {

                            adHocDevice.setDirectedConnected(false);

                            listenerApp.onConnection(adHocDevice);

                            // Update set
                            setRemoteDevices.add(adHocDevice);
                        }
                    }
                }

                break;
            }
            case DISCONNECT_BROADCAST: {
                if (checkFloodEvent((String) message.getPdu())) {

                    // Re-broadcast message
                    broadcastExcept(message, message.getHeader().getLabel());

                    // Get Message Header
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

    /**
     * Method allowing to initialize the ConnectionWifiListener object.
     *
     * @return a ConnectionWifiListener object which contains callback functions.
     */
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
                isGroupOwner = true;
                if (v) Log.d(TAG, "onGroupOwner-> own IP: " + ownIpAddress);

                wifiAdHocManager.startRegistration();
            }

            @Override
            public void onClient(final InetAddress groupOwnerAddress, final InetAddress address) throws IOException {

                groupOwnerAddr = groupOwnerAddress.getHostAddress();
                ownIpAddress = address.getHostAddress();
                isGroupOwner = false;
                if (v)
                    Log.d(TAG, "onClient-> GroupOwner IP: " + groupOwnerAddress.getHostAddress());
                if (v) Log.d(TAG, "onClient-> own IP: " + ownIpAddress);

                serviceServer.stopListening();

                // If attempts is not defined set to 1
                if (attempts == 0) {
                    attempts = 3;
                }

                wifiAdHocManager.discoverService(new ServiceDiscoverListener() {
                    @Override
                    public void onServiceCompleted(int port) {
                        if (v) Log.d(TAG, "Remote port is " + port);
                        _connect(port);
                    }
                });

            }
        };
    }
}
