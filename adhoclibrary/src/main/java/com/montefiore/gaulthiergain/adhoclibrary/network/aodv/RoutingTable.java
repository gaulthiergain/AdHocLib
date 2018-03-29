package com.montefiore.gaulthiergain.adhoclibrary.network.aodv;

import android.util.Log;

import java.util.ArrayList;
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
        if (existingEntry.getHop() >= entry.getHop()) {
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
     * @param destAddress a String value which represents the destination address.
     */
    void removeEntry(String destAddress) {
        routingTable.remove(destAddress);
    }

    /**
     * Method allowing to get the entry associated to the destination address.
     *
     * @param destAddress a String value which represents the destination address.
     * @return an EntryRoutingTable object associated to the destination address.
     */
    EntryRoutingTable getNextFromDest(String destAddress) {
        return routingTable.get(destAddress);
    }

    /**
     * Method allowing to check if the destination IP address is in the routing table.
     *
     * @param destAddress a String value which represents the destination address.
     * @return a boolean value which is true if the destination address is in the routing table,
     * otherwise false.
     */
    boolean containsDest(String destAddress) {
        return routingTable.containsKey(destAddress);
    }

    /**
     * Method allowing to check if the next hop IP address is in the routing table.
     *
     * @param nextAddress a String value which represents the next hop address to reach the destination
     *                    IP address.
     * @return true if the next hop address is in the routing table, otherwise false.
     */
    boolean containsNext(String nextAddress) {
        return nextDestMapping.containsKey(nextAddress);
    }

    /**
     * Method allowing to get the destination address from the next hop address.
     *
     * @param nextAddress a String value which represents the next hop address to reach the destination
     *                    IP address.
     * @return a String value which represents the destination IP address.
     */
    String getDestFromNext(String nextAddress) {
        return nextDestMapping.get(nextAddress);
    }

    /**
     * Method allowing to get the routing table.
     *
     * @return a Hashtable<String, EntryRoutingTable> which represents the routing table.
     */
    Hashtable<String, EntryRoutingTable> getRoutingTable() {
        return routingTable;
    }

    /**
     * Method allowing to get a routing table entry associated to the destination address.
     *
     * @param destIpAddress a String value which represents the destination IP address.
     * @return a EntryRoutingTable object associated to the destination address.
     */
    public EntryRoutingTable getDestination(String destIpAddress) {
        return routingTable.get(destIpAddress);
    }

    /**
     * Method allowing to get the precursors of a node.
     *
     * @param destAddress a String value which represents the destination IP address.
     * @return an ArrayList of string which represents the precursors list.
     */
    public ArrayList<String> getPrecursorsFromDest(String destAddress) {

        EntryRoutingTable entry = routingTable.get(destAddress);
        if (entry != null) {
            return entry.getPrecursors();
        }
        return null;

    }

    /**
     * Method allowing to get data from the destination address.
     *
     * @param address a String value which represents the destination address.
     * @return a long value which represents the last time that data has been transmitted.
     */
    public long getDataPathFromAddress(String address) {
        EntryRoutingTable entry = routingTable.get(address);
        if (entry != null) {
            return entry.getActivesDataPath(address);
        }

        return 0;
    }
}