package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageMainListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.UdpPeers;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.Constants;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.TypeAodv;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
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
    private HashMap<String, AdHocDevice> neighbors;


    public WrapperWifiUdp(boolean verbose, Context context, Config config,
                          HashMap<String, AdHocDevice> mapAddressDevice,
                          final ListenerApp listenerApp, final ListenerDataLink listenerDataLink) {
        super(verbose, context, config, mapAddressDevice, listenerApp, listenerDataLink);

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
        wifiAdHocManager.connect(device.getMacAddress());
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
            public void onDiscoveryFailed(Exception exception) {
                listenerApp.onDiscoveryFailed(exception);
            }

            @Override
            public void onDeviceDiscovered(AdHocDevice device) {

                //todo refactor this

                if (!mapMacDevices.containsKey(device.getMacAddress())) {
                    if (v)
                        Log.d(TAG, "Add " + device.getMacAddress() + " into mapMacDevices");
                    mapMacDevices.put(device.getMacAddress(), device);
                }

                listenerApp.onDeviceDiscovered(device);
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

                if (discoveryListener != null) {
                    listenerApp.onDiscoveryCompleted(mapMacDevices);
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
        this.ownMac = wifiAdHocManager.getOwnMACAddress();
        this.wifiAdHocManager.getAdapterName(new WifiAdHocManager.ListenerWifiDeviceName() {
            @Override
            public void getDeviceName(String name) {
                ownName = name;
                listenerDataLink.initInfos(ownMac, ownName);
            }
        });
        this.ackSet = new HashSet<>();
        this.serverPort = config.getServerPort();
        this.listenServer();
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

        WifiAdHocDevice wifiDevice = (WifiAdHocDevice) neighbors.get(label);
        if (wifiDevice != null) {
            _sendMessage(msg, wifiDevice.getIpAddress());
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
        for (Map.Entry<String, AdHocDevice> entry : neighbors.entrySet()) {
            if (!entry.getKey().equals(excludedAddress)) {
                WifiAdHocDevice wifiAdHocDevice = (WifiAdHocDevice) entry.getValue();
                _sendMessage(message, wifiAdHocDevice.getIpAddress());
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
                } catch (IOException e) {
                    listenerApp.processMsgException(e);
                }
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
                broadcast(new MessageAdHoc(new Header(TypeAodv.HELLO.getType(), label, ownName)));
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

    private void processMsgReceived(final MessageAdHoc message) throws IOException {

        switch (message.getHeader().getType()) {
            case CONNECT_SERVER: {

                // Receive UDP header from remote host
                Header header = message.getHeader();

                String destAddress = (String) message.getPdu();

                // If ownIpAddress is unknown, init the field
                if (ownIpAddress == null) {
                    ownIpAddress = destAddress;
                }

                // Send message to remote host with own info
                _sendMessage(new MessageAdHoc(new Header(CONNECT_CLIENT, ownIpAddress,
                        ownMac, label, ownName)), header.getAddress());

                WifiAdHocDevice device = new WifiAdHocDevice(header.getLabel(), header.getMac(),
                        header.getName(), type, header.getAddress());

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

                break;
            }
            case CONNECT_CLIENT: {

                // Receive UDP header from remote host
                Header header = message.getHeader();

                // Update the ackSet for reliable transmission
                if (ackSet.contains(header.getAddress())) {
                    ackSet.remove(header.getAddress());
                }

                WifiAdHocDevice device = new WifiAdHocDevice(header.getLabel(), header.getMac(),
                        header.getName(), type, header.getAddress());

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
            case Constants.HELLO: {
                // Add helloMessages messages to hashmap
                helloMessages.put(message.getHeader().getLabel(), System.currentTimeMillis());
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
            public void onConnectionFailed(Exception e) {
                wifiAdHocManager.cancelConnection();
                listenerApp.onConnectionFailed(e);
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

                timerConnectMessage(new MessageAdHoc(new Header(CONNECT_SERVER, ownIpAddress,
                                ownMac, label, ownName), groupOwnerAddress.getHostAddress()),
                        groupOwnerAddress.getHostAddress(), TIMER_ACK);
            }
        };
    }
}
