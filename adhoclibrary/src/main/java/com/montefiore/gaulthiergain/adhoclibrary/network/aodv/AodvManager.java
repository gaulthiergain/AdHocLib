package com.montefiore.gaulthiergain.adhoclibrary.network.aodv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvAbstractException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvMessageException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownTypeException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>This class represents the core of the AODV protocols. It manages all the messages and the
 * node behaviour. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

public class AodvManager {

    private static final String TAG = "[AdHoc][AodvManager]";

    private final boolean v;
    private final AodvHelper aodvHelper;
    private final HashMap<String, Long> mapDestSequenceNumber;

    private String ownName;
    private String ownMac;
    private String ownAddress;
    private long ownSequenceNum;
    private ListenerApp listenerApp;
    private DataLinkManager dataLink;
    private MessageAdHoc dataMessage;
    private ListenerDataLink listenerDataLink;

    /**
     * Constructor
     *
     * @param verbose     a boolean value to set the debug/verbose mode.
     * @param listenerApp a ListenerApp object which contains callback functions.
     */
    private AodvManager(boolean verbose, final ListenerApp listenerApp) {
        this.v = verbose;
        this.aodvHelper = new AodvHelper(v);
        this.ownSequenceNum = Constants.FIRST_SEQUENCE_NUMBER;
        this.listenerApp = listenerApp;
        this.mapDestSequenceNumber = new HashMap<>();
        if (v) {
            // Print routing table
            this.initTimerDebugRIB();
        }
        this.listenerDataLink = new ListenerDataLink() {

            @Override
            public void initInfos(String macAddress, String deviceName) {
                ownMac = macAddress;
                ownName = deviceName;
            }

            @Override
            public void brokenLink(String remoteNode) throws IOException {
                brokenLinkDetected(remoteNode);
            }

            @Override
            public void processMsgReceived(MessageAdHoc message) {
                try {
                    processAodvMsgReceived(message);
                } catch (IOException | AodvAbstractException e) {
                    listenerApp.processMsgException(e);
                }
            }
        };
    }

    /**
     * Constructor wifi
     *
     * @param verbose     a boolean value to set the debug/verbose mode.
     * @param context     a Context object which gives global information about an application
     *                    environment.
     * @param config      a Config object which contains specific configurations.
     * @param listenerApp a ListenerApp object which contains callback functions.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public AodvManager(boolean verbose, Context context, Config config, ListenerApp listenerApp)
            throws IOException {

        this(verbose, listenerApp);

        this.ownAddress = config.getLabel();
        this.dataLink = new DataLinkManager(verbose, context, config, listenerApp, listenerDataLink);
    }

    /**
     * Method allowing to send a message to a remote address.
     *
     * @param pdu     an Object value which represents the PDU of the message.
     * @param address a String value which represents the destination address.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void sendMessageTo(Object pdu, String address) throws IOException {

        // Create MessageAdHoc object
        Header header = new Header(TypeAodv.DATA.getType(), ownMac, ownAddress, ownName);

        MessageAdHoc msg = new MessageAdHoc(header, new Data(address, pdu));

        send(msg, address);
    }

    //---------------------------------------Private methods---------------------------------------/

    /**
     * Method allowing to detect if a link is broken
     *
     * @param remoteNode a String which represents the destination address
     * @throws IOException signals that an I/O exception of some sort has occurred.
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
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @param address a String value which represents the destination address.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    private void sendDirect(MessageAdHoc message, String address) throws IOException {
        // The destination is directly connected
        dataLink.sendMessage(message, address);
    }

    /**
     * Method allowing to send a message to a remote address.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @param address a String value which represents the destination address.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    private void send(MessageAdHoc message, String address) throws IOException {

        if (dataLink.isDirectNeighbors(address)) {

            EntryRoutingTable destNext = aodvHelper.getNextfromDest(address);
            if (destNext != null && message.getHeader().getType() == TypeAodv.DATA.getType()) {
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

                if (message.getHeader().getType() == TypeAodv.DATA.getType()) {
                    // Update dataPath
                    destNext.updateDataPath(address);
                }

                // Send message to remote device
                sendDirect(message, destNext.getNext());
            }
        } else if (message.getHeader().getType() == TypeAodv.RERR.getType()) {
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
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    private void processRREQ(MessageAdHoc message) throws IOException {

        // Get the RREQ message
        RREQ rreq = (RREQ) message.getPdu();

        // Get previous hop and previous source address
        int hop = rreq.getHopCount();
        String originateAddr = message.getHeader().getLabel();

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
                send(new MessageAdHoc(new Header(TypeAodv.RREP.getType(), ownAddress, ownName), rrep),
                        entry.getNext());

                //Run timer for this reverse route
                timerFlushReverseRoute(rreq.getOriginIpAddress(), rreq.getOriginSequenceNum());
            }
        } else if (aodvHelper.containsDest(rreq.getDestIpAddress())) {
            // Send RREP GRATUITOUS to destination
            sendRREP_GRAT(message.getHeader().getLabel(), rreq);
        } else {
            // Broadcast RREQ
            if (rreq.getOriginIpAddress().equals(ownAddress)) {
                if (v) Log.d(TAG, "Reject own RREQ " + rreq.getOriginIpAddress());
            } else if (aodvHelper.addBroadcastId(rreq.getOriginIpAddress(), rreq.getRreqId())) {

                // Update PDU and Header
                rreq.incrementHopCount();
                message.setPdu(rreq);
                message.setHeader(new Header(TypeAodv.RREQ.getType(), ownAddress, ownName));

                // Broadcast message to all directly connected devices
                dataLink.broadcastExcept(message, originateAddr);

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
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @throws IOException              signals that an I/O exception of some sort has occurred.
     * @throws AodvUnknownDestException signals that an AodvUnknownDestException has occurred.
     */
    private void processRREP(MessageAdHoc message) throws IOException, AodvUnknownDestException {

        // Get the RREP message
        RREP rrep = (RREP) message.getPdu();

        // Get previous hop and previous source address
        int hopRcv = rrep.getHopCount();
        String nextHop = message.getHeader().getLabel();

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
                send(new MessageAdHoc(new Header(TypeAodv.RREP.getType(), ownAddress, ownName)
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
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @throws IOException              signals that an I/O exception of some sort has occurred.
     * @throws AodvUnknownDestException signals that an AodvUnknownDestException has occurred.
     */
    private void processRREP_GRATUITOUS(MessageAdHoc message) throws IOException, AodvUnknownDestException {

        // Get the RREP message
        RREP rrep = (RREP) message.getPdu();

        int hopCount = rrep.incrementHopCount();

        if (rrep.getDestIpAddress().equals(ownAddress)) {
            if (v) Log.d(TAG, ownAddress + " is the destination (stop RREP)");

            // Update routing table
            aodvHelper.addEntryRoutingTable(rrep.getOriginIpAddress(), message.getHeader().getLabel(),
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
                aodvHelper.addEntryRoutingTable(rrep.getOriginIpAddress(), message.getHeader().getLabel(),
                        hopCount, rrep.getSequenceNum(), rrep.getLifetime(), addPrecursors(destNext.getNext()));

                // Add timer for reverse route
                timerFlushReverseRoute(rrep.getOriginIpAddress(), rrep.getSequenceNum());

                // Update header
                message.setHeader(new Header(TypeAodv.RREP_GRATUITOUS.getType(), ownAddress, ownName));

                // Send message to the next destination
                send(message, destNext.getNext());
            }

        }
    }

    /**
     * Method allowing to process a RERR message.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    private void processRERR(MessageAdHoc message) throws IOException {

        // Get the RERR message
        RERR rerr = (RERR) message.getPdu();

        // Get previous source address
        String originateAddr = message.getHeader().getLabel();

        if (v) Log.d(TAG, "Received RERR from " + originateAddr + " -> Node "
                + rerr.getUnreachableDestIpAddress() + " is unreachable");


        if (rerr.getUnreachableDestIpAddress().equals(ownAddress)) {
            if (v) Log.d(TAG, "RERR received on the destination (stop forward)");
        } else if (aodvHelper.containsDest(rerr.getUnreachableDestIpAddress())) {

            message.setHeader(new Header(TypeAodv.RERR.getType(), ownAddress, ownName));
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
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @throws IOException              signals that an I/O exception of some sort has occurred.
     * @throws AodvUnknownDestException signals that an AodvUnknownDestException has occurred.
     */
    private void processData(MessageAdHoc message) throws IOException, AodvUnknownDestException {

        // Get the DATA message
        Data data = (Data) message.getPdu();

        if (v) Log.d(TAG, "Data message received from: " + message.getHeader().getLabel());

        if (data.getDestIpAddress().equals(ownAddress)) {
            if (v) Log.d(TAG, ownAddress + " is the destination (stop DATA message)");
            if (listenerApp != null) {

                // Get header
                Header header = message.getHeader();
                AdHocDevice adHocDevice = new AdHocDevice(header.getLabel(), header.getMac(),
                        header.getName(), header.getDeviceType());

                // Call listener
                listenerApp.onReceivedData(adHocDevice, data.getPayload());
            }
        } else {
            // Forward the DATA message to the destination by checking the routing table
            EntryRoutingTable destNext = aodvHelper.getNextfromDest(data.getDestIpAddress());
            if (destNext == null) {
                throw new AodvUnknownDestException("No destNext found in the routing Table for " +
                        data.getDestIpAddress());
            } else {
                if (v) Log.d(TAG, "Destination reachable via " + destNext.getNext());

                // Get header
                Header header = message.getHeader();
                AdHocDevice adHocDevice = new AdHocDevice(header.getLabel(), header.getMac(),
                        header.getName(), header.getDeviceType());

                // Call listener
                listenerApp.onForwardData(adHocDevice, data.getPayload());

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
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    private void startTimerRREQ(final String destAddr, final int retry, final int time) throws IOException {

        // No destination was found, send RREQ request (with timer)
        if (v) Log.d(TAG, "No connection to " + destAddr + " -> send RREQ message");

        // Broadcast message to all directly connected devices
        MessageAdHoc message = new MessageAdHoc(new Header(TypeAodv.RREQ.getType(), ownAddress, ownName),
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

                        @SuppressLint("HandlerLeak") final Handler mHandler = new Handler(Looper.getMainLooper()) {
                            // Used handler to avoid updating views in other threads than the main thread
                            public void handleMessage(Message msg) {
                                listenerApp.processMsgException(
                                        new AodvMessageException("Unable to establish a communication with: " + destAddr));
                            }
                        };

                        if (v) Log.d(TAG, "Expired time: no RREP received for " + destAddr);
                        mHandler.obtainMessage(1).sendToTarget();
                        dataMessage = null;
                    } else {
                        if (v) Log.d(TAG, "Expired time: no RREP received for " + destAddr +
                                " Retry: " + retry);
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
     * @throws IOException signals that an I/O exception of some sort has occurred.
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
                        send(new MessageAdHoc(new Header(TypeAodv.RERR.getType(), ownAddress, ownName), rrer),
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
     * @throws IOException signals that an I/O exception of some sort has occurred.
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
        send(new MessageAdHoc(new Header(TypeAodv.RREP_GRATUITOUS.getType(), ownAddress, ownName), rrep),
                entry.getNext());
        if (v) Log.d(TAG, "Send Gratuitous RREP to " + entry.getNext());

        // Generate RREP message for the source
        rrep = new RREP(TypeAodv.RREP.getType(), entry.getHop() + 1,
                rreq.getOriginIpAddress(), entry.getDestSeqNum(), entry.getDestIpAddress(), Constants.LIFE_TIME);

        // Send RREP message to the source
        send(new MessageAdHoc(new Header(TypeAodv.RREP.getType(), ownAddress, ownName), rrep),
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
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @throws IOException              signals that an I/O exception of some sort has occurred.
     * @throws AodvUnknownTypeException signals that a Unknown AODV type has been caught.
     * @throws AodvUnknownDestException signals that an AodvUnknownDestException has occurred.
     */
    private void processAodvMsgReceived(MessageAdHoc message) throws IOException,
            AodvUnknownTypeException, AodvUnknownDestException {

        switch (message.getHeader().getType()) {
            case Constants.RREQ:
                processRREQ(message);
                // Increment sequence number
                getNextSequenceNumber();
                break;
            case Constants.RREP:
                processRREP(message);
                // Increment sequence number
                getNextSequenceNumber();
                break;
            case Constants.RREP_GRATUITOUS:
                processRREP_GRATUITOUS(message);
                // Increment sequence number
                getNextSequenceNumber();
                break;
            case Constants.RERR:
                processRERR(message);
                // Increment sequence number
                getNextSequenceNumber();
                break;
            case Constants.DATA:
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
        }, Constants.DELAY, Constants.PERIOD);
    }

    /**
     * Method allowing to display the routing table in debug mode.
     */
    private void updateRoutingTable() {

        boolean display = false;
        StringBuilder stringBuilder = new StringBuilder();

        if (aodvHelper.getEntrySet().size() > 0) {
            display = true;
            stringBuilder.append("--------Routing Table:--------\n");

            for (Map.Entry<String, EntryRoutingTable> entry : aodvHelper.getEntrySet()) {
                stringBuilder.append(entry.getValue().toString()).append("\n");
            }
        }

        if (mapDestSequenceNumber.size() > 0) {
            display = true;
            stringBuilder.append("--------SequenceNumber:--------\n");
            for (Map.Entry<String, Long> entry : mapDestSequenceNumber.entrySet()) {
                stringBuilder.append(entry.getKey()).append(" -> ").append(entry.getValue().toString()).append("\n");
            }
        }

        if (display) {
            Log.d(TAG, stringBuilder.toString());
        }
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

    /**
     * Method allowing to get a DataLinkManager object.
     *
     * @return a DataLinkManager object which allows to manage Wrappers.
     */
    public DataLinkManager getDataLink() {
        return dataLink;
    }

    /**
     * Method allowing to update the ListenerApp.
     *
     * @param listenerApp a ListenerApp object which contains callback functions.
     */
    public void updateListener(ListenerApp listenerApp) {
        this.listenerApp = listenerApp;
        this.dataLink.updateListener(listenerApp);
    }
}
