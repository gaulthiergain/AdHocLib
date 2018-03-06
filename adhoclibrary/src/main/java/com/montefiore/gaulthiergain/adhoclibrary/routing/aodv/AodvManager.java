package com.montefiore.gaulthiergain.adhoclibrary.routing.aodv;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DataLinkBtManager;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DataLinkWifiManager;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.IDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ListenerAodv;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.Timer;

/**
 * <p>This class represents the core of the AODV protocols. It manages all the messages and the
 * node behaviour. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

public class AodvManager {

    private static final String TAG = "[AdHoc][AodvManager]";

    // Constants for taking only the last part of the UUID
    private final static int LOW = 24;
    private final static int END = 36;

    // Constants for displaying the routing table
    private final static int DELAY = 60000;
    private final static int PERIOD = DELAY;

    private IDataLink dataLink;
    private long ownSequenceNum;
    private MessageAdHoc dataMessage;

    private final boolean v;
    private final String ownName;
    private final String ownAddress;
    private final AodvHelper aodvHelper;
    private final ListenerAodv listenerAodv;
    private final HashMap<String, Long> mapDestSequenceNumber;


    /**
     * Constructor
     *
     * @param verbose      a boolean value to set the debug/verbose mode.
     * @param ownAddress   a String value which represents the address of the current device.
     * @param ownName      a String value which represents the name of the current device.
     * @param listenerAodv a ListenerAodv object which serves as callback functions.
     */
    private AodvManager(boolean verbose, String ownAddress, String ownName,
                        ListenerAodv listenerAodv) {
        this.v = verbose;
        this.aodvHelper = new AodvHelper(v);
        this.ownAddress = ownAddress;
        this.ownName = ownName;
        this.ownSequenceNum = Constants.FIRST_SEQUENCE_NUMBER;
        this.listenerAodv = listenerAodv;
        this.mapDestSequenceNumber = new HashMap<>();
        if (v) {
            // Print routing table
            this.initTimerDebugRIB();
        }
    }

    /**
     * Constructor
     *
     * @param verbose      a boolean value to set the debug/verbose mode.
     * @param context      a Context object which gives global information about an application
     *                     environment.
     * @param ownUUID      an UUID object which represents the UUID of the current device.
     * @param ownName      a String value which represents the name of the current device
     * @param listenerAodv a ListenerAodv object which serves as callback functions.
     * @throws IOException     Signals that an I/O exception of some sort has occurred.
     * @throws DeviceException Signals that a DeviceException has occurred.
     */
    public AodvManager(boolean verbose, Context context, UUID ownUUID, String ownName,
                       ListenerAodv listenerAodv) throws IOException, DeviceException {
        this(verbose, ownUUID.toString().substring(LOW, END), ownName, listenerAodv);
        this.initDataLinkBt(verbose, context, ownUUID, ownName);
    }

    /**
     * Constructor
     *
     * @param verbose      a boolean value to set the debug/verbose mode.
     * @param context      a Context object which gives global information about an application
     *                     environment.
     * @param ownAddress   a String value which represents the address of the current device.
     * @param ownName      a String value which represents the name of the current device.
     * @param serverPort   an integer value which represents the server port.
     * @param listenerAodv a ListenerAodv object which serves as callback functions.
     * @throws DeviceException Signals that a DeviceException has occurred.
     */
    public AodvManager(boolean verbose, Context context, String ownAddress, String ownName,
                       int serverPort, ListenerAodv listenerAodv) throws DeviceException {
        this(verbose, ownAddress, ownName, listenerAodv);
        initDataLinkWifi(verbose, context, ownAddress, ownName, serverPort);
    }

    /**
     * Method allowing to send a message to a remote address.
     *
     * @param pdu     a Serializable value which represents the PDU of the message.
     * @param address a String value which represents the destination address.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public void sendMessageTo(Serializable pdu, String address) throws IOException {

        // Create MessageAdHoc object
        Header header = new Header(TypeAodv.DATA.getCode(), ownAddress, ownName);
        MessageAdHoc msg = new MessageAdHoc(header, new Data(address, pdu));

        send(msg, address);
    }

    /**
     * Method allowing to connect to other devices
     */
    public void connect() {
        dataLink.connect();
    }

    /**
     * Method allowing to stop the server listening threads.
     *
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public void stopListening() throws IOException {
        dataLink.stopListening();
    }

    /**************************************************Private methods*************************************************/

    private void initDataLinkWifi(boolean v, Context context, String ownAddress, String ownName, int serverPort)
            throws DeviceException {
        dataLink = new DataLinkWifiManager(v, context, 10, serverPort,
                listenerAodv, new ListenerDataLinkAodv() {

            @Override
            public void brokenLink(String remoteNode) throws IOException {
                brokenLinkDetected(remoteNode);
            }

            @Override
            public void processMsgReceived(MessageAdHoc message) throws IOException, AodvUnknownTypeException,
                    AodvUnknownDestException, NoConnectionException {
                processAodvMsgReceived(message);
            }
        });
    }

    private void initDataLinkBt(boolean v, Context context, UUID ownUUID, String ownName)
            throws IOException, DeviceException {

        dataLink = new DataLinkBtManager(v, context, ownUUID, ownName, true,
                listenerAodv, new ListenerDataLinkAodv() {

            @Override
            public void brokenLink(String remoteNode) throws IOException {
                brokenLinkDetected(remoteNode);
            }

            @Override
            public void processMsgReceived(MessageAdHoc message) throws IOException, AodvUnknownTypeException, AodvUnknownDestException, NoConnectionException {
                processAodvMsgReceived(message);
            }
        });

    }


    /**
     * Method allowing to detect if a link is broken
     *
     * @param remoteNode a String which represents the destination address
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void brokenLinkDetected(String remoteNode) throws IOException {
        // Send RRER to precursors to signal that a remote node is disconnected
        if (aodvHelper.sizeRoutingTable() > 0) {
            if (v) Log.d(TAG, "Send RRER ");
            sendRRER(remoteNode);
        }

        // Check if the node contains the remote node
        if (aodvHelper.containsDest(remoteNode)) {
            if (v)
                Log.d(TAG, "Remove " + remoteNode + " from RIB");
            aodvHelper.removeEntry(remoteNode);
        }
    }

    /**
     * Method allowing to send directly a message to a remote address
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @param address a String value which represents the destination address.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void sendDirect(MessageAdHoc message, String address) throws IOException {
        // The destination is directly connected
        dataLink.sendMessage(message, address);
    }

    /**
     * Method allowing to send a message to a remote address.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @param address a String value which represents the destination address.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void send(MessageAdHoc message, String address) throws IOException {

        if (dataLink.isDirectNeighbors(address)) {

            EntryRoutingTable destNext = aodvHelper.getNextfromDest(address);
            if (destNext != null && message.getHeader().getType().equals(TypeAodv.DATA.getCode())) {
                // Update dataPath
                destNext.updateDataPath(address);
            }
            sendDirect(message, address);

        } else if (aodvHelper.containsDest(address)) {
            // The destination learned from neighbors -> send to next by checking the routing table
            EntryRoutingTable destNext = aodvHelper.getNextfromDest(address);
            if (destNext == null) {
                if (v) Log.d(TAG, "No destNext found in the routing Table for " + address);
            } else {

                if (v) Log.d(TAG, "Routing table contains " + destNext.getNext());

                if (message.getHeader().getType().equals(TypeAodv.DATA.getCode())) {
                    // Update dataPath
                    destNext.updateDataPath(address);
                }

                // Send message to remote device
                sendDirect(message, destNext.getNext());
            }
        } else if (message.getHeader().getType().equals(TypeAodv.RERR.getCode())) {
            if (v) Log.d(TAG, "RERR sent");
        } else {
            dataMessage = message;
            // Increment sequence number prior to insertion in RREQ
            getNextSequenceNumber();
            startTimerRREQ(address, Constants.RREQ_RETRIES, Constants.NET_TRANVERSAL_TIME);
        }
    }

    /**
     * Method allowing to send a message to a remote address.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void processRREQ(MessageAdHoc message) throws IOException {

        // Get the RREQ message
        RREQ rreq = (RREQ) message.getPdu();

        // Get previous hop and previous source address
        int hop = rreq.getHopCount();
        String originateAddr = message.getHeader().getSenderAddr();

        if (v) Log.d(TAG, "Received RREQ from " + originateAddr);

        if (rreq.getDestIpAddress().equals(ownAddress)) {

            // Save the destination sequence number into a hashmap
            saveDestSequenceNumber(rreq.getOriginIpAddress(), rreq.getOriginSequenceNum());

            if (v) Log.d(TAG, ownAddress + " is the destination (stop RREQ broadcast)");

            // Update routing table
            EntryRoutingTable entry = aodvHelper.addEntryRoutingTable(rreq.getOriginIpAddress(),
                    originateAddr, hop, rreq.getOriginSequenceNum(), Constants.NO_LIFE_TIME, null);
            if (entry != null) {

                //Destination nodes increment their sequence numbers when the sequence number in the RREQ is equal
                // to their stored number.
                if (rreq.getDestSequenceNum() > ownSequenceNum) {
                    getNextSequenceNumber();
                }

                // Generate RREP
                RREP rrep = new RREP(TypeAodv.RREP.getType(), Constants.INIT_HOP_COUNT, rreq.getOriginIpAddress(),
                        ownSequenceNum, ownAddress, Constants.LIFE_TIME);

                if (v) Log.d(TAG, "Destination reachable via " + entry.getNext());

                // Send message to the next destination
                send(new MessageAdHoc(new Header(TypeAodv.RREP.getCode(), ownAddress, ownName), rrep),
                        entry.getNext());

                //Run timer for this reverse route
                timerFlushReverseRoute(rreq.getOriginIpAddress(), rreq.getOriginSequenceNum());
            }
        } else if (aodvHelper.containsDest(rreq.getDestIpAddress())) {
            // Send RREP GRATUITOUS to destination
            sendRREP_GRAT(message.getHeader().getSenderAddr(), rreq);
        } else {
            // Broadcast RREQ
            if (rreq.getOriginIpAddress().equals(ownAddress)) {
                if (v) Log.d(TAG, "Reject own RREQ " + rreq.getOriginIpAddress());
            } else if (aodvHelper.addBroadcastId(rreq.getOriginIpAddress(), rreq.getRreqId())) {

                // Update PDU and Header
                rreq.incrementHopCount();
                message.setPdu(rreq);
                message.setHeader(new Header(TypeAodv.RREQ.getCode(), ownAddress, ownName));

                // Broadcast message to all directly connected devices
                dataLink.broadcastExcept(originateAddr, message);

                // Update routing table
                aodvHelper.addEntryRoutingTable(rreq.getOriginIpAddress(), originateAddr, hop,
                        rreq.getOriginSequenceNum(), Constants.NO_LIFE_TIME, null);

                //Run timer for this reverse route
                timerFlushReverseRoute(rreq.getOriginIpAddress(), rreq.getOriginSequenceNum());

            } else {
                if (v) Log.d(TAG, "Already received this RREQ from " + rreq.getOriginIpAddress());
            }
        }
    }

    /**
     * Method allowing to process a RREP message.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException              Signals that an I/O exception of some sort has occurred.
     * @throws AodvUnknownDestException Signals that an AodvUnknownDestException has occurred.
     */
    private void processRREP(MessageAdHoc message) throws IOException, AodvUnknownDestException {

        // Get the RREP message
        RREP rrep = (RREP) message.getPdu();

        // Get previous hop and previous source address
        int hopRcv = rrep.getHopCount();
        String nextHop = message.getHeader().getSenderAddr();

        if (v) Log.d(TAG, "Received RREP from " + nextHop);

        if (rrep.getDestIpAddress().equals(ownAddress)) {
            if (v) Log.d(TAG, ownAddress + " is the destination (stop RREP)");

            // Save the destination sequence number into a hashmap
            saveDestSequenceNumber(rrep.getOriginIpAddress(), rrep.getSequenceNum());

            // Update routing table
            aodvHelper.addEntryRoutingTable(rrep.getOriginIpAddress(),
                    nextHop, hopRcv, rrep.getSequenceNum(), rrep.getLifetime(), null);

            // Update the connexion and send message to destination node
            Data data = (Data) dataMessage.getPdu();
            send(dataMessage, data.getDestIpAddress());

            timerFlushForwardRoute(rrep.getOriginIpAddress(), rrep.getSequenceNum(), rrep.getLifetime());
        } else {
            // Forward the RREP message to the destination by checking the routing table
            EntryRoutingTable destNext = aodvHelper.getNextfromDest(rrep.getDestIpAddress());
            if (destNext == null) {
                throw new AodvUnknownDestException("No destNext found in the routing Table for " +
                        rrep.getDestIpAddress());
            } else {
                if (v) Log.d(TAG, "Destination reachable via " + destNext.getNext());

                // Increment HopCount and send message to the next destination
                rrep.incrementHopCount();
                send(new MessageAdHoc(new Header(TypeAodv.RREP.getCode(), ownAddress, ownName)
                        , rrep), destNext.getNext());

                // Update routing table
                aodvHelper.addEntryRoutingTable(rrep.getOriginIpAddress(), nextHop, hopRcv, rrep.getSequenceNum(),
                        rrep.getLifetime(), addPrecursors(destNext.getNext()));

                // Launch Timer for forwarding route
                timerFlushForwardRoute(rrep.getOriginIpAddress(), rrep.getSequenceNum(), rrep.getLifetime());
            }
        }
    }

    /**
     * Method allowing to process a RREP message.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException              Signals that an I/O exception of some sort has occurred.
     * @throws AodvUnknownDestException Signals that an AodvUnknownDestException has occurred.
     */
    private void processRREP_GRATUITOUS(MessageAdHoc message) throws IOException, AodvUnknownDestException {

        // Get the RREP message
        RREP rrep = (RREP) message.getPdu();

        int hopCount = rrep.incrementHopCount();

        if (rrep.getDestIpAddress().equals(ownAddress)) {
            if (v) Log.d(TAG, ownAddress + " is the destination (stop RREP)");

            // Update routing table
            aodvHelper.addEntryRoutingTable(rrep.getOriginIpAddress(), message.getHeader().getSenderAddr(),
                    hopCount, rrep.getSequenceNum(), rrep.getLifetime(), null);

            // Add timer for reverse route
            timerFlushReverseRoute(rrep.getOriginIpAddress(), rrep.getSequenceNum());
        } else {
            EntryRoutingTable destNext = aodvHelper.getNextfromDest(rrep.getDestIpAddress());
            if (destNext == null) {
                throw new AodvUnknownDestException("No destNext found in the routing Table for " +
                        rrep.getDestIpAddress());
            } else {
                if (v) Log.d(TAG, "Destination reachable via " + destNext.getNext());


                // Update routing table
                aodvHelper.addEntryRoutingTable(rrep.getOriginIpAddress(), message.getHeader().getSenderAddr(),
                        hopCount, rrep.getSequenceNum(), rrep.getLifetime(), addPrecursors(destNext.getNext()));

                // Add timer for reverse route
                timerFlushReverseRoute(rrep.getOriginIpAddress(), rrep.getSequenceNum());

                // Update header
                message.setHeader(new Header(TypeAodv.RREP_GRATUITOUS.getCode(), ownAddress, ownName));

                // Send message to the next destination
                send(message, destNext.getNext());
            }

        }
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

        if (v) Log.d(TAG, "Received RERR from " + originateAddr + " -> Node "
                + rerr.getUnreachableDestIpAddress() + " is unreachable");


        if (rerr.getUnreachableDestIpAddress().equals(ownAddress)) {
            if (v) Log.d(TAG, "RERR received on the destination (stop forward)");
        } else if (aodvHelper.containsDest(rerr.getUnreachableDestIpAddress())) {

            message.setHeader(new Header(TypeAodv.RERR.getCode(), ownAddress, ownName));
            // Send to precursors
            ArrayList<String> precursors = aodvHelper.getPrecursorsFromDest(rerr.getUnreachableDestIpAddress());
            if (precursors != null) {
                for (String precursor : precursors) {
                    if (v) Log.d(TAG, " Precursor: " + precursor);
                    send(message, precursor);
                }
            } else {
                if (v) Log.d(TAG, "No precursors");
            }

            // remove entry
            aodvHelper.removeEntry(rerr.getUnreachableDestIpAddress());
        } else {
            if (v) Log.d(TAG, "Node doesn't contain dest: " + rerr.getUnreachableDestIpAddress());
        }
    }

    /**
     * Method allowing to process a DATA message.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException              Signals that an I/O exception of some sort has occurred.
     * @throws AodvUnknownDestException Signals that an AodvUnknownDestException has occurred.
     */
    private void processData(MessageAdHoc message) throws IOException, AodvUnknownDestException {

        // Get the DATA message
        Data data = (Data) message.getPdu();

        if (v) Log.d(TAG, "Data message received from: " + message.getHeader().getSenderAddr());

        if (data.getDestIpAddress().equals(ownAddress)) {
            if (v) Log.d(TAG, ownAddress + " is the destination (stop DATA message");
            if (listenerAodv != null) listenerAodv.receivedDATA(message);
        } else {
            // Forward the DATA message to the destination by checking the routing table
            EntryRoutingTable destNext = aodvHelper.getNextfromDest(data.getDestIpAddress());
            if (destNext == null) {
                throw new AodvUnknownDestException("No destNext found in the routing Table for " +
                        data.getDestIpAddress());
            } else {
                if (v) Log.d(TAG, "Destination reachable via " + destNext.getNext());

                // Update dataPath
                destNext.updateDataPath(data.getDestIpAddress());

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
     * @param time     an integer value which represents the period of the timer.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void startTimerRREQ(final String destAddr, final int retry, final int time) throws IOException {

        // No destination was found, send RREQ request (with timer)
        if (v) Log.d(TAG, "No connection to " + destAddr + " -> send RREQ message");


        // Broadcast message to all directly connected devices
        MessageAdHoc message = new MessageAdHoc(new Header(TypeAodv.RREQ.getCode(), ownAddress, ownName),
                new RREQ(TypeAodv.RREQ.getType(), Constants.INIT_HOP_COUNT,
                        aodvHelper.getIncrementRreqId(), getDestSequenceNumber(destAddr), destAddr, ownSequenceNum, ownAddress));
        dataLink.broadcast(message);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {

                EntryRoutingTable entry = aodvHelper.getNextfromDest(destAddr);
                if (entry == null) {
                    // If no dest found, retry again the route discovery
                    if (retry == 0) {
                        if (v) Log.d(TAG, "Expired time: no RREP received for " + destAddr);
                        dataMessage = null;
                        listenerAodv.timerExpiredRREQ(destAddr, retry);
                    } else {
                        if (v) Log.d(TAG, "Expired time: no RREP received for " + destAddr +
                                " Retry: " + retry);
                        listenerAodv.timerExpiredRREQ(destAddr, retry);
                        try {
                            startTimerRREQ(destAddr, retry - 1, time * 2);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }, time);
    }

    /**
     * Method allowing to send a RRER message when the connection is closed.
     *
     * @param brokenNodeAddress a String value which represents the broken node address.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void sendRRER(String brokenNodeAddress) throws IOException {

        if (aodvHelper.containsNext(brokenNodeAddress)) {
            String dest = aodvHelper.getDestFromNext(brokenNodeAddress);

            if (dest.equals(ownAddress)) {
                if (v) Log.d(TAG, "RERR received on the destination (stop forward)");
            } else {

                RERR rrer = new RERR(TypeAodv.RERR.getType(), dest, ownSequenceNum);

                // Send RERR message to all precursors
                ArrayList<String> precursors = aodvHelper.getPrecursorsFromDest(dest);
                if (precursors != null) {
                    for (String precursor : precursors) {
                        if (v)
                            Log.d(TAG, "send RERR to " + precursor);
                        send(new MessageAdHoc(new Header(TypeAodv.RERR.getCode(), ownAddress, ownName), rrer),
                                precursor);
                    }
                }

                // Remove the destination from Routing table
                aodvHelper.removeEntry(dest);
            }
        }
    }

    /**
     * Method allowing to send a RREP gratuitous message to source and destination nodes.
     *
     * @param senderAddr a String value which represents the source address.
     * @param rreq       a RREQ object which represents a RREQ message.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void sendRREP_GRAT(String senderAddr, RREQ rreq) throws IOException {
        // Get entry in routing table for the destination
        EntryRoutingTable entry = aodvHelper.getDestination(rreq.getDestIpAddress());

        // Update the list of precursors
        entry.updatePrecursors(senderAddr);

        // Add routing table entry
        aodvHelper.addEntryRoutingTable(rreq.getOriginIpAddress(), senderAddr,
                rreq.getHopCount(), rreq.getOriginSequenceNum(), Constants.NO_LIFE_TIME, addPrecursors(entry.getNext()));

        // Add timer for reverse route
        timerFlushReverseRoute(rreq.getOriginIpAddress(), rreq.getOriginSequenceNum());

        // Generate gratuitous RREP
        RREP rrep = new RREP(TypeAodv.RREP_GRATUITOUS.getType(), rreq.getHopCount(),
                rreq.getDestIpAddress(), ownSequenceNum, rreq.getOriginIpAddress(), Constants.LIFE_TIME);

        // Send gratuitous RREP message to the next destination
        send(new MessageAdHoc(new Header(TypeAodv.RREP_GRATUITOUS.getCode(), ownAddress, ownName), rrep),
                entry.getNext());
        if (v) Log.d(TAG, "Send Gratuitous RREP to " + entry.getNext());

        // Generate RREP message for the source
        rrep = new RREP(TypeAodv.RREP.getType(), entry.getHop() + 1,
                rreq.getOriginIpAddress(), entry.getDestSeqNum(), entry.getDestIpAddress(), Constants.LIFE_TIME);

        // Send RREP message to the source
        send(new MessageAdHoc(new Header(TypeAodv.RREP.getCode(), ownAddress, ownName), rrep),
                rreq.getOriginIpAddress());
        if (v) Log.d(TAG, "Send RREP to " + rreq.getOriginIpAddress());
    }

    /**
     * Method allowing to purge the forward entries of the routing table after LIFE_TIME in ms if no data is
     * transmitted on a connection.
     *
     * @param destIpAddress a string value which represents the destination address.
     * @param sequenceNum   a long value which represents the destination sequence number.
     * @param lifeTime      a long value which represents the lifetime of the entry in the routing table.
     */
    private void timerFlushForwardRoute(final String destIpAddress, final long sequenceNum, final long lifeTime) {
        Timer timerFlushForwardRoute = new Timer();

        timerFlushForwardRoute.schedule(new TimerTask() {
            @Override
            public void run() {
                if (v) Log.d(TAG, "Add timer for " + destIpAddress
                        + " - seq: " + sequenceNum + " - lifeTime: " + lifeTime);
                long lastChanged = aodvHelper.getDataPathFromAddress(destIpAddress);
                // Get difference of time between the current time and the last time where data is transmitted
                long difference = (System.currentTimeMillis() - lastChanged);

                if (lastChanged == 0) {
                    // If no data on the reverse route, delete it
                    aodvHelper.removeEntry(destIpAddress);
                    if (v) Log.d(TAG, "No Data on " + destIpAddress);
                } else if (difference < lifeTime) {
                    // Data on the path, restart timer
                    timerFlushForwardRoute(destIpAddress, sequenceNum, lifeTime);
                } else {
                    // If no data on the reverse route, delete it
                    aodvHelper.removeEntry(destIpAddress);
                    if (v)
                        Log.d(TAG, "No Data on " + destIpAddress + " since " + difference);
                }
            }
        }, lifeTime);
    }

    /**
     * Method allowing to purge the reverse entries of the routing table after EXPIRED_TABLE in ms if no data is
     * transmitted on a connection.
     *
     * @param originIpAddress a string value which represents the address of the originator.
     * @param sequenceNum     a long value which represents the destination sequence number.
     */
    private void timerFlushReverseRoute(final String originIpAddress, final long sequenceNum) {

        Timer timerFlushReverseRoute = new Timer();

        timerFlushReverseRoute.schedule(new TimerTask() {
            @Override
            public void run() {
                if (v) Log.d(TAG, "Add timer for " + originIpAddress + "- seq: " + sequenceNum);
                long lastChanged = aodvHelper.getDataPathFromAddress(originIpAddress);
                // Get difference of time between the current time and the last time where data is transmitted
                long difference = (System.currentTimeMillis() - lastChanged);

                if (lastChanged == 0) {
                    // If no data on the reverse route, delete it
                    aodvHelper.removeEntry(originIpAddress);
                    if (v) Log.d(TAG, "No Data on " + originIpAddress);
                } else if (difference < Constants.EXPIRED_TIME) {
                    // Data on the path, restart timer
                    timerFlushReverseRoute(originIpAddress, sequenceNum);
                } else {
                    // If no data on the reverse route, delete it
                    aodvHelper.removeEntry(originIpAddress);
                    if (v)
                        Log.d(TAG, "No Data on " + originIpAddress + " since " + difference);
                }
            }
        }, Constants.EXPIRED_TABLE);
    }

    /**
     * Method allowing to process AODV messages.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException              Signals that an I/O exception of some sort has occurred.
     * @throws AodvUnknownTypeException Signals that a Unknown AODV type has been caught.
     * @throws AodvUnknownDestException Signals that an AodvUnknownDestException has occurred.
     */
    private void processAodvMsgReceived(MessageAdHoc message) throws IOException,
            AodvUnknownTypeException, AodvUnknownDestException, NoConnectionException {

        switch (message.getHeader().getType()) {
            case "RREQ":
                if (listenerAodv != null) listenerAodv.receivedRREQ(message);
                processRREQ(message);
                // Increment sequence number
                getNextSequenceNumber();
                break;
            case "RREP":
                if (listenerAodv != null) listenerAodv.receivedRREP(message);
                processRREP(message);
                // Increment sequence number
                getNextSequenceNumber();
                break;
            case "RREP_GRATUITOUS":
                if (listenerAodv != null) listenerAodv.receivedRREP_GRAT(message);
                processRREP_GRATUITOUS(message);
                // Increment sequence number
                getNextSequenceNumber();
                break;
            case "RERR":
                if (listenerAodv != null) listenerAodv.receivedRERR(message);
                processRERR(message);
                // Increment sequence number
                getNextSequenceNumber();
                break;
            case "DATA":
                processData(message);
                break;
            default:
                throw new AodvUnknownTypeException("Unknown AODV Type");
        }
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

        stringBuilder.append("--------Routing Table:--------\n");

        for (Map.Entry<String, EntryRoutingTable> entry : aodvHelper.getEntrySet()) {
            stringBuilder.append(entry.getValue().toString()).append("\n");
        }

        if (mapDestSequenceNumber.size() > 0) {
            stringBuilder.append("--------SequenceNumber:--------\n");
            for (Map.Entry<String, Long> entry : mapDestSequenceNumber.entrySet()) {
                stringBuilder.append(entry.getKey()).append(" -> ").append(entry.getValue().toString()).append("\n");
            }
        }

        Log.d(TAG, stringBuilder.toString());
    }

    /**
     * Method allowing to increment the sequence number
     */
    private void getNextSequenceNumber() {
        if (ownSequenceNum < Constants.MAX_VALID_SEQ_NUM) {
            ++ownSequenceNum;
        } else {
            ownSequenceNum = Constants.MIN_VALID_SEQ_NUM;
        }
    }

    /**
     * Method allowing to add a precursor to a node.
     *
     * @param precursorName a String value which represents the name of a precursor.
     * @return an ArrayList of String which represents the list of the precursors.
     */
    private ArrayList<String> addPrecursors(String precursorName) {

        ArrayList<String> precursors = new ArrayList<>();
        precursors.add(precursorName);
        return precursors;
    }

    /**
     * Method allowing to associate a destination sequence number with its destination.
     *
     * @param dest   a String value which represents the destination address node.
     * @param seqNum a long value which represents the destination sequence number.
     */
    private void saveDestSequenceNumber(String dest, long seqNum) {
        mapDestSequenceNumber.put(dest, seqNum);
    }

    /**
     * Method allowing to get a destination sequence number from its destination.
     *
     * @param dest a String value which represents the destination address node.
     * @return a long value which represents the destination sequence number.
     */
    private long getDestSequenceNumber(String dest) {
        if (mapDestSequenceNumber.containsKey(dest)) {
            return mapDestSequenceNumber.get(dest);
        }
        return Constants.UNKNOWN_SEQUENCE_NUMBER;
    }

    public void getPaired() {
        dataLink.getPaired();
    }

    public void discovery() {
        dataLink.discovery();
    }
}
