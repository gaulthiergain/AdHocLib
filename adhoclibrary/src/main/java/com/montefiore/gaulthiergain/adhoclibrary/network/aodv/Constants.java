package com.montefiore.gaulthiergain.adhoclibrary.network.aodv;

/**
 * <p>This class defines some constants for AODV protocol. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface Constants {

    byte INIT_HOP_COUNT = 0;
    short RREQ_RETRIES = 2;

    //Sequence Numbers
    long MIN_VALID_SEQ_NUM = 0;
    long MAX_VALID_SEQ_NUM = Long.MAX_VALUE;
    long UNKNOWN_SEQUENCE_NUMBER = -1; //TODO change to 0
    long FIRST_SEQUENCE_NUMBER = 1;
    long SEQUENCE_NUMBER_INTERVAL = (Integer.MAX_VALUE / 2);

    // AODV PDU types
    byte RREQ = 1;
    byte RERR = 3;
    byte RREP = 2;
    byte RREP_GRATUITOUS = 4;
    byte DATA = 8;
    byte HELLO = 9;

    int NET_TRANVERSAL_TIME = 2800;
    int EXPIRED_TABLE = 10000;
    int EXPIRED_TIME = EXPIRED_TABLE * 2;

    byte NO_LIFE_TIME = -1;
    //alive time for a route
    int LIFE_TIME = EXPIRED_TIME;

    //the time to wait between each hello message sent
    int BROADCAST_INTERVAL = 1;

    //the time of hello packets
    int HELLO_PACKET_INTERVAL_SND = 30000;
    int HELLO_PACKET_INTERVAL = HELLO_PACKET_INTERVAL_SND / 2;

    // Constants for displaying the routing table
    int DELAY = 60000;
    int PERIOD = DELAY;
}
