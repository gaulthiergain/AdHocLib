package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAction;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceMessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.UdpPDU;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.UdpPeers;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionWifiListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.Constants;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.TypeAodv;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>This class represents a wrapper and manages all communications related to UDP for Wi-FI.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
class WrapperWifiUdp extends AbstractWrapper implements IWrapperWifi {

    private static final String TAG = "[AdHoc][WrapperWifiUdp]";
    private static final int TIMER_ACK = 2000;

    private int serverPort;
    private int remotePort;
    private UdpPeers udpPeers;
    private String ownIpAddress;
    private boolean isGroupOwner;
    private HashSet<String> ackSet;
    private WifiAdHocManager wifiAdHocManager;
    private HashMap<String, Long> helloMessages;
    private HashMap<String, AdHocDevice> neighbors;

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
     */
    WrapperWifiUdp(boolean verbose, Context context, Config config,
                   HashMap<String, AdHocDevice> mapAddressDevice,
                   final ListenerApp listenerApp, final ListenerDataLink listenerDataLink) {
        super(verbose, config, mapAddressDevice, listenerApp, listenerDataLink);

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
     */
    @Override
    void init(Config config, Context context) {
        this.isGroupOwner = false;
        this.neighbors = new HashMap<>();
        this.helloMessages = new HashMap<>();
        this.ackSet = new HashSet<>();
        this.serverPort = config.getServerPort();
        this.listenServer();
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

        String label = getLabelByMac(adHocDevice.getMacAddress());
        if (label == null) {
            wifiAdHocManager.connect(adHocDevice.getMacAddress());
        } else {
            if (!neighbors.containsKey(label)) {
                wifiAdHocManager.connect(adHocDevice.getMacAddress());
            } else {
                throw new DeviceException(adHocDevice.getDeviceName()
                        + "(" + adHocDevice.getMacAddress() + ") is already connected");
            }
        }
    }

    /**
     * Method allowing to stop a listening on incoming connections.
     */
    @Override
    void stopListening() {
        udpPeers.stopServer();
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
            public void onDiscoveryFailed(Exception exception) {
                discoveryListener.onDiscoveryFailed(exception);
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

                    if (listenerBothDiscovery != null) {
                        discoveryListener.onDiscoveryCompleted(mapMacDevices);
                    }

                    discoveryCompleted = true;

                    wifiAdHocManager.unregisterDiscovery();
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
        neighbors.clear();
        ackSet.clear();
        helloMessages.clear();

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
    void unregisterConnection() {
        wifiAdHocManager.unregisterConnection();
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

    /**
     * Method allowing to send a message to a remote peer.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @param address a String value which represents the address of the remote device.
     */
    @Override
    void sendMessage(MessageAdHoc message, String address) {

        WifiAdHocDevice wifiDevice = (WifiAdHocDevice) neighbors.get(address);
        if (wifiDevice != null) {
            _sendMessage(message, wifiDevice.getIpAddress());
        }
    }

    /**
     * Method allowing to check if a node is a direct neighbour.
     *
     * @param address a String value which represents the address of the remote device.
     * @return a boolean value which is true if the device is a direct neighbors. Otherwise, false.
     */
    @Override
    boolean isDirectNeighbors(String address) {
        return neighbors.containsKey(address);
    }

    /**
     * Method allowing to broadcast a message to all directly connected nodes.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     */
    @Override
    boolean broadcast(MessageAdHoc message) {
        if (neighbors.size() > 0) {
            _sendMessage(message, "192.168.49.255");
            return true;
        }

        return false;
    }

    /**
     * Method allowing to disconnect the mobile from all the remote mobiles.
     */
    @Override
    void disconnectAll() {
        // Not used in this context
    }

    /**
     * Method allowing to disconnect the mobile from a remote mobile.
     */
    @Override
    void disconnect(String remoteDest) {
        // Not used in this context
    }

    /**
     * Method allowing to broadcast a message to all directly connected nodes excepted the excluded
     * node.
     *
     * @param message         a MessageAdHoc object which represents the message to send through
     *                        the network.
     * @param excludedAddress a String value which represents the excluded address.
     * @return a boolean value which is true if the broadcast was successful. Otherwise, false.
     */
    @Override
    boolean broadcastExcept(MessageAdHoc message, String excludedAddress) {
        if (neighbors.size() > 0) {
            for (Map.Entry<String, AdHocDevice> entry : neighbors.entrySet()) {
                if (!entry.getKey().equals(excludedAddress)) {
                    WifiAdHocDevice wifiAdHocDevice = (WifiAdHocDevice) entry.getValue();
                    _sendMessage(message, wifiAdHocDevice.getIpAddress());
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Method allowing to get the direct neighbours of the current mobile.
     *
     * @return an ArrayList<AdHocDevice> object which represents the direct neighbours of the current mobile.
     */
    public ArrayList<AdHocDevice> getDirectNeighbors() {
        return new ArrayList<>(neighbors.values());
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
     */
    private void listenServer() {
        udpPeers = new UdpPeers(v, serverPort, json, label, new ServiceMessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                processMsgReceived(message);
            }

            @Override
            public void onConnectionClosed(String remoteAddress) {
                // If remote node has disabled wifi
                String label = getLabelByIP(remoteAddress);
                AdHocDevice adHocDevice = null;

                if (v) Log.w(TAG, label + " has wifi doisconnected");
                if (neighbors.containsKey(label)) {
                    adHocDevice = neighbors.get(label);
                    neighbors.remove(label);
                }

                if (helloMessages.containsKey(label)) {
                    helloMessages.remove(label);
                }

                if (adHocDevice != null) {
                    listenerApp.onConnectionClosed(adHocDevice);
                }
            }

            @Override
            public void onConnection(String remoteAddress) {
                // Ignored in udp context
            }

            @Override
            public void onConnectionFailed(Exception e) {
                // Ignored in udp context
            }

            @Override
            public void onMsgException(Exception e) {
                listenerApp.processMsgException(e);
            }
        });

        //Run timers for HELLO messages
        timerHello(Constants.HELLO_PACKET_INTERVAL);
        timerHelloCheck(Constants.HELLO_PACKET_INTERVAL_SND);
    }

    /**
     * Method allowing to send a Connect message if no ACK is received.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @param dest    a String address which represents the IP address of the remote node.
     * @param time    an integer value which represents the time-expiration of the timer.
     */
    private void timerConnectMessage(final MessageAdHoc message, final String dest, final int time) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                if (ackSet.contains(dest)) {
                    _sendMessage(message, dest);
                    // Restart timer if no ACK is received
                    timerConnectMessage(message, dest, time);
                }
            }
        }, time);
    }

    /**
     * Method allowing to send a message to a remote node.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @param dest    a String address which represents the IP address of the remote node.
     */
    private void _sendMessage(final MessageAdHoc message, final String dest) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InetAddress inetAddress;
                try {
                    inetAddress = InetAddress.getByName(dest);
                    udpPeers.sendMessageTo(message, inetAddress, remotePort);
                } catch (UnknownHostException e) {
                    listenerApp.processMsgException(e);
                }
            }
        }).start();
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        // Used to avoid updating views in other threads than the main thread
        public void handleMessage(Message msg) {
            // Used handler to avoid updating views in other threads than the main thread
            AdHocDevice adHocDevice = (AdHocDevice) msg.obj;

            listenerApp.onConnectionClosed(adHocDevice);

            // If connectionFlooding option is enable, flood disconnect events
            if (connectionFlooding) {
                String id = adHocDevice.getLabel() + System.currentTimeMillis();
                setFloodEvents.add(id);
                Header header = new Header(AbstractWrapper.DISCONNECT_BROADCAST,
                        adHocDevice.getMacAddress(), adHocDevice.getLabel(), adHocDevice.getDeviceName(),
                        adHocDevice.getType());
                broadcastExcept(new MessageAdHoc(header, id), adHocDevice.getLabel());
            }
        }
    };

    /**
     * Method allowing to launch the timer to send HELLO messages between peers every TIME (ms).
     *
     * @param time an integer value which represents the period of the timer.
     */
    private void timerHello(final int time) {
        Timer timerHelloPackets = new Timer();
        timerHelloPackets.schedule(new TimerTask() {
            @Override
            public void run() {
                broadcastExcept(new MessageAdHoc(new Header(TypeAodv.HELLO.getType(), label, ownName)), label);
                timerHello(time);
            }
        }, time);
    }

    /**
     * Method allowing to launch the timer to check every TIME (ms) if a neighbor is broken.
     *
     * @param time an integer value which represents the period of the timer.
     */
    private void timerHelloCheck(final int time) {
        Timer timerHelloPackets = new Timer();
        timerHelloPackets.schedule(new TimerTask() {
            @Override
            public void run() {

                // Check peers
                Iterator<Map.Entry<String, Long>> iter = helloMessages.entrySet().iterator();
                while (iter.hasNext()) {

                    Map.Entry<String, Long> entry = iter.next();
                    long upTime = (System.currentTimeMillis() - entry.getValue());
                    Log.d(TAG, "UpTime: " + upTime);
                    if (upTime > Constants.HELLO_PACKET_INTERVAL_SND) {

                        if (v)
                            Log.d(TAG, "Neighbor " + entry.getKey() + " is down for " + upTime);

                        // Remove the hello message
                        iter.remove();
                        try {
                            leftPeer(entry.getKey());
                        } catch (IOException e) {
                            listenerApp.processMsgException(e);
                        }
                    }
                }
                timerHelloCheck(time);
            }
        }, time);
    }

    /**
     * Method allowing to process a remote peer that has left the session.
     *
     * @param label a String address which represents the label of the remote node.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    private void leftPeer(String label) throws IOException {

        // Process broken link in protocol
        listenerDataLink.brokenLink(label);

        // Callback via handler
        WifiAdHocDevice wifiDevice = (WifiAdHocDevice) neighbors.get(label);
        if (wifiDevice != null) {
            // Used handler to avoid using runOnUiThread in main app
            mHandler.obtainMessage(1, wifiDevice).sendToTarget();

            // Remove the remote device from a neighbors
            neighbors.remove(label);
        }
    }

    /**
     * Method allowing to process messages from remote nodes.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     */
    private void processMsgReceived(final MessageAdHoc message) {

        switch (message.getHeader().getType()) {
            case CONNECT_SERVER: {

                Log.d(TAG, "Receive " + message);

                boolean event = false;

                // Receive UDP header from remote host
                Header header = message.getHeader();

                // Extract PDU
                UdpPDU udpPDU = (UdpPDU) message.getPdu();

                String destAddress = udpPDU.getHostAddress();
                remotePort = udpPDU.getPort();

                // If ownIpAddress is unknown, init the field
                if (ownIpAddress == null) {
                    ownIpAddress = destAddress;
                }

                // Send message to remote host with own info
                _sendMessage(new MessageAdHoc(new Header(CONNECT_CLIENT, ownIpAddress,
                        ownMac, label, ownName)), header.getAddress());

                WifiAdHocDevice device = new WifiAdHocDevice(header.getLabel(), header.getMac(),
                        header.getName(), type, header.getAddress());

                if (!neighbors.containsKey(header.getLabel())) {
                    event = true;
                }

                if (event) {
                    // Add remote host to neighbors
                    neighbors.put(header.getLabel(), device);

                    // Callback connection
                    listenerApp.onConnection(device);

                    // If connectionFlooding option is enable, flood connect events
                    if (connectionFlooding) {
                        String id = header.getLabel() + System.currentTimeMillis();
                        setFloodEvents.add(id);
                        header.setType(AbstractWrapper.CONNECT_BROADCAST);
                        broadcastExcept(new MessageAdHoc(header, id), header.getLabel());
                    }
                }

                break;
            }
            case CONNECT_CLIENT: {

                Log.d(TAG, "Receive " + message);

                boolean event = false;

                // Receive UDP header from remote host
                Header header = message.getHeader();

                // Update the ackSet for reliable transmission
                if (ackSet.contains(header.getAddress())) {
                    ackSet.remove(header.getAddress());
                } else {
                    break;
                }

                WifiAdHocDevice device = new WifiAdHocDevice(header.getLabel(), header.getMac(),
                        header.getName(), type, header.getAddress());

                if (!neighbors.containsKey(header.getLabel())) {
                    event = true;
                }

                if (event) {
                    // Add remote host to neighbors
                    neighbors.put(header.getLabel(), device);

                    // Callback connection
                    listenerApp.onConnection(device);

                    // If connectionFlooding option is enable, flood connect events
                    if (connectionFlooding) {
                        String id = header.getLabel() + System.currentTimeMillis();
                        setFloodEvents.add(id);
                        header.setType(AbstractWrapper.CONNECT_BROADCAST);
                        broadcastExcept(new MessageAdHoc(header, id), header.getLabel());
                    }
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
            case Constants.HELLO: {

                if (v) Log.d(TAG, "Received Hello message from " + message.getHeader().getName());

                // Add helloMessages messages to hashmap
                helloMessages.put(message.getHeader().getLabel(), System.currentTimeMillis());
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
            public void onClient(final InetAddress groupOwnerAddress, final InetAddress address) {

                ownIpAddress = address.getHostAddress();
                isGroupOwner = false;
                if (v)
                    Log.d(TAG, "onClient-> GroupOwner IP: " + groupOwnerAddress.getHostAddress());
                if (v) Log.d(TAG, "onClient-> own IP: " + ownIpAddress);
                ackSet.add(groupOwnerAddress.getHostAddress());

                wifiAdHocManager.discoverService(new WifiAdHocManager.ServiceDiscoverListener() {
                    @Override
                    public void onServiceCompleted(int port) {
                        remotePort = port;
                        if (v) Log.d(TAG, "Remote port is " + remotePort);
                        timerConnectMessage(new MessageAdHoc(
                                        new Header(CONNECT_SERVER, ownIpAddress, ownMac, label, ownName),
                                        new UdpPDU(serverPort, groupOwnerAddress.getHostAddress())),
                                groupOwnerAddress.getHostAddress(), TIMER_ACK);
                    }
                });
            }
        };
    }

    /**
     * Method allowing to get a label address from a MAC address.
     *
     * @param mac a String value which represents a MAC address.
     * @return a String value which represents an label address associated with the given MAC address.
     */
    private String getLabelByMac(String mac) {
        for (Map.Entry<String, AdHocDevice> entry : neighbors.entrySet()) {
            if (mac.equals(entry.getValue().getMacAddress())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Method allowing to get a label address from an IP address.
     *
     * @param ip a String value which represents an IP address.
     * @return a String value which represents an label address associated with the given IP address.
     */
    private String getLabelByIP(String ip) {
        for (Map.Entry<String, AdHocDevice> entry : neighbors.entrySet()) {
            WifiAdHocDevice wifiDevice = (WifiAdHocDevice) entry.getValue();
            if (ip.equals(wifiDevice.getIpAddress())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
