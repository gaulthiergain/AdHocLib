package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.applayer.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageMainListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.UdpMsg;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.UdpPeers;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.WifiAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ActiveConnections;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DiscoveredDevice;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by gaulthiergain on 22/03/18.
 */

public class WrapperWifiUdp extends AbstractWrapper {

    private static final String TAG = "[AdHoc][WrapperUdp]";

    private int serverPort;
    private String ownIpAddress;

    private HashMap<String, Long> helloMessages;
    private WifiAdHocManager wifiAdHocManager;
    private HashMap<String, WifiAdHocDevice> neighbors;

    private UdpPeers udpPeers;
    private HashSet<String> ackSet;


    public WrapperWifiUdp(boolean v, Context context, short nbThreads, final int serverPort,
                          final String label, ActiveConnections activeConnections,
                          HashMap<String, DiscoveredDevice> mapAddressDevice,
                          final ListenerApp listenerAodv, final ListenerDataLink listenerDataLink) {
        super(v, context, label, mapAddressDevice, activeConnections, listenerAodv, listenerDataLink);

        ConnectionListener connectionListener = new ConnectionListener() {
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

                ownIpAddress = address.getHostAddress();

                Log.d(TAG, "onClient groupOwner Address: " + groupOwnerAddress.getHostAddress());
                Log.d(TAG, "OWN IP address: " + ownIpAddress);

                Header header = new Header(CONNECT_SERVER, label, ownName);
                ackSet.add(groupOwnerAddress.getHostAddress());
                Log.d(TAG, "Client add " + (groupOwnerAddress.getHostAddress()) + " into set");

                timerClientMessage(new MessageAdHoc(header, new UdpMsg(ownMac, ownIpAddress, groupOwnerAddress.getHostAddress())),
                        groupOwnerAddress.getHostAddress(), 2000);
            }
        };

        try {
            this.wifiAdHocManager = new WifiAdHocManager(v, context, connectionListener);
            if (wifiAdHocManager.isEnabled()) {

                this.helloMessages = new HashMap<>();
                this.ownMac = wifiAdHocManager.getOwnMACAddress().toLowerCase();
                this.serverPort = serverPort;


                //this.mapIpNetwork = new HashMap<>();
                //this.mapLabelRemoteDeviceName = new HashMap<>();

                this.wifiAdHocManager.getDeviceName(new WifiAdHocManager.ListenerWifiDeviceName() {

                    @Override
                    public void getDeviceName(String name) {
                        // Update ownName
                        ownName = name;
                        wifiAdHocManager.unregisterInitName();
                    }
                });


                this.init();
                //
                this.ackSet = new HashSet<>();
                this.neighbors = new HashMap<>();
            } else {
                enabled = false;
            }
        } catch (DeviceException e) {
            enabled = false;
        }
    }

    private void timerClientMessage(final MessageAdHoc message,
                                    final String dest, final int time) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                Log.d(TAG, "Timer client: " + (dest) + " into set");

                if (ackSet.contains(dest)) {
                    // send Connect Reply Message
                    _sendMessage(message, dest);
                    Log.d(TAG, "Timer client contains " + (dest) + " into set");
                    timerClientMessage(message, dest, time);
                }
            }
        }, time);
    }

    /**
     * Method allowing to init the UDP peers.
     */
    private void init() {
        udpPeers = new UdpPeers(true, serverPort, true, new MessageMainListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException | NoConnectionException e) {
                    listenerApp.catchException(e);
                } catch (AodvUnknownDestException e) {
                    listenerApp.catchException(e);
                } catch (AodvUnknownTypeException e) {
                    listenerApp.catchException(e);
                }
            }

            @Override
            public void onMessageSent(MessageAdHoc message) {
            }

            @Override
            public void onForward(MessageAdHoc message) {

            }

            @Override
            public void catchException(Exception e) {
                listenerApp.catchException(e);
            }
        });
        // Run timers for HELLO messages TODO
        /*timerHello(Constants.HELLO_PACKET_INTERVAL);
        timerHelloCheck(Constants.HELLO_PACKET_INTERVAL_SND);*/
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
                                        entry.getValue().deviceName, DiscoveredDevice.WIFI));
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

    }

    @Override
    public void connect(DiscoveredDevice device) {
        Log.d(TAG, "Remote Address" + device.getAddress());
        wifiAdHocManager.connect(device.getAddress());
    }

    private void processMsgReceived(final MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {
        switch (message.getHeader().getType()) {
            case CONNECT_SERVER: {

                UdpMsg udpmsg = (UdpMsg) message.getPdu();

                neighbors.put(message.getHeader().getSenderAddr(),
                        new WifiAdHocDevice(message.getHeader().getSenderName(), udpmsg.getSourceAddress()));
                Log.d(TAG, "CATCH " + neighbors.get(message.getHeader().getSenderAddr()));

                if (ownIpAddress == null) {
                    ownIpAddress = udpmsg.getDestinationAddress();
                }

                _sendMessage(new MessageAdHoc(new Header(CONNECT_CLIENT, label, ownName),
                                new UdpMsg(ownMac, ownIpAddress, udpmsg.getSourceAddress())),
                        udpmsg.getSourceAddress());

                // Callback connection
                listenerApp.onConnection(message.getHeader().getSenderAddr(),
                        message.getHeader().getSenderName());

                break;
            }
            case CONNECT_CLIENT: {

                UdpMsg udpmsg = (UdpMsg) message.getPdu();

                Log.d(TAG, "Client check " + (udpmsg.getSourceAddress()) + " in set");
                if (ackSet.contains(udpmsg.getSourceAddress())) {
                    ackSet.remove(udpmsg.getSourceAddress());
                    Log.d(TAG, "Client remove " + (udpmsg.getSourceAddress()) + " in set");
                }

                neighbors.put(message.getHeader().getSenderAddr(), new WifiAdHocDevice(message.getHeader().getSenderName(),
                        udpmsg.getSourceAddress()));
                Log.d(TAG, "CATCH " + neighbors.get(message.getHeader().getSenderAddr()));

                // Callback connection
                listenerApp.onConnection(message.getHeader().getSenderAddr(), message.getHeader().getSenderName());

                break;
            /*case HELLO:
                helloMessages.put(message.getHeader().getSenderAddr(), (long) message.getPdu());
                break;*/
            }
            default:
                // Handle messages in protocol scope
                listenerDataLink.processMsgReceived(message);
        }
    }

    @Override
    public void stopListening() throws IOException {
        udpPeers.setBackgroundRunning(false);
    }

    @Override
    public void getPaired() {

    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void unregisterConnection() {

    }

    public boolean isDirectNeighbors(String address) {
        return neighbors.containsKey(address);
    }

    @Override
    public void enable(int duration) {
        wifiAdHocManager.enable();
    }

    public void sendMessage(MessageAdHoc msg, String label){
        WifiAdHocDevice wifiAdHocDevice = neighbors.get(label);
        if(wifiAdHocDevice != null){
            Log.d(TAG, "Neigbors " + wifiAdHocDevice.toString());
            _sendMessage(msg, wifiAdHocDevice.getIpAddress());
        }
    }

    private void _sendMessage(final MessageAdHoc msg, final String address) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                InetAddress addr;
                try {
                    addr = InetAddress.getByName(address);
                    udpPeers.sendMessageTo(msg, addr, serverPort);
                    Log.d(TAG, msg.toString() + " is sent on " + addr + " on " + serverPort);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
