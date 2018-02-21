package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import android.util.Log;

import java.util.HashMap;
import java.util.Hashtable;

/**
 * <p>This class represents the routing table for the AODV protocol. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
class RoutingTable {

    private static final String TAG = "[AdHoc][RoutingTable]";
    private final boolean v;
    private final Hashtable<String, EntryRoutingTable> routingTable;
    private final HashMap<String, String> nextDestMapping;

    /**
     * Constructor
     *
     * @param verbose a boolean value to set the debug/verbose mode.
     */
    RoutingTable(boolean verbose) {
        this.v = verbose;
        this.routingTable = new Hashtable<>();
        this.nextDestMapping = new HashMap<>();
    }

    /**
     * Method allowing to add a new entry in the routing table.
     *
     * @param entry an EntryRoutingTable object which represents an entry in the routing table.
     * @return a boolean value which is true if the entry has been added otherwise false.
     */
    boolean addEntry(EntryRoutingTable entry) {

        if (!routingTable.containsKey(entry.getDestIpAddress())) {
            if (v) Log.d(TAG, "Add new Entry in the RIB " + entry.getDestIpAddress());
            routingTable.put(entry.getDestIpAddress(), entry);
            // Add next destination mapping --> for RERR
            nextDestMapping.put(entry.getNext(), entry.getDestIpAddress());
            return true;
        }

        // Get existing entry
        EntryRoutingTable existingEntry = routingTable.get(entry.getDestIpAddress());

        // Compare hop between the two entries and take the lowest. Take also fresh routes
        if (existingEntry.getHop() >= entry.getHop() && entry.getSeq() >= existingEntry.getSeq()) {

            // Add new Entry
            routingTable.put(entry.getDestIpAddress(), entry);
            // Add next dest mapping --> for RERR
            nextDestMapping.put(entry.getNext(), entry.getDestIpAddress());

            if (v) Log.d(TAG, "Entry: " + existingEntry.getDestIpAddress()
                    + " hops: " + existingEntry.getHop()
                    + " is replaced by " + entry.getDestIpAddress()
                    + " hops: " + entry.getHop());
            return true;
        }

        if (v) Log.d(TAG, "Entry: " + existingEntry.getDestIpAddress()
                + " hops: " + existingEntry.getHop()
                + " is NOT replaced by " + entry.getDestIpAddress()
                + " hops: " + entry.getHop());

        return false;
    }

    /**
     * Method allowing to remove an entry in the routing table from the destination IP address.
     *
     * @param destIpAddress a String value which represents the destination IP address.
     */
    void removeEntry(String destIpAddress) {
        routingTable.remove(destIpAddress);
    }

    /**
     * Method allowing to get the entry associated to the destination IP address.
     *
     * @param destIpAddress a String value which represents the destination IP address.
     * @return an EntryRoutingTable object associated to the destination IP address.
     */
    EntryRoutingTable getNextFromDest(String destIpAddress) {
        return routingTable.get(destIpAddress);
    }

    /**
     * Method allowing to check if the destination IP address is in the routing table.
     *
     * @param destIpAddress a String value which represents the destination IP address.
     * @return a boolean value which is true if the destination address is in the routing table,
     * otherwise false.
     */
    boolean containsDest(String destIpAddress) {
        return routingTable.containsKey(destIpAddress);
    }

    /**
     * Method allowing to check if the next hop IP address is in the routing table.
     *
     * @param next a String value which represents the next hop address to reach the destination
     *             IP address.
     * @return true if the next hop address is in the routing table, otherwise false.
     */
    boolean containsNext(String next) {
        return nextDestMapping.containsKey(next);
    }

    /**
     * Method allowing to get the destination address from the next hop address.
     *
     * @param next a String value which represents the next hop address to reach the destination
     *             IP address.
     * @return a String value which represents the destination IP address.
     */
    String getDestFromNext(String next) {
        return nextDestMapping.get(next);
    }

    /**
     * Method allowing to get the routing table.
     *
     * @return a Hashtable<String, EntryRoutingTable> which represents the routing table.
     */
    Hashtable<String, EntryRoutingTable> getRoutingTable() {
        return routingTable;
    }
}
