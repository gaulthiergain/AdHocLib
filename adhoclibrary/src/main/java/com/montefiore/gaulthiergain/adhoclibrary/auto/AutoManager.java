package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.montefiore.gaulthiergain.adhoclibrary.aodv.Aodv;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.Data;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.EntryRoutingTable;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoManager {

    private final static int DURATION = 10;
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

    private BluetoothManager bluetoothManager;
    private HashMap<String, BluetoothAdHocDevice> hashMapDevices;

    private BluetoothServiceServer bluetoothServiceServer;


    public AutoManager(boolean v, Context context, UUID ownUUID) {
        try {
            this.bluetoothManager = new BluetoothManager(true, context);
        } catch (DeviceException e) {
            e.printStackTrace();
        }

        this.v = v;
        this.context = context;
        this.autoConnectionActives = new AutoConnectionActives();
        this.hashMapDevices = new HashMap<>();
        this.ownStringUUID = ownUUID.toString();
        this.ownName = BluetoothUtil.getCurrentName();
        this.ownMac = BluetoothUtil.getCurrentMac(context);

        this.listenServer(ownUUID);
        this.updateName();

        this.aodv = new Aodv();
    }

    private void updateName() {
        //TODO update this
        //bluetoothManager.updateDeviceName(Code.ID_APP );
    }

    public void discovery() {
        try {
            bluetoothManager = new BluetoothManager(true, context);
        } catch (DeviceException e) {
            e.printStackTrace();
        }

        if (!bluetoothManager.isEnabled()) {

            // Enable bluetooth and enable the discovery
            try {
                bluetoothManager.enable();
                bluetoothManager.enableDiscovery(DURATION);
            } catch (BluetoothBadDuration bluetoothBadDuration) {
                bluetoothBadDuration.printStackTrace();
            }
        }

        // Paired devices
        for (Map.Entry<String, BluetoothAdHocDevice> entry : bluetoothManager.getPairedDevices().entrySet()) {
            if (entry.getValue().getDevice().getName().contains(Code.ID_APP)) {
                hashMapDevices.put(entry.getValue().getUuid(), entry.getValue());
                if (v) Log.d(TAG, "Add paired " + entry.getValue().getUuid() + " into Hashmap");
            }
        }

        // Start Discovery
        bluetoothManager.discovery(new DiscoveryListener() {
            @Override
            public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
                    if (entry.getValue().getDevice().getName() != null &&
                            entry.getValue().getDevice().getName().contains(Code.ID_APP)) {
                        hashMapDevices.put(entry.getValue().getUuid(), entry.getValue());
                        if (v)
                            Log.d(TAG, "Add not paired" + entry.getValue().getUuid() + " into Hashmap");
                    }
                }
                bluetoothManager.unregisterDiscovery();
                if (listenerGUI != null) {
                    // Listener on GUI
                    listenerGUI.onDiscoveryCompleted(hashMapBluetoothDevice);
                }
            }

            @Override
            public void onDiscoveryStarted() {

            }

            @Override
            public void onDeviceFound(BluetoothDevice device) {
                Toast.makeText(context, "New devices found: " + device.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onScanModeChange(int currentMode, int oldMode) {

            }
        });
    }

    public void connect() {
        for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapDevices.entrySet()) {

            if (!autoConnectionActives.getActivesConnections().containsKey(entry.getValue().getUuid())) {
                //TODO remove
                if (ownName.equals("#eO91#Huawei") && entry.getValue().getDevice().getName().equals("#e091#Samsung_gt")) {

                } else {
                    _connect(entry.getValue());
                }
            } else {
                if (v) Log.d(TAG, entry.getValue().getUuid() + " is already connected");
            }
        }
    }

    private void _connect(final BluetoothAdHocDevice bluetoothAdHocDevice) {
        final BluetoothServiceClient bluetoothServiceClient = new BluetoothServiceClient(true, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                processMsgReceived(message);
            }

            @Override
            public void onMessageSent(MessageAdHoc message) {
                Log.d(TAG, "Message sent: " + message.getPdu().toString());
            }

            @Override
            public void onForward(MessageAdHoc message) {
                Log.d(TAG, "OnForward: " + message.getPdu().toString());
            }

            @Override
            public void onConnectionClosed(String deviceName, String deviceAddress) {

                Log.d(TAG, "Link broken with " + deviceAddress);

                //remove remote connections
                /*
                if (autoConnectionActives.getActivesConnections().containsKey(remoteUuid)) {
                    Log.d(TAG, "Remove active connection with " + remoteUuid.substring(LOW, END));
                    NetworkObject networkObject = autoConnectionActives.getActivesConnections().get(remoteUuid);
                    autoConnectionActives.getActivesConnections().remove(remoteUuid);
                    networkObject.closeConnection();
                    networkObject = null;
                }

                if (aodv.getRoutingTable().containsDest(remoteUuid)) {
                    Log.traceAodv(TAG, "Remove " + remoteUuid.substring(LOW, END) + " entry from RIB");
                    aodv.getRoutingTable().removeEntry(remoteUuid);
                }

                if (aodv.getRoutingTable().getRoutingTable().size() > 0) {
                    Log.traceAodv(TAG, "Send RRER ");
                    sendRRER(remoteUuid);
                }*/
            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                Log.d(TAG, "Connected to server: " + deviceAddress + " - " + deviceName);
                if (listenerGUI != null) {
                    listenerGUI.onConnection(deviceName, deviceAddress, localAddress);
                }
            }
        }, true, true, ATTEMPTS, bluetoothAdHocDevice);

        bluetoothServiceClient.setListenerAutoConnect(new ListenerAutoConnect() {
            @Override
            public void connected(UUID uuid, NetworkObject network) {
                autoConnectionActives.addConnection(uuid.toString().toLowerCase(), network);
                try {
                    bluetoothServiceClient.send(new MessageAdHoc(
                            new Header("CONNECT", ownMac, ownName), ownStringUUID));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoConnectionException e) {
                    e.printStackTrace();
                }
            }
        });
        new Thread(bluetoothServiceClient).start();
    }

    public void stopListening() throws IOException {
        bluetoothServiceServer.stopListening();
    }

    private void listenServer(UUID ownUUID) {
        bluetoothServiceServer = new BluetoothServiceServer(true, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                processMsgReceived(message);
            }

            @Override
            public void onMessageSent(MessageAdHoc message) {
                Log.d(TAG, "Message sent: " + message.getPdu().toString());
            }

            @Override
            public void onForward(MessageAdHoc message) {
                Log.d(TAG, "OnForward: " + message.getPdu().toString());
            }

            @Override
            public void onConnectionClosed(String deviceName, String deviceAddress) {
                Log.d(TAG, "onConnectionClosed");
                String remoteUuid = "";
                NetworkObject networkObject = bluetoothServiceServer.getActiveConnexion().get(deviceAddress);
                if (networkObject != null) {
                    for (Map.Entry<String, NetworkObject> entry : autoConnectionActives.getActivesConnections().entrySet()) {
                        if (entry.getValue().equals(networkObject)) {
                            remoteUuid = entry.getKey();
                            Log.d(TAG, "Link broken with " + remoteUuid);
                        }
                    }
                    //remove remote connections
                    if (autoConnectionActives.getActivesConnections().containsKey(remoteUuid)) {
                        Log.d(TAG, "Remote active connection with " + remoteUuid);
                        autoConnectionActives.getActivesConnections().remove(remoteUuid);
                    }

                    /*if (aodv.getRoutingTable().containsDest(remoteUuid)) {
                        Log.traceAodv(TAG, "Remote " + remoteUuid.substring(LOW, END) + " from RIB");
                        aodv.getRoutingTable().removeEntry(remoteUuid);
                    }

                    if (aodv.getRoutingTable().getRoutingTable().size() > 0) {
                        Log.traceAodv(TAG, "Send RRER ");
                        sendRRER(remoteUuid);
                    }*/

                    networkObject.closeConnection();
                    networkObject = null;
                } else {
                    Log.e(TAG, "onConnectionClosed >>> Not Found");
                }

            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                Log.d(TAG, "Connected to client: " + deviceAddress);
                if (listenerGUI != null) {
                    listenerGUI.onConnection(deviceName, deviceAddress, localAddress);
                }

            }
        });
        try {
            bluetoothServiceServer.listen(NB_THREAD, true, "secure",
                    BluetoothAdapter.getDefaultAdapter(), ownUUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processMsgReceived(MessageAdHoc message) {
        Log.d(TAG, "Message received: " + message.getPdu().toString());
        switch (message.getHeader().getType()) {
            case "CONNECT":
                NetworkObject networkObject = bluetoothServiceServer.getActiveConnexion().get(message.getHeader().getSenderAddr());
                if (networkObject != null) {
                    String remoteUUID = (String) message.getPdu();
                    autoConnectionActives.addConnection(remoteUUID, networkObject);
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
                Log.e(TAG, "DEFAULT MSG");
        }
    }

    public void sendMessage(String uuid, String message) {

        Header header = new Header(TypeAodv.DATA.getCode(), ownStringUUID, ownName);
        MessageAdHoc messageAdHoc = new MessageAdHoc(header, new Data(uuid, message));

        try {
            send(messageAdHoc, uuid);
        } catch (IOException | NoConnectionException e) {
            e.printStackTrace();
        }
    }

    private void send(MessageAdHoc messageAdHoc, String uuidString) throws IOException, NoConnectionException {

        String remoteDeviceName = "";
        if (hashMapDevices.containsKey(uuidString)) {
            remoteDeviceName = hashMapDevices.get(uuidString).getDevice().getName();
        }

        if (autoConnectionActives.getActivesConnections().containsKey(uuidString)) {
            // Destinations directly connected
            NetworkObject networkObject = autoConnectionActives.getActivesConnections().get(uuidString);
            networkObject.sendObjectStream(messageAdHoc);
            Log.d(TAG, "Send to " + uuidString + " (" + remoteDeviceName + ")");
        } else if (aodv.containsDest(uuidString)) {
            // Destinations learned from neighbors -> send to next by checking the routing table
            EntryRoutingTable destNext = aodv.getNextfromDest(uuidString);
            if (destNext == null) {
                Log.d(TAG, "No destNext found in the routing Table for " + uuidString
                        + " (" + remoteDeviceName + ")");
            } else {
                try {
                    Log.d(TAG, "Routing table contains " + destNext.getNext()
                            + " (" + remoteDeviceName + ")");

                    // Update the connexion
                    autoConnectionActives.updateDataPath(uuidString);

                    send(messageAdHoc, destNext.getNext());
                } catch (IOException | NoConnectionException e) {
                    e.printStackTrace();
                }
            }
        } else if (messageAdHoc.getHeader().getType().equals(TypeAodv.RERR.getCode())) {
            Log.d(TAG, ">>>>>RERR");
        } else {
            //RREQ
            //TODO startTimerRREQ(uuid, Aodv.RREQ_RETRIES);
            Log.d(TAG, "No connection to " + uuidString + "-> send RREQ message");
            broadcastMsg(new MessageAdHoc(new Header(TypeAodv.RREQ.getCode(), ownStringUUID, ownName),
                    new RREQ(TypeAodv.RREQ.getType(), Aodv.INIT_HOP_COUNT, aodv.getIncrementRreqId(), uuidString,
                            1, ownStringUUID, 1)));
        }
    }

    private void broadcastMsg(MessageAdHoc messageAdHoc) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : autoConnectionActives.getActivesConnections().entrySet()) {
            entry.getValue().sendObjectStream(messageAdHoc);
            Log.d(TAG, "Broadcast Message to " + entry.getKey());
        }
    }

    private void broadcastMsgExcept(MessageAdHoc messageAdHoc, String address) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : autoConnectionActives.getActivesConnections().entrySet()) {
            if (!entry.getKey().equals(address)) {
                entry.getValue().sendObjectStream(messageAdHoc);
                Log.d(TAG, "Broadcast Message to " + entry.getKey());
            }
        }
    }

    private void processRREQ(MessageAdHoc message) {
        RREQ rreq = (RREQ) message.getPdu();
        // Get previous hop
        int hop = rreq.getHopCount();
        // Get previous src
        String originateAddr = message.getHeader().getSenderAddr();

        Log.d(TAG, "Received RREQ from " + originateAddr);

        if (rreq.getDestIpAddress().equals(ownStringUUID)) {
            Log.d(TAG, ownStringUUID + " is the destination (stop RREQ broadcast)");

            //Update routing table
            EntryRoutingTable entry = aodv.addEntryRoutingTable(rreq.getOriginIpAddress(),
                    originateAddr, hop, rreq.getOriginSeqNum());
            if (entry != null) {

                //Generate RREP
                RREP rrep = new RREP(TypeAodv.RREP.getType(), Aodv.INIT_HOP_COUNT, rreq.getOriginIpAddress(),
                        1, ownStringUUID, Aodv.LIFE_TIME);

                try {
                    Log.d(TAG, "Destination reachable via " + entry.getNext());

                    send(new MessageAdHoc(new Header(TypeAodv.RREP.getCode(), ownStringUUID, ownName), rrep),
                            entry.getNext());
                } catch (IOException | NoConnectionException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (aodv.addBroadcastId(rreq.getOriginIpAddress() + rreq.getRreqId())) {
                try {
                    Log.d(TAG, "Received RREQ from " + rreq.getOriginIpAddress());

                    rreq.incrementHopCount();
                    message.setPdu(rreq);

                    message.setHeader(new Header(TypeAodv.RREQ.getCode(), ownStringUUID, ownName));

                    broadcastMsgExcept(message, originateAddr);

                    //Update routing table
                    EntryRoutingTable entry = aodv.addEntryRoutingTable(rreq.getOriginIpAddress(),
                            originateAddr, hop, rreq.getOriginSeqNum());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "Already received this RREQ from " + rreq.getOriginIpAddress());
            }
        }
    }

    private void processRREP(MessageAdHoc message) {
        RREP rrepRcv = (RREP) message.getPdu();
        // Get previous hop
        int hopRcv = rrepRcv.getHopCount();
        // Get previous src
        String originateAddrRcv = message.getHeader().getSenderAddr();

        Log.d(TAG, "Received RREP from " + originateAddrRcv);
        if (rrepRcv.getDestIpAddress().equals(ownStringUUID)) {
            Log.d(TAG, ownStringUUID + " is the destination (stop RREP)");
            //todo boolean with timer to manage the LIFE TIME of the entry
        } else {
            //Forward message depending the next entry on the routing table
            EntryRoutingTable destNext = aodv.getNextfromDest(rrepRcv.getDestIpAddress());
            if (destNext == null) {
                Log.d(TAG, "No destNext found in the routing Table for "
                        + rrepRcv.getDestIpAddress());
            } else {
                Log.d(TAG, "Destination reachable via " + destNext.getNext());
                try {
                    rrepRcv.incrementHopCount();
                    send(new MessageAdHoc(new Header(TypeAodv.RREP.getCode(), ownStringUUID, ownName)
                            , rrepRcv), destNext.getNext());
                } catch (IOException | NoConnectionException e) {
                    e.printStackTrace();
                }
            }
        }

        //Update routing table
        EntryRoutingTable entryRRep = aodv.addEntryRoutingTable(rrepRcv.getOriginIpAddress(),
                originateAddrRcv, hopRcv, rrepRcv.getDestSeqNum());
    }

    private void processRERR(MessageAdHoc message) {

    }

    private void processData(MessageAdHoc message) {
        // Check if dest otherwise forward to path
        Data data = (Data) message.getPdu();

        // Update the connexion
        autoConnectionActives.updateDataPath(data.getDestIpAddress());


        Log.d(TAG, "Data message received from: " + message.getHeader().getSenderAddr());

        if (data.getDestIpAddress().equals(ownStringUUID)) {
            Log.d(TAG, ownStringUUID + " is the destination (stop data message)");
            Log.d(TAG, "send DATA-ACK");

            try {
                String destinationAck = message.getHeader().getSenderAddr();
                message.setPdu(new Data(destinationAck, "ACK"));
                message.setHeader(new Header("DATA_ACK", ownStringUUID, ownName));
                send(message, destinationAck);
            } catch (IOException | NoConnectionException e) {
                e.printStackTrace();
            }
        } else {
            EntryRoutingTable destNext = aodv.getNextfromDest(data.getDestIpAddress());
            if (destNext == null) {
                Log.d(TAG, "No destNext found in the routing Table for " + data.getDestIpAddress());
            } else {
                try {
                    send(message, destNext.getNext());
                } catch (IOException | NoConnectionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void processDataAck(MessageAdHoc message) {
        // Check if dest otherwise forward to path
        Data dataAck = (Data) message.getPdu();

        // Update the connexion
        autoConnectionActives.updateDataPath(dataAck.getDestIpAddress());

        Log.d(TAG, "DataAck message received from: " + message.getHeader().getSenderAddr());

        if (dataAck.getDestIpAddress().equals(ownStringUUID)) {
            Log.d(TAG, ownStringUUID + " is the destination (stop data-ack message)");
        } else {
            EntryRoutingTable destNext = aodv.getNextfromDest(dataAck.getDestIpAddress());
            if (destNext == null) {
                Log.d(TAG, "No  destNext found in the routing Table for " + dataAck.getDestIpAddress());
            } else {
                try {
                    send(message, destNext.getNext());
                } catch (IOException | NoConnectionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setListenerGUI(ListenerGUI listenerGUI) {
        this.listenerGUI = listenerGUI;
    }
}
