package com.montefiore.gaulthiergain.adhoclibrary.aodv;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

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
    private final long destSeqNum;
    private final long lifetime;
    private ArrayList<String> precursors;
    private final ConcurrentHashMap<String, Long> activesDataPath;

    /**
     * Constructor
     *
     * @param destIpAddress a String value which represents the destination IP address.
     * @param next          a String value which represents the next hop to reach the destination
     *                      IP address.
     * @param hop           an integer value which represents the hops number of the destination.
     * @param destSeqNum    an integer value which represents the sequence number.
     * @param lifetime      a long value which represents the lifetime of the entry.
     * @param precursors    a list that contains the precursors of the current node.
     */
    EntryRoutingTable(String destIpAddress, String next, int hop, long destSeqNum, long lifetime,
                      ArrayList<String> precursors) {
        this.destIpAddress = destIpAddress;
        this.next = next;
        this.hop = hop;
        this.destSeqNum = destSeqNum;
        this.lifetime = lifetime;
        this.precursors = precursors;
        this.activesDataPath = new ConcurrentHashMap<>();
    }

    /**
     * Method allowing to update the Data Path (active data flow)
     *
     * @param key a String value which represents the address of a remote device.
     */
    public void updateDataPath(String key) {
        activesDataPath.put(key, System.currentTimeMillis());
    }


    public long getActivesDataPath(String ipAddress) {

        if (activesDataPath.containsKey(ipAddress)) {
            return activesDataPath.get(ipAddress);
        }

        return 0;
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
    long getDestSeqNum() {
        return destSeqNum;
    }

    /**
     * Method allowing to get the lifetime of the RREP message.
     *
     * @return a long value which represents the lifetime of the RREP message.
     */
    public long getLifetime() {
        return lifetime;
    }

    @Override
    public String toString() {
        return "- dst: " + destIpAddress +
                " nxt: " + next +
                " hop: " + hop +
                " seq: " + destSeqNum + displayPrecursors() +
                " dataPath " + activesDataPath.get(destIpAddress);

    }

    private String displayPrecursors() {

        if (precursors == null) {
            return "";
        }

        StringBuilder str = new StringBuilder(" precursors: {");
        for (String precursor : precursors) {
            str.append(precursor).append(" ");
        }
        str.append("}");
        return str.toString();
    }

    public ArrayList<String> getPrecursors() {
        return precursors;
    }

    public void updatePrecursors(String senderAddr) {
        if (precursors == null) {
            precursors = new ArrayList<>();
            precursors.add(senderAddr);
        } else if (!precursors.contains(senderAddr)) {
            precursors.add(senderAddr);
        }
    }
}
