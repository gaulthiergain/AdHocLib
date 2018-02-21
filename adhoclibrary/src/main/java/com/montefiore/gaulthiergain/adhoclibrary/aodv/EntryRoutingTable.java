package com.montefiore.gaulthiergain.adhoclibrary.aodv;

/**
 * <p>This class represents routing table entries for AODV protocol. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

public class EntryRoutingTable {

    private final String destIpAddress;
    private final String next;
    private final int hop;
    private final long seq;

    /**
     * Constructor
     *
     * @param destIpAddress a String value which represents the destination IP address.
     * @param next          a String value which represents the next hop to reach the destination
     *                      IP address.
     * @param hop           an integer value which represents the hops number of the destination.
     * @param seq           an integer value which represents the sequence number.
     */
    EntryRoutingTable(String destIpAddress, String next, int hop, long seq) {
        this.destIpAddress = destIpAddress;
        this.next = next;
        this.hop = hop;
        this.seq = seq;
    }

    /**
     * Method allowing to get the destination IP address stored in the routing table.
     *
     * @return a String value which represents the destination IP address.
     */
    String getDestIpAddress() {
        return destIpAddress;
    }

    /**
     * Method allowing to get the next hop stored in the routing table.
     *
     * @return a String value which represents the next hop to reach the destination IP address.
     */
    String getNext() {
        return next;
    }

    /**
     * Method allowing to get he hops number stored in the routing table.
     *
     * @return an integer value which represents the hops number of the destination.
     */
    int getHop() {
        return hop;
    }

    /**
     * Method allowing to get the sequence number stored in the routing table.
     *
     * @return an integer value which represents the sequence number.
     */
    long getSeq() {
        return seq;
    }

    @Override
    public String toString() {
        return "- dst: " + destIpAddress +
                " nxt: " + next +
                " hop: " + hop +
                " seq: " + seq;
    }
}
