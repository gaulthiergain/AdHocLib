package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageMainListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi.UdpPeers;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiManager;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.Constants;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DataLinkWifiManagerUdp implements IDataLink {


    public static final String ID_APP = "#e091#";
    private static final String TAG = "[AdHoc][DataLinkWifi]";
    private final boolean v;
    private final Context context;

    private String ownIpAddress;
    private String ownMacAddress;
    private boolean groupOwner;

    private long seqNum;
    private HashSet<String> ackSet;

    private final int serverPort;
    private final HashMap<String, WifiAdHocDevice> neighbors;
    private final HashMap<String, Long> helloMessages;
    private final ListenerAodv listenerAodv;
    private final ListenerDataLinkAodv listenerDataLinkAodv;

    private final WifiManager wifiManager;

    private UdpPeers udpPeers;
    private MessageAdHoc connectMessage;
    private final Hashtable<String, WifiP2pDevice> peers;

    /**
     * Constructor
     *
     * @param verbose              a boolean value to set the debug/verbose mode.
     * @param context              a Context object which gives global information about an application
     *                             environment.
     * @param listenerAodv         a ListenerAodv object which serves as callback functions.
     * @param listenerDataLinkAodv a ListenerDataLinkAodv object which serves as callback functions.
     */
    public DataLinkWifiManagerUdp(boolean verbose, Context context, int serverPort,
                                  ListenerAodv listenerAodv, ListenerDataLinkAodv listenerDataLinkAodv)
            throws DeviceException {
        this.v = verbose;
        this.context = context;
        this.serverPort = serverPort;
        this.listenerAodv = listenerAodv;
        this.listenerDataLinkAodv = listenerDataLinkAodv;
        this.wifiManager = new WifiManager(v, context);
        this.neighbors = new HashMap<>();
        this.helloMessages = new HashMap<>();
        this.ownMacAddress = wifiManager.getOwnMACAddress().toLowerCase();
        Log.d(TAG, "OWN MAC is " + ownMacAddress);
        this.init();
        //
        this.seqNum = 1;
        this.ackSet = new HashSet<>();
        this.peers = new Hashtable<>();
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
                } catch (IOException e) {
                    listenerAodv.IOException(e);
                } catch (AodvUnknownTypeException e) {
                    listenerAodv.AodvUnknownTypeException(e);
                } catch (AodvUnknownDestException e) {
                    listenerAodv.AodvUnknownDestException(e);
                } catch (NoConnectionException e) {
                    listenerAodv.NoConnectionException(e);
                }
            }

            @Override
            public void onMessageSent(MessageAdHoc message) {
            }

            @Override
            public void onForward(MessageAdHoc message) {

            }
        });
        // Run timers for HELLO messages TODO
        /*timerHello(Constants.HELLO_PACKET_INTERVAL);
        timerHelloCheck(Constants.HELLO_PACKET_INTERVAL_SND);*/
    }

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
                /*for (Map.Entry<String, WifiP2pDevice> entry : peers.entrySet()) {
                    Header header = new Header(TypeAodv.HELLO.getCode(), ownIpAddress, ownName);
                    MessageAdHoc msg = new MessageAdHoc(header, System.currentTimeMillis());
                    try {
                        sendMessage(msg, entry.getValue().getAddr() + ":" + entry.getValue().getPort());
                    } catch (IOException e) {
                        listenerAodv.IOException(e);
                    }
                }*/

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
                        // Remove the peers and send RRER packets
                        try {
                            if (v)
                                Log.d(TAG, "Neighbor " + entry.getKey() + " is down for " + upTime);
                            neighbors.remove(entry.getKey());
                            iter.remove();
                            listenerDataLinkAodv.brokenLink(entry.getKey());
                        } catch (IOException e) {
                            listenerAodv.IOException(e);
                        } catch (NoConnectionException e) {
                            listenerAodv.NoConnectionException(e);
                        }
                    }
                }
                timerHelloCheck(time);
            }
        }, time);

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
        switch (message.getHeader().getType()) {
            case "CONNECT":

                if (groupOwner) {
                    Log.d(TAG, "THIS IS SPARTA GO");
                    /*Header header = new Header("CONNECT", groupOwnerAddress.toString(), "client");
                    sendConnectMessage(new MessageAdHoc(header, ownMacAddress), "192.168.49.255");*/
                }

                String sourceAddr = message.getHeader().getSenderAddr();
                UDPmsg udPmsg = (UDPmsg) message.getPdu();
                neighbors.put(message.getHeader().getSenderAddr(),
                        new WifiAdHocDevice(message.getHeader().getSenderName(),
                                message.getHeader().getSenderAddr(), udPmsg.getOwnMac()));
                Log.d(TAG, "CATCH " + neighbors.get(message.getHeader().getSenderAddr()));

                if (ownIpAddress == null) {
                    ownIpAddress = udPmsg.getDestinationAddress();
                }

                Header header = new Header("CONNECT_REPLY", ownIpAddress, "groupOwner");
                //ackSet.add(sourceAddr + "#" + seqNum);
                sendConnectMessage(new MessageAdHoc(header, new UDPmsg(ownMacAddress, udPmsg.getSeqNumber(), sourceAddr)),
                        sourceAddr);
                break;
            case "CONNECT_REPLY":

                String sourceAddr2 = message.getHeader().getSenderAddr();
                UDPmsg udPmsg2 = (UDPmsg) message.getPdu();


                Log.d(TAG, "Client check " + (sourceAddr2 + "#" + udPmsg2.getSeqNumber()) + " in set");
                if (ackSet.contains(sourceAddr2 + "#" + udPmsg2.getSeqNumber())) {
                    ackSet.remove(sourceAddr2 + "#" + udPmsg2.getSeqNumber());
                    Log.d(TAG, "Client remove " + (sourceAddr2 + "#" + udPmsg2.getSeqNumber()) + " in set");
                }

                neighbors.put(message.getHeader().getSenderAddr(),
                        new WifiAdHocDevice(message.getHeader().getSenderName(),
                                message.getHeader().getSenderAddr(), udPmsg2.getOwnMac()));

                Log.d(TAG, "CATCH " + neighbors.get(message.getHeader().getSenderAddr()));

                break;
            case "HELLO":
                helloMessages.put(message.getHeader().getSenderAddr(), (long) message.getPdu());
                break;
            default:
                // Handle messages in protocol scope
                listenerDataLinkAodv.processMsgReceived(message);
        }
    }

    @Override
    public void connect() {
        for (Map.Entry<String, WifiP2pDevice> deviceEntry : peers.entrySet()) {
            Log.d(TAG, "Remote Address" + deviceEntry.getValue().deviceAddress);
            wifiManager.connect(deviceEntry.getValue().deviceAddress, new ConnectionListener() {
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
                    groupOwner = true;

                    Log.d(TAG, "onGroupOwner: " + ownIpAddress);
                }

                @Override
                public void onClient(final InetAddress groupOwnerAddress, final InetAddress address) {

                    ownIpAddress = address.getHostAddress();
                    groupOwner = false;

                    Log.d(TAG, "onClient groupOwner Address: " + groupOwnerAddress.getHostAddress());
                    Log.d(TAG, "OWN IP address: " + ownIpAddress);

                    Header header = new Header("CONNECT", ownIpAddress, "client");
                    ackSet.add(groupOwnerAddress.getHostAddress() + "#" + seqNum);
                    Log.d(TAG, "Client add " + (groupOwnerAddress.getHostAddress() + "#" + seqNum) + " into set");
                    timerClientMessage(new MessageAdHoc(header, new UDPmsg(ownMacAddress, seqNum++, groupOwnerAddress.getHostAddress())),
                            groupOwnerAddress.getHostAddress(), 1000);


                }
            });
        }
    }

    private void timerClientMessage(final MessageAdHoc message,
                                    final String dest, final int time) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                // send Connect Reply Message
                sendConnectMessage(message, dest);

                UDPmsg udPmsg = (UDPmsg) message.getPdu();

                Log.d(TAG, "Timer client: " + (dest + "#" + udPmsg.getSeqNumber()) + " into set");

                if (ackSet.contains(dest + "#" + udPmsg.getSeqNumber())) {
                    Log.d(TAG, "Timer client contains " + (dest + "#" + udPmsg.getSeqNumber()) + " into set");
                    timerClientMessage(message, dest, time);
                }
                /*if (time < 30000) {
                    timerClientMessage(message, dest, time + time);
                }*/
            }
        }, time);
    }


    private void sendConnectMessage(final MessageAdHoc msg, final String address) {

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

    @Override
    public void stopListening() {
        udpPeers.setBackgroundRunning(false);
    }

    @Override
    public void sendMessage(final MessageAdHoc message, final String address) throws IOException {

    }

    @Override
    public boolean isDirectNeighbors(String address) {
        return neighbors.containsKey(address);
    }

    @Override
    public void broadcast(MessageAdHoc message) throws IOException {
//InetAddress.getByName("192.168.49.255")
    }

    @Override
    public void broadcastExcept(String originateAddr, MessageAdHoc message) throws IOException {
        /*for (Map.Entry<String, SimulateDevice> entry : peers.entrySet()) {
            if(!entry.getKey().equals(originateAddr)){
                sendMessage(message, entry.getValue().getAddr() + ":" + entry.getValue().getPort());
            }
        }*/
        //InetAddress.getByName("192.168.49.255")
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
            public void onDiscoveryCompleted(String deviceName, HashMap<String, WifiP2pDevice> peerslist) {
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


    class WifiAdHocDevice {
        private final String name;
        private final String ipAddress;
        private final String macAddress;

        WifiAdHocDevice(String name, String ipAddress, String macAddress) {
            this.name = name;
            this.ipAddress = ipAddress;
            this.macAddress = macAddress;
        }

        public String getName() {
            return name;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public String getMacAddress() {
            return macAddress;
        }

        @Override
        public String toString() {
            return "WifiAdHocDevice{" +
                    "name='" + name + '\'' +
                    ", ipAddress='" + ipAddress + '\'' +
                    ", macAddress='" + macAddress + '\'' +
                    '}';
        }
    }
}
