package com.montefiore.gaulthiergain.adhoclibrary.aodv;

public class EntryRoutingTable {
    private final String destIpAddress;
    private final String next;
    private final int hop;
    private final long seq;

    public EntryRoutingTable(String destIpAddress, String next, int hop, long seq) {
        this.destIpAddress = destIpAddress;
        this.next = next;
        this.hop = hop;
        this.seq = seq;
    }

    @Override
    public String toString() {
        return "- dst: " + destIpAddress +
                " nxt: " + next +
                " hop: " + hop +
                " seq: " + seq;
    }

    public String getDestIpAddress() {
        return destIpAddress;
    }

    public String getNext() {
        return next;
    }

    public int getHop() {
        return hop;
    }

    public long getSeq() {
        return seq;
    }
}
