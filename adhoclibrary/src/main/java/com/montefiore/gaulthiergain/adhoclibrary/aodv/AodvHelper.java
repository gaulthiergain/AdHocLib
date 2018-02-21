package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import android.util.Log;

import java.util.HashSet;

/**
 * <p>This class allows to help the AodvManager by providing constants and by managing broadcast
 * requests. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

class AodvHelper {

    final static short UNSET_NUM_SEQ = 0;
    final static short RREQ_RETRIES = 2;
    final static short INIT_HOP_COUNT = 1;

    final static int NET_TRANVERSAL_TIME = 2800;
    final static int LIFE_TIME = 1000;
    final static int EXPIRED_TABLE = 60000; //todo update name and value
    final static int EXPIRED_TIME = EXPIRED_TABLE * 2;

    private final boolean v;
    private final static String TAG = "[AdHoc][AodvHelper]";
    private RoutingTable routingTable;
    private HashSet<String> entryBroadcast;

    private long rreqId;

    /**
     * Constructor
     *
     * @param verbose a boolean value to set the debug/verbose mode.
     */
    AodvHelper(boolean verbose) {
        this.v = verbose;
        this.routingTable = new RoutingTable(verbose);
        this.entryBroadcast = new HashSet<>();
        this.rreqId = 1;
    }

    /**
     * Method allowing to an entry to the routing table
     *
     * @param destIpAddress a String value which represents the destination IP address.
     * @param next          a String value which represents the next neighbor to reach the
     *                      destination IP address.
     * @param hop           an integer value which represents the number of hops between the source
     *                      and the destination.
     * @param seq           an integer value which represents the sequence number.
     * @return an EntryRoutingTable object which contains the new entry to the routing table.
     */
    EntryRoutingTable addEntryRoutingTable(String destIpAddress, String next, int hop, long seq) {
        EntryRoutingTable entry = new EntryRoutingTable(destIpAddress, next, hop, seq);
        if (routingTable.addEntry(entry)) {
            return entry;
        } else {
            return null;
        }
    }

    /**
     * Method allowing to control the broadcast requests by adding the couple
     * <Source IP address, rreqId> in a set.
     *
     * @param sourceAddress a String wich represents the source IP address that send the request.
     * @param rreqId        a long which represents the broadcast ID.
     * @return a boolean value which is true if the broadcastId has been added or false if the
     * broadcastId has already contained in the set.
     */
    boolean addBroadcastId(String sourceAddress, long rreqId) {
        String entry = sourceAddress + rreqId;
        if (!entryBroadcast.contains(entry)) {
            entryBroadcast.add(entry);
            if (v) Log.d(TAG, "Add " + entry + " into broadcast set");
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method allowing to get the AODV routing table.
     *
     * @return a RoutingTable object which contains all information about AODV routes.
     */
    RoutingTable getRoutingTable() {
        return routingTable;
    }


    /**
     * Method allowing to get the next neighbor address from the destination address.
     *
     * @param destAddress a String object which represents the destination IP address.
     * @return a EntryRoutingTable object which contains the entry associated to the destination IP
     * address.
     */
    EntryRoutingTable getNextfromDest(String destAddress) {
        return routingTable.getNextFromDest(destAddress);
    }

    /**
     * Method allowing to check if a destination address is contained in the routing table.
     *
     * @param destAddress a String object which represents the destination IP address.
     * @return a boolean value which is true if the destination is contained otherwise no.
     */
    boolean containsDest(String destAddress) {
        return routingTable.containsDest(destAddress);
    }

    /**
     * Method allowing to get the broadcastId.
     *
     * @return a long value which represents the broadcastId.
     */
    long getRreqId() {
        return rreqId;
    }

    /**
     * Method allowing to increment the broadcastId.
     *
     * @return a long value which represents the broadcastId incremented by one.
     */
    long getIncrementRreqId() {
        return rreqId++;
    }

}
