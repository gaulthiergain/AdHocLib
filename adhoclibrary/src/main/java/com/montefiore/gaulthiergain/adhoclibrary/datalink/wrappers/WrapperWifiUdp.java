package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageMainListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.UdpMsg;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.UdpPeers;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.WifiAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.Constants;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.TypeAodv;
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
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class WrapperWifiUdp extends AbstractWrapper {

    private static final String TAG = "[AdHoc][WrapperWifiUdp]";
    private static final int TIMER_ACK = 2000;

    private int serverPort;
    private UdpPeers udpPeers;
    private String ownIpAddress;
    private HashSet<String> ackSet;
    private WifiAdHocManager wifiAdHocManager;
    private HashMap<String, Long> helloMessages;


    public WrapperWifiUdp(boolean verbose, Context context, Config config, Neighbors neighbors,
                          HashMap<String, AdHocDevice> mapAddressDevice,
                          final ListenerApp listenerAodv, final ListenerDataLink listenerDataLink) {
        super(verbose, context, config.isJson(), config.isBackground(), config.getLabel(),
                mapAddressDevice, neighbors, listenerAodv, listenerDataLink);

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

                ownIpAddress = address.getHostAddress();
                if (v) Log.d(TAG, "onClient: " + ownIpAddress);

                ackSet.add(groupOwnerAddress.getHostAddress());
                timerConnectMessage(new MessageAdHoc(new Header(CONNECT_SERVER, label, ownName),
                                new UdpMsg(ownMac, ownIpAddress, groupOwnerAddress.getHostAddress())),
                        groupOwnerAddress.getHostAddress(), TIMER_ACK);
            }
        };
        try {
            this.wifiAdHocManager = new WifiAdHocManager(v, context, connectionListener);
            if (wifiAdHocManager.isEnabled()) {

                this.type = DataLinkManager.WIFI;
                this.helloMessages = new HashMap<>();
                this.ownMac = wifiAdHocManager.getOwnMACAddress().toLowerCase();
                this.serverPort = config.getServerPort();

                this.wifiAdHocManager.getDeviceName(new WifiAdHocManager.ListenerWifiDeviceName() {

                    @Override
                    public void getDeviceName(String name) {
                        ownName = name;
                        wifiAdHocManager.unregisterInitName();
                    }
                });

                this.initUdpPeers();
                this.ackSet = new HashSet<>();
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
    public void stopListening() {
        udpPeers.setBackgroundRunning(false);
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

                // Add devices into hashmap
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
    public void enable(int duration, ListenerAdapter listenerAdapter) {
        wifiAdHocManager.enable();
        wifiAdHocManager.onEnableWifi(listenerAdapter);
    }

    @Override
    public void disable() {
        wifiAdHocManager.disable();
    }

    @Override
    public void unregisterConnection() {
        wifiAdHocManager.unregisterConnection();
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void unregisterAdapter() {
        wifiAdHocManager.unregisterEnableAdapter();
    }

    @Override
    public void updateName(String name) {
        try {
            wifiAdHocManager.updateName(name);
        } catch (InvocationTargetException e) {
            listenerApp.catchException(e);
        } catch (IllegalAccessException e) {
            listenerApp.catchException(e);
        } catch (NoSuchMethodException e) {
            listenerApp.catchException(e);
        }
    }

    @Override
    public void sendMessage(MessageAdHoc msg, String label) {

        NetworkObject networkObject = neighbors.getNeighbors().get(label);
        if (networkObject != null && networkObject.getType() == type) {
            WifiAdHocDevice wifiAdHocDevice = (WifiAdHocDevice) networkObject.getSocketManager();
            _sendMessage(msg, wifiAdHocDevice.getIpAddress());
        }
    }

    @Override
    public void broadcast(MessageAdHoc message) {
        for (Map.Entry<String, NetworkObject> entry : neighbors.getNeighbors().entrySet()) {
            if (entry.getValue().getType() == type) {
                WifiAdHocDevice wifiAdHocDevice = (WifiAdHocDevice) entry.getValue().getSocketManager();
                _sendMessage(message, wifiAdHocDevice.getIpAddress());
            }
        }
    }

    @Override
    public void broadcastExcept(MessageAdHoc message, String excludedAddress) {
        for (Map.Entry<String, NetworkObject> entry : neighbors.getNeighbors().entrySet()) {
            if (entry.getValue().getType() == type) {
                if (!entry.getKey().equals(excludedAddress)) {
                    WifiAdHocDevice wifiAdHocDevice = (WifiAdHocDevice) entry.getValue().getSocketManager();
                    _sendMessage(message, wifiAdHocDevice.getIpAddress());
                }
            }
        }
    }

    /*--------------------------------------Public methods----------------------------------------*/

    public void setGroupOwnerValue(int valueGroupOwner) throws GroupOwnerBadValue {

        if (valueGroupOwner < 0 || valueGroupOwner > 15) {
            throw new GroupOwnerBadValue("GroupOwner value must be ");
        }

        wifiAdHocManager.setValueGroupOwner(valueGroupOwner);
    }
    /*--------------------------------------Private methods---------------------------------------*/

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
     * Method allowing to init the UDP peers.
     */
    private void initUdpPeers() {
        udpPeers = new UdpPeers(true, serverPort, true, new MessageMainListener() {
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
                if (v) Log.d(TAG, "onMessageSent");
            }

            @Override
            public void onForward(MessageAdHoc message) {
                if (v) Log.d(TAG, "onForward");
            }

            @Override
            public void catchException(Exception e) {
                listenerApp.catchException(e);
            }
        });
        //Run timers for HELLO messages
        timerHello(Constants.HELLO_PACKET_INTERVAL);
        timerHelloCheck(Constants.HELLO_PACKET_INTERVAL_SND);
    }

    private void _sendMessage(final MessageAdHoc msg, final String address) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                InetAddress inetAddress;
                try {
                    inetAddress = InetAddress.getByName(address);
                    udpPeers.sendMessageTo(msg, inetAddress, serverPort);
                    if (v)
                        Log.d(TAG, msg.toString() + " is sent on " + inetAddress + " on " + serverPort);
                } catch (UnknownHostException e) {
                    listenerApp.catchException(e);
                }
            }
        }).start();
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        // Used to avoid updating views in other threads than the main thread
        public void handleMessage(Message msg) {
            // Used handler to avoid updating views in other threads than the main thread
            String[] couple = (String[]) msg.obj;
            listenerApp.onConnectionClosed(couple[0], couple[1]);
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
                for (Map.Entry<String, NetworkObject> entry : neighbors.getNeighbors().entrySet()) {

                    NetworkObject networkObject = entry.getValue();
                    if (networkObject.getType() == type) {
                        WifiAdHocDevice wifiAdHocDevice = (WifiAdHocDevice) networkObject.getSocketManager();
                        MessageAdHoc msg = new MessageAdHoc(new Header(TypeAodv.HELLO.getType(), label, ownName)
                                , System.currentTimeMillis());
                        _sendMessage(msg, wifiAdHocDevice.getIpAddress());
                        if (v) Log.d(TAG, "Send HELLO message to " + entry.getKey());
                    }
                }

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
                    if (upTime > Constants.HELLO_PACKET_INTERVAL_SND) {
                        try {
                            if (v)
                                Log.d(TAG, "Neighbor " + entry.getKey() + " is down for " + upTime);

                            // Remove the hello message
                            iter.remove();

                            // Process broken link in protocol
                            listenerDataLink.brokenLink(entry.getKey());

                            // Callback via handler
                            NetworkObject networkObject = neighbors.getNeighbors().get(entry.getKey());
                            if (networkObject != null && networkObject.getType() == type) {
                                WifiAdHocDevice wifiAdHocDevice = (WifiAdHocDevice) networkObject.getSocketManager();
                                // Used handler to avoid using runOnUiThread in main app
                                mHandler.obtainMessage(1,
                                        new String[]{entry.getKey(), wifiAdHocDevice.getName()})
                                        .sendToTarget();
                            }

                            // Remove the remote device from a neighbors
                            neighbors.getNeighbors().remove(entry.getKey());
                        } catch (IOException | NoConnectionException e) {
                            listenerApp.catchException(e);
                        }
                    }
                }
                timerHelloCheck(time);
            }
        }, time);
    }

    private void processMsgReceived(final MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {

        if (v) Log.d(TAG, "Message rcvd " + message.toString());
        switch (message.getHeader().getType()) {
            case CONNECT_SERVER: {

                // Receive UDP message from remote host
                UdpMsg udpmsg = (UdpMsg) message.getPdu();

                // If ownIpAddress is unknown, init the field
                if (ownIpAddress == null) {
                    ownIpAddress = udpmsg.getDestinationAddress();
                }

                // Send message to remote host with own info
                _sendMessage(new MessageAdHoc(new Header(CONNECT_CLIENT, label, ownName),
                                new UdpMsg(ownMac, ownIpAddress, udpmsg.getSourceAddress())),
                        udpmsg.getSourceAddress());

                if (!neighbors.getNeighbors().containsKey(message.getHeader().getSenderAddr())) {
                    // Callback connection
                    listenerApp.onConnection(message.getHeader().getSenderAddr(),
                            message.getHeader().getSenderName());
                }

                // Add remote host to neighbors
                neighbors.getNeighbors().put(message.getHeader().getSenderAddr(),
                        new NetworkObject(DataLinkManager.WIFI,
                                new WifiAdHocDevice(message.getHeader().getSenderName(),
                                        udpmsg.getSourceAddress())));
                break;
            }
            case CONNECT_CLIENT: {

                // Receive UDP message from remote host
                UdpMsg udpmsg = (UdpMsg) message.getPdu();

                // Update the ackSet for reliable transmission
                if (ackSet.contains(udpmsg.getSourceAddress())) {
                    ackSet.remove(udpmsg.getSourceAddress());
                }

                if (!neighbors.getNeighbors().containsKey(message.getHeader().getSenderAddr())) {
                    // Callback connection
                    listenerApp.onConnection(message.getHeader().getSenderAddr(),
                            message.getHeader().getSenderName());
                }

                // Add remote host to neighbors
                neighbors.getNeighbors().put(message.getHeader().getSenderAddr(),
                        new NetworkObject(DataLinkManager.WIFI,
                                new WifiAdHocDevice(message.getHeader().getSenderName(),
                                        udpmsg.getSourceAddress())));
                break;
            }
            case Constants.HELLO: {
                // Add neighbors messages to hashmap
                helloMessages.put(message.getHeader().getSenderAddr(), (long) message.getPdu());
                break;
            }
            default:
                // Handle messages in protocol scope
                listenerDataLink.processMsgReceived(message);
        }
    }
}
