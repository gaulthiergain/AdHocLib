package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageMainListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.UdpMsg;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.UdpPeers;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.WifiUdpDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.Constants;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.TypeAodv;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvAbstractException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
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
    private HashMap<String, WifiUdpDevice> neighbors;


    public WrapperWifiUdp(boolean verbose, Context context, Config config,
                          HashMap<String, AdHocDevice> mapAddressDevice,
                          final ListenerApp listenerAodv, final ListenerDataLink listenerDataLink) {
        super(verbose, context, config.getName(), config.isJson(), config.getLabel(),
                mapAddressDevice, listenerAodv, listenerDataLink);

        try {
            this.type = Service.WIFI;
            this.wifiAdHocManager = new WifiAdHocManager(v, context, initConnectionListener());
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
    public void connect(AdHocDevice device) {
        wifiAdHocManager.connect(device.getDeviceAddress());
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
                listenerApp.onDiscoveryStarted();
            }

            @Override
            public void onDiscoveryFailed(int reasonCode) {
                //TODO switch with reason code
                listenerApp.onDiscoveryFailed("");
            }

            @Override
            public void onDeviceDiscovered(AdHocDevice device) {
                listenerApp.onDeviceDiscovered(device);
            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapNameDevice) {
                if (v) Log.d(TAG, "onDiscoveryCompleted");

                // Add devices into hashmap
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
    public void unregisterConnection() {
        wifiAdHocManager.unregisterConnection();
    }

    @Override
    public void init(Config config) {
        this.neighbors = new HashMap<>();
        this.helloMessages = new HashMap<>();
        this.ownMac = wifiAdHocManager.getOwnMACAddress().toLowerCase();
        this.serverPort = config.getServerPort();
        this.listenServer();
        this.ackSet = new HashSet<>();
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

    @Override
    public void sendMessage(MessageAdHoc msg, String label) {

        WifiUdpDevice wifiUdpDevice = neighbors.get(label);
        if (wifiUdpDevice != null) {
            _sendMessage(msg, wifiUdpDevice.getIpAddress());
        }
    }

    @Override
    public boolean isDirectNeighbors(String address) {
        return neighbors.containsKey(address);
    }

    @Override
    public void broadcast(MessageAdHoc message) {
        if (neighbors.size() > 0) {
            _sendMessage(message, "192.168.49.255");
        }
    }

    @Override
    public void disconnectAll() {
        // Not used in this context
    }

    @Override
    public void disconnect(String remoteDest) {
        // Not used in this context
    }

    @Override
    public void broadcastExcept(MessageAdHoc message, String excludedAddress) {
        for (Map.Entry<String, WifiUdpDevice> entry : neighbors.entrySet()) {
            if (!entry.getKey().equals(excludedAddress)) {
                _sendMessage(message, entry.getValue().getIpAddress());
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

    private void listenServer() {
        udpPeers = new UdpPeers(true, serverPort, true, new MessageMainListener() {
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
        //Run timers for HELLO messages
        timerHello(Constants.HELLO_PACKET_INTERVAL);
        timerHelloCheck(Constants.HELLO_PACKET_INTERVAL_SND);
    }

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
                    listenerApp.traceException(e);
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
                broadcast(new MessageAdHoc(new Header(TypeAodv.HELLO.getType(), label, ownName), ""));
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
                        try {
                            if (v)
                                Log.d(TAG, "Neighbor " + entry.getKey() + " is down for " + upTime);

                            // Remove the hello message
                            iter.remove();
                            leftPeer(entry.getKey());

                        } catch (IOException | NoConnectionException e) {
                            listenerApp.traceException(e);
                        }
                    }
                }
                timerHelloCheck(time);
            }
        }, time);
    }

    private void leftPeer(String label) throws IOException, NoConnectionException {

        // Process broken link in protocol
        listenerDataLink.brokenLink(label);

        // Callback via handler
        WifiUdpDevice wifiUdpDevice = neighbors.get(label);
        if (wifiUdpDevice != null) {
            // Used handler to avoid using runOnUiThread in main app
            mHandler.obtainMessage(1,
                    new String[]{label, wifiUdpDevice.getName()})
                    .sendToTarget();

            // Remove the remote device from a neighbors
            neighbors.remove(label);
        }
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

                if (!neighbors.containsKey(message.getHeader().getSenderAddr())) {
                    // Callback connection
                    listenerApp.onConnection(message.getHeader().getSenderAddr(),
                            message.getHeader().getSenderName(), 0);
                }

                // Add remote host to neighbors
                neighbors.put(message.getHeader().getSenderAddr(),
                        new WifiUdpDevice(message.getHeader().getSenderName(),
                                udpmsg.getSourceAddress()));
                break;
            }
            case CONNECT_CLIENT: {

                // Receive UDP message from remote host
                UdpMsg udpmsg = (UdpMsg) message.getPdu();

                // Update the ackSet for reliable transmission
                if (ackSet.contains(udpmsg.getSourceAddress())) {
                    ackSet.remove(udpmsg.getSourceAddress());
                }

                if (!neighbors.containsKey(message.getHeader().getSenderAddr())) {
                    // Callback connection
                    listenerApp.onConnection(message.getHeader().getSenderAddr(),
                            message.getHeader().getSenderName(), 0);
                }

                // Add remote host to neighbors
                neighbors.put(message.getHeader().getSenderAddr(),
                        new WifiUdpDevice(message.getHeader().getSenderName(),
                                udpmsg.getSourceAddress()));
                break;
            }
            case Constants.HELLO: {
                // Add helloMessages messages to hashmap
                helloMessages.put(message.getHeader().getSenderAddr(), System.currentTimeMillis());
                break;
            }
            default:
                // Handle messages in protocol scope
                listenerDataLink.processMsgReceived(message);
        }
    }

    private ConnectionListener initConnectionListener() {
        return new ConnectionListener() {
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
    }
}
