package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import android.util.Log;

import java.util.HashMap;
import java.util.Hashtable;

public class RoutingTable {
    private static final String TAG = "[AdHoc][RoutingTable]";
    private final Hashtable<String, EntryRoutingTable> routingTable;
    private final HashMap<String, String> nxtDestMapping;

    RoutingTable() {
        this.routingTable = new Hashtable<>();
        this.nxtDestMapping = new HashMap<>();
    }

    boolean addEntry(EntryRoutingTable entry) {

        if (!routingTable.containsKey(entry.getDestIpAddress())) {
            Log.d(TAG, "Add new Entry in the RIB " + entry.getDestIpAddress());
            routingTable.put(entry.getDestIpAddress(), entry);
            //add nxt dest mapping --> for RERR
            nxtDestMapping.put(entry.getNext(), entry.getDestIpAddress());
            return true;
        }

        // Get existing entry
        EntryRoutingTable existingEntry = routingTable.get(entry.getDestIpAddress());

        // Compare hop between the two entries and take the lowest (todo test SEQ --> Fresh route)
        if (existingEntry.getHop() >= entry.getHop()) {
            // Add new Entry
            routingTable.put(entry.getDestIpAddress(), entry);
            //add nxt dest mapping --> for RERR
            nxtDestMapping.put(entry.getNext(), entry.getDestIpAddress());

            Log.d(TAG, "Entry: " + existingEntry.getDestIpAddress().substring(24, 36)
                    + " hops: " + existingEntry.getHop()
                    + " is replaced by " + entry.getDestIpAddress().substring(24, 36)
                    + " hops: " + entry.getHop());
            return true;
        }

        Log.d(TAG, "Entry: " + existingEntry.getDestIpAddress().substring(24, 36)
                + " hops: " + existingEntry.getHop()
                + " is NOT replaced by " + entry.getDestIpAddress().substring(24, 36)
                + " hops: " + entry.getHop());

        return false;
    }

    public void removeEntry(String destIpAddress) {
        routingTable.remove(destIpAddress);
    }

    EntryRoutingTable getNextFromDest(String dest) {
        return routingTable.get(dest);
    }

    public boolean containsDest(String address) {
        return routingTable.containsKey(address);
    }

    public Hashtable<String, EntryRoutingTable> getRoutingTable() {
        return routingTable;
    }

    public boolean containsNxt(String next) {
        return nxtDestMapping.containsKey(next);
    }

    public String getDestFromNxt(String next) {
        return nxtDestMapping.get(next);
    }
}
