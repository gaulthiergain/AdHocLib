package com.montefiore.gaulthiergain.adhoclibrary.aodv;


import android.util.Log;

import java.util.HashSet;

class AodvHelper {

    final static short RREQ_RETRIES = 2;
    final static short INIT_HOP_COUNT = 1;

    final static int NET_TRANVERSAL_TIME = 2800;
    final static int LIFE_TIME = 1000;
    final static int EXPIRED_TABLE = 60000; //todo update name and value
    final static int EXPIRED_TIME = EXPIRED_TABLE * 2;

    private RoutingTable routingTable;
    private HashSet<String> entryBroadcast;
    private long rreqId;

    AodvHelper() {
        this.routingTable = new RoutingTable();
        this.entryBroadcast = new HashSet<>();
        this.rreqId = 1;
    }

    EntryRoutingTable addEntryRoutingTable(String destIpAddress, String next, int hop, long seq) {
        EntryRoutingTable entry = new EntryRoutingTable(destIpAddress, next, hop, seq);
        //todo temporaire remove after???
        if (routingTable.addEntry(entry)) {
            return entry;
        } else {
            return null;
        }
    }

    boolean addBroadcastId(String entry) {
        if (!entryBroadcast.contains(entry)) {
            entryBroadcast.add(entry);
            Log.d("[AodvHelper]", "Add " + entry + " into broadcast set");
            return true;
        } else {
            return false;
        }
    }

    RoutingTable getRoutingTable() {
        return routingTable;
    }

    HashSet<String> getEntryBroadcast() {
        return entryBroadcast;
    }

    EntryRoutingTable getNextfromDest(String address) {
        return routingTable.getNextFromDest(address);
    }

    boolean containsDest(String address) {
        return routingTable.containsDest(address);
    }

    long getRreqId() {
        return rreqId;
    }

    long getIncrementRreqId() {
        return rreqId++;
    }

}
