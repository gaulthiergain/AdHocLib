package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.auto.AutoConnectionActives;
import com.montefiore.gaulthiergain.adhoclibrary.auto.ListenerAodv;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>This class represents the core of the AODV protocols. It manages all the messages and the
 * node behaviour. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

public class AodvManager {

    private final static int DELAY = 60000;
    private final static int PERIOD = 5000;

    private long sequenceNum;

    private final boolean v;
    private final String ownName;
    private final String ownStringUUID;
    private final AodvHelper aodvHelper;
    private final Timer timerRoutingTable;
    private final ListenerAodv listenerAodv;
    private final String TAG = "[AdHoc][AodvManager]";
    private final AutoConnectionActives autoConnectionActives;

    /**
     * Constructor
     *
     * @param verbose       a boolean value to set the debug/verbose mode.
     * @param ownStringUUID a String value which represents the UUID of the current device.
     * @param ownName       a String value which represents the name of the current device.
     * @param listenerAodv  a ListenerAodv object which serves as callback functions.
     */
    public AodvManager(boolean verbose, String ownStringUUID, String ownName,
                       ListenerAodv listenerAodv) {
        this.v = verbose;
        this.autoConnectionActives = new AutoConnectionActives();
        this.timerRoutingTable = new Timer();
        //this.initTimer();
        this.aodvHelper = new AodvHelper(v);
        this.ownStringUUID = ownStringUUID;
        this.ownName = ownName;
        this.sequenceNum = AodvHelper.UNSET_NUM_SEQ;
        this.listenerAodv = listenerAodv;
        if (v) {
            // Print routing table
            this.initTimerDebugRIB();
        }
    }

    /**
     * Method allowing to send a message to a remote address.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @param address a String value which represents the destination address.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     */
    public void send(MessageAdHoc message, String address) throws IOException, NoConnectionException {

        if (autoConnectionActives.getActivesConnections().containsKey(address)) {
            // The destination is directly connected
            NetworkObject networkObject = autoConnectionActives.getActivesConnections().get(address);
            networkObject.sendObjectStream(message);
            if (v) Log.d(TAG, "Send to " + address);

        } else if (aodvHelper.containsDest(address)) {
            // The destination learned from neighbors -> send to next by checking the routing table
            EntryRoutingTable destNext = aodvHelper.getNextfromDest(address);
            if (destNext == null) {
                if (v) Log.d(TAG, "No destNext found in the routing Table for " + address);
            } else {

                if (v) Log.d(TAG, "Routing table contains " + destNext.getNext());
                // Update the connexion
                autoConnectionActives.updateDataPath(address);
                // Send message to remote device
                send(message, destNext.getNext());
            }
        } else if (message.getHeader().getType().equals(TypeAodv.RERR.getCode())) {
            if (v) Log.d(TAG, "RERR sent");
        } else {
            startTimerRREQ(address, AodvHelper.RREQ_RETRIES);
        }
    }

    /**
     * Method allowing to send a message to a remote address.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     */
    private void processRREQ(MessageAdHoc message) throws IOException, NoConnectionException {

        // Get the RREQ message
        RREQ rreq = (RREQ) message.getPdu();

        // Get previous hop and previous source address
        int hop = rreq.getHopCount();
        String originateAddr = message.getHeader().getSenderAddr();

        if (v) Log.d(TAG, "Received RREQ from " + originateAddr);

        if (rreq.getDestIpAddress().equals(ownStringUUID)) {
            if (v) Log.d(TAG, ownStringUUID + " is the destination (stop RREQ broadcast)");

            // Update routing table
            EntryRoutingTable entry = aodvHelper.addEntryRoutingTable(rreq.getOriginIpAddress(),
                    originateAddr, hop, rreq.getSequenceNum());
            if (entry != null) {

                // Generate RREP
                RREP rrep = new RREP(TypeAodv.RREP.getType(), AodvHelper.INIT_HOP_COUNT, rreq.getOriginIpAddress(),
                        sequenceNum, ownStringUUID, AodvHelper.LIFE_TIME);

                if (v) Log.d(TAG, "Destination reachable via " + entry.getNext());

                // Send message to the next destination
                send(new MessageAdHoc(new Header(TypeAodv.RREP.getCode(), ownStringUUID, ownName), rrep),
                        entry.getNext());
            }
        } else {
            if (rreq.getOriginIpAddress().equals(ownStringUUID)) {
                if (v) Log.traceAodv(TAG, "Reject own RREQ " + rreq.getOriginIpAddress()
                                     + " (" + UtilSimulation.getName(rreq.getOriginIpAddress()) + ")");
            } else if (aodvHelper.addBroadcastId(rreq.getOriginIpAddress(), rreq.getRreqId())) {

                // Update PDU and Header
                rreq.incrementHopCount();
                message.setPdu(rreq);
                message.setHeader(new Header(TypeAodv.RREQ.getCode(), ownStringUUID, ownName));

                // Broadcast message to all directly connected devices
                broadcastMsgExcept(message, originateAddr);

                // Update routing table
                EntryRoutingTable entry = aodvHelper.addEntryRoutingTable(rreq.getOriginIpAddress(),
                        originateAddr, hop, rreq.getSequenceNum());

            } else {
                if (v) Log.d(TAG, "Already received this RREQ from " + rreq.getOriginIpAddress());
            }
        }
    }

    /**
     * Method allowing to process a RREP message.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     */
    private void processRREP(MessageAdHoc message) throws IOException, NoConnectionException {

        // Get the RREP message
        RREP rrep = (RREP) message.getPdu();

        // Get previous hop and previous source address
        int hopRcv = rrep.getHopCount();
        String originateAddrRcv = message.getHeader().getSenderAddr();

        if (v) Log.d(TAG, "Received RREP from " + originateAddrRcv);

        if (rrep.getDestIpAddress().equals(ownStringUUID)) {
            if (v) Log.d(TAG, ownStringUUID + " is the destination (stop RREP)");

            // Increment originSeqNum
            sequenceNum += 2;
        } else {
            // Forward the RREP message to the destination by checking the routing table
            EntryRoutingTable destNext = aodvHelper.getNextfromDest(rrep.getDestIpAddress());
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
        EntryRoutingTable entryRRep = aodvHelper.addEntryRoutingTable(rrep.getOriginIpAddress(),
                originateAddrRcv, hopRcv, rrep.getSequenceNum());
    }

    /**
     * Method allowing to process a RERR message.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void processRERR(MessageAdHoc message) throws IOException {

        // Get the RERR message
        RERR rerr = (RERR) message.getPdu();

        // Get previous source address
        String originateAddr = message.getHeader().getSenderAddr();

        Log.d(TAG, "Received RERR from " + originateAddr + " -> Node " +
                rerr.getUnreachableDestIpAddress() + " is unreachable");

        if (rerr.getUnreachableDestIpAddress().equals(ownStringUUID)) {
            Log.d(TAG, "RERR received on the destination (stop forward)");
        } else if (aodvHelper.containsDest(rerr.getUnreachableDestIpAddress())) {
            aodvHelper.getRoutingTable().removeEntry(rerr.getUnreachableDestIpAddress());
            message.setHeader(new Header(TypeAodv.RERR.getCode(), ownStringUUID, ownName));
            // Broadcast message to all directly connected devices
            broadcastMsgExcept(message, originateAddr);
        } else {
            Log.d(TAG, "Node doesn't contain dest: " + rerr.getUnreachableDestIpAddress());
        }
    }

    /**
     * Method allowing to process a DATA message.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     */
    private void processData(MessageAdHoc message) throws IOException, NoConnectionException {

        // Get the DATA message
        Data data = (Data) message.getPdu();

        // Update the data path
        autoConnectionActives.updateDataPath(data.getDestIpAddress());

        if (v) Log.d(TAG, "Data message received from: " + message.getHeader().getSenderAddr());

        if (data.getDestIpAddress().equals(ownStringUUID)) {
            if (v) Log.d(TAG, ownStringUUID + " is the destination (stop DATA message " +
                    "and send ACK)");

            // Update PDU and Header
            String destinationAck = message.getHeader().getSenderAddr();
            message.setPdu(new Data(destinationAck, TypeAodv.DATA_ACK.getCode()));
            message.setHeader(new Header(TypeAodv.DATA_ACK.getCode(), ownStringUUID, ownName));

            // Send message to the destination
            send(message, destinationAck);
        } else {
            // Forward the DATA message to the destination by checking the routing table
            EntryRoutingTable destNext = aodvHelper.getNextfromDest(data.getDestIpAddress());
            if (destNext == null) {
                if (v) Log.d(TAG, "No destNext found in the routing Table for " +
                        data.getDestIpAddress());
            } else {
                if (v) Log.d(TAG, "Destination reachable via " + destNext.getNext());
                // Send message to the next destination
                send(message, destNext.getNext());
            }
        }
    }

    /**
     * Method allowing to process a DATA message.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     */
    private void processDataAck(MessageAdHoc message) throws IOException, NoConnectionException {

        // Get the ACK message
        Data dataAck = (Data) message.getPdu();

        // Update the data path
        autoConnectionActives.updateDataPath(dataAck.getDestIpAddress());

        if (v) Log.d(TAG, "ACK message received from: " + message.getHeader().getSenderAddr());

        if (dataAck.getDestIpAddress().equals(ownStringUUID)) {
            if (v) Log.d(TAG, ownStringUUID + " is the destination (stop data-ack message)");
        } else {
            // Forward the ACK message to the destination by checking the routing table
            EntryRoutingTable destNext = aodvHelper.getNextfromDest(dataAck.getDestIpAddress());
            if (destNext == null) {
                if (v)
                    Log.d(TAG, "No destNext found in the routing Table for " +
                            dataAck.getDestIpAddress());
            } else {
                if (v) Log.d(TAG, "Destination reachable via " + destNext.getNext());
                // Send message to the next destination
                send(message, destNext.getNext());
            }
        }
    }

    /**
     * Method allowing to send a RREQ message to find a destination. This timer is executed
     * RREQ_RETRIES times and every NET_TRANVERSAL_TIME seconds.
     *
     * @param destAddr a String value which represents the destination address.
     * @param retry    an integer value which represents the retries of the RREQ Timer.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void startTimerRREQ(final String destAddr, final int retry) throws IOException {

        // No destination was found, send RREQ request (with timer)
        Log.d(TAG, "No connection to " + destAddr + "-> send RREQ message");

        // Broadcast message to all directly connected devices
        broadcastMsg(new MessageAdHoc(new Header(TypeAodv.RREQ.getCode(), ownStringUUID, ownName),
                new RREQ(TypeAodv.RREQ.getType(), AodvHelper.INIT_HOP_COUNT,
                        aodvHelper.getIncrementRreqId(), destAddr, sequenceNum, ownStringUUID)));

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                
                EntryRoutingTable entry = aodvHelper.getNextfromDest(destAddr);
                if (entry != null) {
                    //test seq num here todo
                    
                } else {
                    if (retry == 0) {
                        if (v) Log.traceAodv(TAG, "Expired time: no RREP received for " + destAddr);
                        listenerAodv.timerExpiredRREQ(destAddr, retry);
                    } else {
                        if (v) Log.traceAodv(TAG, "Expired time: no RREP received for " + destAddr +
                                             " Retry: " + retry);
                        listenerAodv.timerExpiredRREQ(destAddr, retry);
                        try {
                            startTimerRREQ(destAddr, retry - 1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                
            }
        }, AodvHelper.NET_TRANVERSAL_TIME);
    }

    /**
     * Method allowing to send a RRER message when the connection is closed.
     *
     * @param destAddr a String value which represents the destination address.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     */
    private void sendRRER(String destAddr) throws IOException, NoConnectionException {

        if (aodvHelper.getRoutingTable().containsNext(destAddr)) {
            String dest = aodvHelper.getRoutingTable().getDestFromNext(destAddr);

            if (dest.equals(ownStringUUID)) {
                if (v) Log.d(TAG, "RERR received on the destination (stop forward)");
            } else {
                // Remove the destination from Routing table
                aodvHelper.getRoutingTable().removeEntry(dest);
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

    /**
     * Method allowing to purge the routing table after EXPIRED_TABLE in ms if no data are
     * transmitted on a connection.
     */
    private void initTimer() {
        timerRoutingTable.schedule(new TimerTask() {
            @Override
            public void run() {

                Iterator<Map.Entry<String, EntryRoutingTable>> it = aodvHelper.getRoutingTable().
                        getRoutingTable().entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, EntryRoutingTable> entry = it.next();

                    // Check if data is recently sent/received
                    if (autoConnectionActives.getActivesDataPath().containsKey(entry.getKey())) {
                        long lastChanged = autoConnectionActives.getActivesDataPath().get(entry.getKey());
                        if (System.currentTimeMillis() - lastChanged > AodvHelper.EXPIRED_TIME) {
                            if (v)
                                Log.d(TAG, "No data on " + entry.getKey() + " since " +
                                        AodvHelper.EXPIRED_TIME + "ms -> Purge Entry in RIB");
                            autoConnectionActives.getActivesDataPath().remove(entry.getKey());
                            it.remove();
                            if (listenerAodv != null) listenerAodv.timerFlushRoutingTable();
                        } else {
                            if (v)
                                Log.d(TAG, ">>> data on " + entry.getKey() + " since " +
                                        AodvHelper.EXPIRED_TIME + "ms");
                        }
                    } else {
                        // Purge entry in RIB
                        if (v)
                            Log.d(TAG, "No data on " + entry.getKey() + " since " +
                                    AodvHelper.EXPIRED_TIME + "ms -> Purge Entry in RIB");
                        it.remove();
                        if (listenerAodv != null) listenerAodv.timerFlushRoutingTable();
                    }
                }
            }
        }, AodvHelper.EXPIRED_TABLE, AodvHelper.EXPIRED_TABLE);
    }

    /**
     * Method allowing to broadcast a message to all nodes.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void broadcastMsg(MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : autoConnectionActives.getActivesConnections().entrySet()) {
            entry.getValue().sendObjectStream(message);
            if (v) Log.d(TAG, "Broadcast Message to " + entry.getKey());
        }
    }

    /**
     * Method allowing to broadcast a message to all nodes except a particular one.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @param address a String value which represents the destination address.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void broadcastMsgExcept(MessageAdHoc message, String address) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : autoConnectionActives.getActivesConnections().entrySet()) {
            if (!entry.getKey().equals(address)) {
                entry.getValue().sendObjectStream(message);
                if (v) Log.d(TAG, "Broadcast Message to " + entry.getKey());
            }
        }
    }

    /**
     * Method allowing to remove a remote connection from the autoConnectionActives set and from
     * the routing table.
     *
     * @param remoteUuid a String value which represents the remote address of a node.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     */
    public void removeRemoteConnection(String remoteUuid) throws IOException, NoConnectionException {

        // Remove remote connections
        if (autoConnectionActives.getActivesConnections().containsKey(remoteUuid)) {
            if (v) Log.d(TAG, "Remove active connection with " + remoteUuid);
            NetworkObject networkObject = autoConnectionActives.getActivesConnections().get(remoteUuid);
            autoConnectionActives.getActivesConnections().remove(remoteUuid);
            networkObject.closeConnection();
        }

        if (aodvHelper.getRoutingTable().containsDest(remoteUuid)) {
            if (v) Log.d(TAG, "Remove " + remoteUuid + " from RIB");
            aodvHelper.getRoutingTable().removeEntry(remoteUuid);
        }

        if (aodvHelper.getRoutingTable().getRoutingTable().size() > 0) {
            if (v) Log.d(TAG, "Send RRER ");
            sendRRER(remoteUuid);
        }
    }

    /**
     * Method allowing to process AODV messages.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException              Signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException    Signals that a No Connection Exception exception has occurred.
     * @throws AodvUnknownTypeException Signals that a Unknown AODV type has been caught.
     */
    public void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException {
        switch (message.getHeader().getType()) {
            case "RREP":
                if (listenerAodv != null) listenerAodv.receivedRREP(message);
                processRREP(message);
                break;
            case "RREQ":
                if (listenerAodv != null) listenerAodv.receivedRREQ(message);
                processRREQ(message);
                break;
            case "RERR":
                if (listenerAodv != null) listenerAodv.receivedRERR(message);
                processRERR(message);
                break;
            case "DATA":
                if (listenerAodv != null) listenerAodv.receivedDATA(message);
                processData(message);
                break;
            case "DATA_ACK":
                if (listenerAodv != null) listenerAodv.receivedDATA_ACK(message);
                processDataAck(message);
                break;
            default:
                throw new AodvUnknownTypeException("Unknown AODV Type");
        }
    }

    /**
     * Method allowing to get all the outgoing and incoming connections.
     *
     * @return a ConcurrentHashMap<String, NetworkObject> object which maps the remote node name to
     * a NetworkObject object.
     */
    public ConcurrentHashMap<String, NetworkObject> getConnections() {
        return autoConnectionActives.getActivesConnections();
    }

    /**
     * Method allowing to add a connection to the autoConnectionActives set.
     *
     * @param key     a String value which represents the address of a remote node.
     * @param network a NetworkObject object which represents the state of the connection.
     */
    public void addConnection(String key, NetworkObject network) {
        autoConnectionActives.addConnection(key, network);
    }

    /**
     * Method allowing to print the routing table after a DELAY in ms and every PERIOD times.
     */
    private void initTimerDebugRIB() {
        Timer timerDebugRIB = new Timer();
        timerDebugRIB.schedule(new TimerTask() {
            @Override
            public void run() {
                updateRoutingTable();
            }
        }, DELAY, PERIOD);
    }

    /**
     * Method allowing to display the routing table in debug mode.
     */
    private void updateRoutingTable() {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("----------------------------------------\n");
        stringBuilder.append("Routing Table:\n");
        for (Map.Entry<String, EntryRoutingTable> entry : aodvHelper.getRoutingTable().
                getRoutingTable().entrySet()) {
            stringBuilder.append(entry.getValue().toString()).append("\n");
        }

        if (autoConnectionActives.getActivesDataPath().size() > 0) {
            stringBuilder.append("----------------------------------------\n");
            stringBuilder.append("Active Data Path:\n");
        }

        for (Map.Entry<String, Long> entry : autoConnectionActives.getActivesDataPath().entrySet()) {
            stringBuilder.append("- ").append(entry.getKey()).append(" ").
                    append(entry.getValue().toString()).append("\n");
        }

        if (v) Log.i(TAG, stringBuilder.toString());
    }
}
