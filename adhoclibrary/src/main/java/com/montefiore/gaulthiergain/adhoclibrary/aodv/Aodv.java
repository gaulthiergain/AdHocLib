package com.montefiore.gaulthiergain.adhoclibrary.aodv;


import android.util.Log;

import java.util.HashSet;

public class Aodv {

    public final static short RREQ_RETRIES = 2;
    public final static short INIT_HOP_COUNT = 1;

    public final static int NET_TRANVERSAL_TIME = 2800;
    public final static int LIFE_TIME = 1000;
    public final static int EXPIRED_TABLE = 60000; //todo update name and value
    public final static int EXPIRED_TIME = EXPIRED_TABLE * 2;

    private RoutingTable routingTable;
    private HashSet<String> entryBroadcast;
    private long rreqId;

    public Aodv() {
        this.routingTable = new RoutingTable();
        this.entryBroadcast = new HashSet<>();
        this.rreqId = 1;
    }

    public EntryRoutingTable addEntryRoutingTable(String destIpAddress, String next, int hop, long seq) {
        EntryRoutingTable entry = new EntryRoutingTable(destIpAddress, next, hop, seq);
        //todo temporaire remove after???
        if (routingTable.addEntry(entry)) {
            return entry;
        } else {
            return null;
        }
    }

    public boolean addBroadcastId(String entry) {
        if (!entryBroadcast.contains(entry)) {
            entryBroadcast.add(entry);
            Log.d("[Aodv]", "Add " + entry + " into broadcast set");
            return true;
        } else {
            return false;
        }
    }

    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    public HashSet<String> getEntryBroadcast() {
        return entryBroadcast;
    }

    public EntryRoutingTable getNextfromDest(String address) {
        return routingTable.getNextFromDest(address);
    }

    public boolean containsDest(String address) {
        return routingTable.containsDest(address);
    }

    public long getRreqId() {
        return rreqId;
    }

    public long getIncrementRreqId() {
        return rreqId++;
    }

}
