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
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.TypeAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DataLinkWifiManager implements IDataLink {


    public static final String ID_APP = "#e091#";
    private static final String TAG = "[AdHoc][DataLinkWifi]";
    private final boolean v;
    private final Context context;

    private String ownAddress;
    private final String ownName;
    private final int serverPort;
    private final HashMap<String, WifiP2pDevice> neighbors;
    private final HashMap<String, Long> helloMessages;
    private final ListenerAodv listenerAodv;
    private final ListenerDataLinkAodv listenerDataLinkAodv;

    private final WifiManager wifiManager;

    private UdpPeers udpPeers;

    /**
     * Constructor
     *
     * @param verbose              a boolean value to set the debug/verbose mode.
     * @param context              a Context object which gives global information about an application
     *                             environment.
     * @param ownName              a String which represents the name of the device.
     * @param ownAddress           a String which represents the address of the device.
     * @param listenerAodv         a ListenerAodv object which serves as callback functions.
     * @param listenerDataLinkAodv a ListenerDataLinkAodv object which serves as callback functions.
     */
    public DataLinkWifiManager(boolean verbose, Context context, String ownName, String ownAddress,
                               int serverPort, ListenerAodv listenerAodv,
                               ListenerDataLinkAodv listenerDataLinkAodv) throws DeviceException {
        this.v = verbose;
        this.context = context;
        this.ownName = ownName;
        this.ownAddress = ownAddress;
        this.serverPort = serverPort;
        this.listenerAodv = listenerAodv;
        this.listenerDataLinkAodv = listenerDataLinkAodv;
        this.wifiManager = new WifiManager(v, context);
        this.neighbors = new HashMap<>();
        this.helloMessages = new HashMap<>();
        this.init();
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
     * Method allowing to launch the timer to send HELLO messages between neighbors every TIME (ms).
     *
     * @param time an integer value which represents the period of the timer.
     */
    private void timerHello(final int time) {
        Timer timerHelloPackets = new Timer();
        timerHelloPackets.schedule(new TimerTask() {
            @Override
            public void run() {
                /*for (Map.Entry<String, WifiP2pDevice> entry : neighbors.entrySet()) {
                    Header header = new Header(TypeAodv.HELLO.getCode(), ownAddress, ownName);
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

                // Check neighbors
                Iterator<Map.Entry<String, Long>> iter = helloMessages.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, Long> entry = iter.next();
                    long upTime = (System.currentTimeMillis() - entry.getValue());
                    if (upTime > Constants.HELLO_PACKET_INTERVAL_SND) {
                        // Remove the neighbors and send RRER packets
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
     * @throws AodvUnknownTypeException Signals that a Unknown route has found.
     */
    private void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {
        switch (message.getHeader().getType()) {
            case "CONNECT":
                message.setHeader(new Header("CONNECT_REPLY", ownAddress, "Group Owner"));
                udpPeers.sendMessageTo(message, InetAddress.getByName(message.getPdu().toString()), serverPort);
                break;
            case "CONNECT_REPLY":
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
        for (Map.Entry<String, WifiP2pDevice> deviceEntry : neighbors.entrySet()) {
            Log.d(TAG, "Remote Address" + deviceEntry.getValue().deviceAddress);
            wifiManager.connect(deviceEntry.getValue().deviceAddress, new ConnectionListener() {
                @Override
                public void onConnectionStarted() {
                    Log.d(TAG, "Connection Started");
                    /*String ip = "192.168.49.1";
                    try {
                        sendConnectMessage(InetAddress.getByAddress(ip.getBytes()));
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }*/
                }

                @Override
                public void onConnectionFailed(int reasonCode) {
                    Log.d(TAG, "Connection Failed: " + reasonCode);
                }

                @Override
                public void onGroupOwner(InetAddress groupOwnerAddress) {
                    Log.d(TAG, "onGroupOwner: " + groupOwnerAddress.toString());
                    ownAddress = groupOwnerAddress.toString();
                }

                @Override
                public void onClient(final InetAddress groupOwnerAddress) {
                    Log.d(TAG, "onClient serverAddress: " + groupOwnerAddress.toString());
                    sendConnectMessage(groupOwnerAddress);
                }
            });
        }
    }

    private void sendConnectMessage(final InetAddress groupOwnerAddress){
        Header header = new Header("CONNECT", "", "");
        final MessageAdHoc msg = new MessageAdHoc(header, "");
        // Send CONNECT message to group owner
        new Thread(new Runnable() {
            @Override
            public void run() {
                udpPeers.sendMessageTo(msg, groupOwnerAddress, serverPort);
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
        /*for (Map.Entry<String, SimulateDevice> entry : neighbors.entrySet()) {
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
            public void onDiscoveryCompleted(HashMap<String, WifiP2pDevice> peers) {
                // Add no paired devices into the hashMapDevices
                for (Map.Entry<String, WifiP2pDevice> entry : peers.entrySet()) {
                    if (entry.getValue().deviceName != null &&
                            entry.getValue().deviceName.contains(DataLinkBtManager.ID_APP)) {
                        neighbors.put(entry.getValue().deviceAddress, entry.getValue());
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
