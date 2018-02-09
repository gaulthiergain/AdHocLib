package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.aodv.Aodv;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.Data;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.EntryRoutingTable;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.RERR;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.RREP;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.RREQ;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.TypeAodv;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class AutoManager {

    //Helper
    private final static int LOW = 24;
    private final static int END = 36;

    private final static int NB_THREAD = 8;
    private final static int ATTEMPTS = 3;

    private final boolean v;
    private final Context context;
    private final String TAG = "[AdHoc][AutoManager]";
    private ListenerGUI listenerGUI;

    private final String ownStringUUID;
    private final String ownName;
    private final String ownMac;

    private final Aodv aodv;
    private final AutoConnectionActives autoConnectionActives;
    private Timer timerRoutingTable;

    private BluetoothManager bluetoothManager;
    private HashMap<String, BluetoothAdHocDevice> hashMapDevices;

    private BluetoothServiceServer bluetoothServiceServer;


    public AutoManager(boolean v, Context context, UUID ownUUID) throws IOException, DeviceException {

        this.bluetoothManager = new BluetoothManager(true, context);

        this.v = v;
        this.context = context;
        this.autoConnectionActives = new AutoConnectionActives();
        this.hashMapDevices = new HashMap<>();
        // Take only the last part (24-36) to optimize the process
        this.ownStringUUID = ownUUID.toString().substring(LOW, END);
        this.ownName = BluetoothUtil.getCurrentName();
        this.ownMac = BluetoothUtil.getCurrentMac(context);

        this.listenServer(ownUUID);
        this.updateName();

        //this.timerRoutingTable = new Timer(); //todo decomment this
        //this.initTimer();

        this.aodv = new Aodv();
    }

    private void updateName() {
        //TODO update this
        //bluetoothManager.updateDeviceName(Code.ID_APP );
    }

    public void discovery(int duration) throws DeviceException, BluetoothBadDuration {

        // Create instance of Bluetooth Manager
        bluetoothManager = new BluetoothManager(true, context);

        // Check if Bluetooth is enabled
        if (!bluetoothManager.isEnabled()) {

            // If not, enable bluetooth and enable the discovery
            bluetoothManager.enable();
            bluetoothManager.enableDiscovery(duration);
        }

        // Add paired devices into the hashMapDevices
        for (Map.Entry<String, BluetoothAdHocDevice> entry : bluetoothManager.getPairedDevices().entrySet()) {
            if (entry.getValue().getDevice().getName().contains(Code.ID_APP)) {
                hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                if (v) Log.d(TAG, "Add paired " + entry.getValue().getShortUuid()
                        + " into hashMapDevices");
            }
        }

        // Start the discovery process
        bluetoothManager.discovery(new DiscoveryListener() {
            @Override
            public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                // Add no paired devices into the hashMapDevices
                for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
                    if (entry.getValue().getDevice().getName() != null &&
                            entry.getValue().getDevice().getName().contains(Code.ID_APP)) {
                        hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                        if (v) Log.d(TAG, "Add no paired " + entry.getValue().getShortUuid()
                                + " into hashMapDevices");
                    }
                }
                // Stop and unregister to the discovery process
                bluetoothManager.unregisterDiscovery();

                // Execute onDiscoveryCompleted in the GUI
                if (listenerGUI != null) {
                    listenerGUI.onDiscoveryCompleted(hashMapBluetoothDevice);
                }
            }

            @Override
            public void onDiscoveryStarted() {
                // Execute onDiscoveryStarted in the GUI
                if (listenerGUI != null) {
                    listenerGUI.onDiscoveryStarted();
                }
            }

            @Override
            public void onDeviceFound(BluetoothDevice device) {
                // Execute onDeviceFound in the GUI
                if (listenerGUI != null) {
                    listenerGUI.onDeviceFound(device);
                }
            }

            @Override
            public void onScanModeChange(int currentMode, int oldMode) {
                // Execute onScanModeChange in the GUI
                if (listenerGUI != null) {
                    listenerGUI.onScanModeChange(currentMode, oldMode);
                }
            }
        });
    }

    public void connect() {
        for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapDevices.entrySet()) {
            if (!autoConnectionActives.getActivesConnections().containsKey(entry.getValue().getShortUuid())) {
                //TODO remove
                if (ownName.equals("#eO91#SamsungGT3") && entry.getValue().getDevice().getName().equals("#e091#Samsung_gt")) {

                } else {
                    _connect(entry.getValue());
                }
            } else {
                if (v) Log.d(TAG, entry.getValue().getShortUuid() + " is already connected");
            }
        }
    }

    private void _connect(final BluetoothAdHocDevice bluetoothAdHocDevice) {
        final BluetoothServiceClient bluetoothServiceClient = new BluetoothServiceClient(true, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoConnectionException e) {
                    e.printStackTrace();
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

                if (v) Log.d(TAG, "Link broken with " + deviceAddress);

                // Get the remote UUID
                String remoteUuid = deviceAddress.replace(":", "").toLowerCase();

                // Remove remote connections
                if (autoConnectionActives.getActivesConnections().containsKey(remoteUuid)) {
                    if (v) Log.d(TAG, "Remove active connection with " + remoteUuid);
                    NetworkObject networkObject = autoConnectionActives.getActivesConnections().get(remoteUuid);
                    autoConnectionActives.getActivesConnections().remove(remoteUuid);
                    networkObject.closeConnection();
                }

                // Remote entry from routing table
                if (aodv.getRoutingTable().containsDest(remoteUuid)) {
                    if (v) Log.d(TAG, "Remove " + remoteUuid + " entry from RIB");
                    aodv.getRoutingTable().removeEntry(remoteUuid);
                }

                // Send RRER
                if (aodv.getRoutingTable().getRoutingTable().size() > 0) {
                    if (v) Log.d(TAG, "Send RRER ");
                    try {
                        sendRRER(remoteUuid);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoConnectionException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                if (v) Log.d(TAG, "Connected to server: " + deviceAddress + " - " + deviceName);

                // Execute onConnection in the GUI
                if (listenerGUI != null) {
                    listenerGUI.onConnection(deviceName, deviceAddress, localAddress);
                }
            }
        }, true, true, ATTEMPTS, bluetoothAdHocDevice);

        bluetoothServiceClient.setListenerAutoConnect(new ListenerAutoConnect() {
            @Override
            public void connected(UUID uuid, NetworkObject network) throws IOException, NoConnectionException {

                // Add the active connection into the autoConnectionActives object
                autoConnectionActives.addConnection(uuid.toString().substring(LOW, END).toLowerCase()
                        , network);

                // Send CONNECT message to establish the pairing
                bluetoothServiceClient.send(new MessageAdHoc(
                        new Header("CONNECT", ownMac, ownName), ownStringUUID));

            }
        });

        // Start the bluetoothServiceClient thread
        new Thread(bluetoothServiceClient).start();
    }

    public void stopListening() throws IOException {
        bluetoothServiceServer.stopListening();
    }

    private void listenServer(UUID ownUUID) throws IOException {
        bluetoothServiceServer = new BluetoothServiceServer(true, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoConnectionException e) {
                    e.printStackTrace();
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
                if (v) Log.d(TAG, "onConnectionClosed");

                String remoteUuid = "";
                NetworkObject networkObject = bluetoothServiceServer.getActiveConnexion().get(deviceAddress);
                if (networkObject != null) {
                    for (Map.Entry<String, NetworkObject> entry : autoConnectionActives.getActivesConnections().entrySet()) {
                        if (entry.getValue().equals(networkObject)) {
                            remoteUuid = entry.getKey();
                            if (v) Log.d(TAG, "Link broken with " + remoteUuid);
                            break;
                        }
                    }

                    // Remove remote connections
                    if (autoConnectionActives.getActivesConnections().containsKey(remoteUuid)) {
                        if (v) Log.d(TAG, "Remove active connection with " + remoteUuid);
                        autoConnectionActives.getActivesConnections().remove(remoteUuid);
                    }

                    if (aodv.getRoutingTable().containsDest(remoteUuid)) {
                        if (v) Log.d(TAG, "Remote " + remoteUuid + " from RIB");
                        aodv.getRoutingTable().removeEntry(remoteUuid);
                    }

                    if (aodv.getRoutingTable().getRoutingTable().size() > 0) {
                        if (v) Log.d(TAG, "Send RRER ");
                        try {
                            sendRRER(remoteUuid);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (NoConnectionException e) {
                            e.printStackTrace();
                        }
                    }

                    networkObject.closeConnection();
                    networkObject = null;
                } else {
                    if (v) Log.e(TAG, "onConnectionClosed >>> Not Found");
                }

            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                if (v) Log.d(TAG, "Connected to client: " + deviceAddress);

                // Execute onConnection in the GUI
                if (listenerGUI != null) {
                    listenerGUI.onConnection(deviceName, deviceAddress, localAddress);
                }

            }
        });

        // Start the bluetoothServiceServer listening process
        bluetoothServiceServer.listen(NB_THREAD, true, "secure",
                BluetoothAdapter.getDefaultAdapter(), ownUUID);

    }

    private void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException {
        Log.d(TAG, "Message received: " + message.getPdu().toString());
        switch (message.getHeader().getType()) {
            case "CONNECT":
                NetworkObject networkObject = bluetoothServiceServer.getActiveConnexion().get(message.getHeader().getSenderAddr());
                if (networkObject != null) {
                    // Add the active connection into the autoConnectionActives object
                    autoConnectionActives.addConnection((String) message.getPdu(), networkObject);
                }
                break;
            case "RREP":
                processRREP(message);
                break;
            case "RREQ":
                processRREQ(message);
                break;
            case "RERR":
                processRERR(message);
                break;
            case "DATA":
                processData(message);
                break;
            case "DATA_ACK":
                processDataAck(message);
                break;
            default:
                if (v) Log.e(TAG, "Default Message");
        }
    }

    public void sendMessage(String address, Serializable serializable) throws IOException, NoConnectionException {

        // Create MessageAdHoc object
        Header header = new Header(TypeAodv.DATA.getCode(), ownStringUUID, ownName);
        MessageAdHoc msg = new MessageAdHoc(header, new Data(address, serializable));

        // Send message to remote device
        send(msg, address);
    }

    private void send(MessageAdHoc msg, String address) throws IOException, NoConnectionException {

        if (autoConnectionActives.getActivesConnections().containsKey(address)) {
            // The destination is directly connected
            NetworkObject networkObject = autoConnectionActives.getActivesConnections().get(address);
            networkObject.sendObjectStream(msg);
            if (v) Log.d(TAG, "Send to " + address);

        } else if (aodv.containsDest(address)) {
            // The destination learned from neighbors -> send to next by checking the routing table
            EntryRoutingTable destNext = aodv.getNextfromDest(address);
            if (destNext == null) {
                if (v) Log.d(TAG, "No destNext found in the routing Table for " + address);
            } else {

                if (v) Log.d(TAG, "Routing table contains " + destNext.getNext());
                // Update the connexion
                autoConnectionActives.updateDataPath(address);
                // Send message to remote device
                send(msg, destNext.getNext());
            }
        } else if (msg.getHeader().getType().equals(TypeAodv.RERR.getCode())) {
            if (v) Log.d(TAG, ">>>>>RERR");
            // TODO change message displayed here
        } else {
            startTimerRREQ(address, Aodv.RREQ_RETRIES);
        }
    }

    private void broadcastMsg(MessageAdHoc msg) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : autoConnectionActives.getActivesConnections().entrySet()) {
            entry.getValue().sendObjectStream(msg);
            if (v) Log.d(TAG, "Broadcast Message to " + entry.getKey());
        }
    }

    private void broadcastMsgExcept(MessageAdHoc message, String address) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : autoConnectionActives.getActivesConnections().entrySet()) {
            if (!entry.getKey().equals(address)) {
                entry.getValue().sendObjectStream(message);
                if (v) Log.d(TAG, "Broadcast Message to " + entry.getKey());
            }
        }
    }

    private void processRREQ(MessageAdHoc msg) throws IOException, NoConnectionException {

        // Get the RREQ message
        RREQ rreq = (RREQ) msg.getPdu();

        // Get previous hop and previous source address
        int hop = rreq.getHopCount();
        String originateAddr = msg.getHeader().getSenderAddr();

        if (v) Log.d(TAG, "Received RREQ from " + originateAddr);

        if (rreq.getDestIpAddress().equals(ownStringUUID)) {
            if (v) Log.d(TAG, ownStringUUID + " is the destination (stop RREQ broadcast)");

            // Update routing table
            EntryRoutingTable entry = aodv.addEntryRoutingTable(rreq.getOriginIpAddress(),
                    originateAddr, hop, rreq.getOriginSeqNum());
            if (entry != null) {

                // Generate RREP
                RREP rrep = new RREP(TypeAodv.RREP.getType(), Aodv.INIT_HOP_COUNT, rreq.getOriginIpAddress(),
                        1, ownStringUUID, Aodv.LIFE_TIME);

                if (v) Log.d(TAG, "Destination reachable via " + entry.getNext());

                // Send message to the next destination
                send(new MessageAdHoc(new Header(TypeAodv.RREP.getCode(), ownStringUUID, ownName), rrep),
                        entry.getNext());
            }
        } else {
            if (aodv.addBroadcastId(rreq.getOriginIpAddress() + rreq.getRreqId())) {
                try {

                    // Update PDU and Header
                    rreq.incrementHopCount();
                    msg.setPdu(rreq);
                    msg.setHeader(new Header(TypeAodv.RREQ.getCode(), ownStringUUID, ownName));

                    // Broadcast message to all directly connected devices
                    broadcastMsgExcept(msg, originateAddr);

                    // Update routing table
                    EntryRoutingTable entry = aodv.addEntryRoutingTable(rreq.getOriginIpAddress(),
                            originateAddr, hop, rreq.getOriginSeqNum());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                if (v) Log.d(TAG, "Already received this RREQ from " + rreq.getOriginIpAddress());
            }
        }
    }

    private void processRREP(MessageAdHoc msg) throws IOException, NoConnectionException {

        // Get the RREP message
        RREP rrep = (RREP) msg.getPdu();

        // Get previous hop and previous source address
        int hopRcv = rrep.getHopCount();
        String originateAddrRcv = msg.getHeader().getSenderAddr();

        if (v) Log.d(TAG, "Received RREP from " + originateAddrRcv);

        if (rrep.getDestIpAddress().equals(ownStringUUID)) {
            if (v) Log.d(TAG, ownStringUUID + " is the destination (stop RREP)");
            //todo boolean with timer to manage the LIFE TIME of the entry
        } else {
            // Forward the RREP message to the destination by checking the routing table
            EntryRoutingTable destNext = aodv.getNextfromDest(rrep.getDestIpAddress());
            if (destNext == null) {
                if (v) Log.d(TAG, "No destNext found in the routing Table for "
                        + rrep.getDestIpAddress());
            } else {
                if (v) Log.d(TAG, "Destination reachable via " + destNext.getNext());
                // Increment HopCount and send message to the next destination
                rrep.incrementHopCount();
                send(new MessageAdHoc(new Header(TypeAodv.RREP.getCode(), ownStringUUID, ownName)
                        , rrep), destNext.getNext());

            }
        }

        // Update routing table
        EntryRoutingTable entryRRep = aodv.addEntryRoutingTable(rrep.getOriginIpAddress(),
                originateAddrRcv, hopRcv, rrep.getDestSeqNum());
    }

    private void processRERR(MessageAdHoc msg) throws IOException {

        // Get the RERR message
        RERR rerr = (RERR) msg.getPdu();

        // Get previous source address
        String originateAddr = msg.getHeader().getSenderAddr();

        Log.d(TAG, "Received RERR from " + originateAddr + " -> Node " +
                rerr.getUnreachableDestIpAddress() + " is unreachable");

        if (rerr.getUnreachableDestIpAddress().equals(ownStringUUID)) {
            Log.d(TAG, "RERR received on the destination (stop forward)");
        } else if (aodv.containsDest(rerr.getUnreachableDestIpAddress())) {
            aodv.getRoutingTable().removeEntry(rerr.getUnreachableDestIpAddress());
            msg.setHeader(new Header(TypeAodv.RERR.getCode(), ownStringUUID, ownName));
            // Broadcast message to all directly connected devices
            broadcastMsgExcept(msg, originateAddr);
        } else {
            Log.d(TAG, "Node doesn't contain dest: " + rerr.getUnreachableDestIpAddress());
        }
    }

    private void processData(MessageAdHoc msg) throws IOException, NoConnectionException {

        // Get the DATA message
        Data data = (Data) msg.getPdu();

        // Update the data path
        autoConnectionActives.updateDataPath(data.getDestIpAddress());

        if (v) Log.d(TAG, "Data message received from: " + msg.getHeader().getSenderAddr());

        if (data.getDestIpAddress().equals(ownStringUUID)) {
            if (v) Log.d(TAG, ownStringUUID + " is the destination (stop DATA message " +
                    "and send ACK)");

            // Update PDU and Header
            String destinationAck = msg.getHeader().getSenderAddr();
            msg.setPdu(new Data(destinationAck, TypeAodv.DATA_ACK.getCode()));
            msg.setHeader(new Header(TypeAodv.DATA_ACK.getCode(), ownStringUUID, ownName));

            // Send message to the destination
            send(msg, destinationAck);
        } else {
            // Forward the DATA message to the destination by checking the routing table
            EntryRoutingTable destNext = aodv.getNextfromDest(data.getDestIpAddress());
            if (destNext == null) {
                if (v) Log.d(TAG, "No destNext found in the routing Table for " +
                        data.getDestIpAddress());
            } else {
                if (v) Log.d(TAG, "Destination reachable via " + destNext.getNext());
                // Send message to the next destination
                send(msg, destNext.getNext());
            }
        }
    }

    private void processDataAck(MessageAdHoc msg) throws IOException, NoConnectionException {

        // Get the ACK message
        Data dataAck = (Data) msg.getPdu();

        // Update the data path
        autoConnectionActives.updateDataPath(dataAck.getDestIpAddress());

        if (v) Log.d(TAG, "ACK message received from: " + msg.getHeader().getSenderAddr());

        if (dataAck.getDestIpAddress().equals(ownStringUUID)) {
            if (v) Log.d(TAG, ownStringUUID + " is the destination (stop data-ack message)");
        } else {
            // Forward the ACK message to the destination by checking the routing table
            EntryRoutingTable destNext = aodv.getNextfromDest(dataAck.getDestIpAddress());
            if (destNext == null) {
                if (v)
                    Log.d(TAG, "No destNext found in the routing Table for " +
                            dataAck.getDestIpAddress());
            } else {
                if (v) Log.d(TAG, "Destination reachable via " + destNext.getNext());
                // Send message to the next destination
                send(msg, destNext.getNext());
            }
        }
    }

    private void startTimerRREQ(final String address, final int retry) throws IOException {

        // No destination was found, send RREQ request (with timer)
        Log.d(TAG, "No connection to " + address + "-> send RREQ message");

        // Broadcast message to all directly connected devices
        broadcastMsg(new MessageAdHoc(new Header(TypeAodv.RREQ.getCode(), ownStringUUID, ownName),
                new RREQ(TypeAodv.RREQ.getType(), Aodv.INIT_HOP_COUNT, aodv.getIncrementRreqId(), address,
                        1, ownStringUUID, 1)));

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {

                EntryRoutingTable entry = aodv.getNextfromDest(address);
                if (entry != null) {
                    //test seq num here todo
                } else {
                    if (retry == 0) {
                        if (v) Log.d(TAG, "Expired time: no RREP received for " + address);
                        //todo event here
                    } else {
                        if (v) Log.d(TAG, "Expired time: no RREP received for " + address +
                                " Retry: " + retry);

                        try {
                            startTimerRREQ(address, retry - 1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, Aodv.NET_TRANVERSAL_TIME);
    }

    private void sendRRER(String remoteUuid) throws IOException, NoConnectionException {

        if (aodv.getRoutingTable().containsNext(remoteUuid)) {
            String dest = aodv.getRoutingTable().getDestFromNext(remoteUuid);

            if (dest.equals(ownStringUUID)) {
                if (v) Log.d(TAG, "RERR received on the destination (stop forward)");
            } else {
                // Remove the destination from Routing table
                aodv.getRoutingTable().removeEntry(dest);
                // Send RERR message
                RERR rrer = new RERR(TypeAodv.RERR.getType(), 0, dest, 1);
                for (Map.Entry<String, NetworkObject> entry : autoConnectionActives.getActivesConnections().entrySet()) {
                    send(new MessageAdHoc(
                                    new Header(TypeAodv.RERR.getCode(), ownStringUUID, ownName), rrer),
                            entry.getKey());
                }
            }
        }
    }

    public void setListenerGUI(ListenerGUI listenerGUI) {
        this.listenerGUI = listenerGUI;
    }
}
