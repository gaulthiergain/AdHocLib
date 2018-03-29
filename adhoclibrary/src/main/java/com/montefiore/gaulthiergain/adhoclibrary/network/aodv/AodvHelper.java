package com.montefiore.gaulthiergain.adhoclibrary.network.aodv;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>This class allows to help the AodvManager by managing broadcast requests and routing table. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

class AodvHelper {

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
     * @param destIpAddress a String value which represents the destination address.
     * @param next          a String value which represents the next neighbor to reach the
     *                      destination address.
     * @param hop           an integer value which represents the number of hops between the source
     *                      and the destination.
     * @param seq           an integer value which represents the sequence number.
     * @param lifetime      a long value which represents the lifetime of the entry.
     * @param precursors    a list that contains the precursors of the current node.
     * @return an EntryRoutingTable object which contains the new entry to the routing table.
     */
    EntryRoutingTable addEntryRoutingTable(String destIpAddress, String next, int hop, long seq, long lifetime
            , ArrayList<String> precursors) {
        EntryRoutingTable entry = new EntryRoutingTable(destIpAddress, next, hop, seq, lifetime, precursors);
        if (routingTable.addEntry(entry)) {
            return entry;
        } else {
            return null;
        }
    }

    /**
     * Method allowing to control the broadcast requests by adding the couple
     * <Source address, rreqId> in a set.
     *
     * @param sourceAddress a String wich represents the source address that send the request.
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
     * Method allowing to get the next neighbor address from the destination address.
     *
     * @param destAddress a String object which represents the destination IP address.
     * @return a EntryRoutingTable object which contains the entry associated to the destination
     * address.
     */
    EntryRoutingTable getNextfromDest(String destAddress) {
        return routingTable.getNextFromDest(destAddress);
    }

    /**
     * Method allowing to check if a destination address is contained in the routing table.
     *
     * @param destAddress a String object which represents the destination address.
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

    /**
     * Method allowing to get a routing table entry associated to the destination address.
     *
     * @param destAddress a String value which represents the destination address.
     * @return a EntryRoutingTable object associated to the destination address.
     */
    public EntryRoutingTable getDestination(String destAddress) {
        return routingTable.getDestination(destAddress);
    }

    /**
     * Method allowing to remove an entry in the routing table from the destination address.
     *
     * @param destAddress a String value which represents the destination address.
     */
    public void removeEntry(String destAddress) {
        routingTable.removeEntry(destAddress);
    }

    /**
     * Method allowing to get the size of the routing table.
     *
     * @return an integer value which represents the size of the routing table.
     */
    public int sizeRoutingTable() {
        return routingTable.getRoutingTable().size();
    }

    /**
     * Method allowing to check if the next hop address is in the routing table.
     *
     * @param nextAddress a String value which represents the next hop address to reach the
     *                    destination address.
     * @return true if the next hop address is in the routing table, otherwise false.
     */
    public boolean containsNext(String nextAddress) {
        return routingTable.containsNext(nextAddress);
    }

    /**
     * Method allowing to get the destination address from the next hop address.
     *
     * @param nextAddress a String value which represents the next hop address to reach the
     *                    destination address.
     * @return a String value which represents the destination address.
     */
    public String getDestFromNext(String nextAddress) {
        return routingTable.getDestFromNext(nextAddress);
    }

    /**
     * Method allowing to get the routing table.
     *
     * @return a Set<Map.Entry<String, EntryRoutingTable>> which represents the routing table.
     */
    public Set<Map.Entry<String, EntryRoutingTable>> getEntrySet() {
        return routingTable.getRoutingTable().entrySet();
    }

    /**
     * Method allowing to get the precursors of a node.
     *
     * @param destAddress a String value which represents the destination address.
     * @return an ArrayList of string which represents the precursors list.
     */
    public ArrayList<String> getPrecursorsFromDest(String destAddress) {
        return routingTable.getPrecursorsFromDest(destAddress);
    }

    /**
     * Method allowing to get data from the destination address.
     *
     * @param address a String value which represents the destination address.
     * @return a long value which represents the last time that data has been transmitted.
     */
    public Long getDataPathFromAddress(String address) {
        return routingTable.getDataPathFromAddress(address);
    }
}
