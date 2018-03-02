package com.montefiore.gaulthiergain.adhoclibrary.routing.aodv;

public interface Constants {

    byte INIT_HOP_COUNT = 0;
    short RREQ_RETRIES = 2;

    //Sequence Numbers
    long MIN_VALID_SEQ_NUM = 0;
    long MAX_VALID_SEQ_NUM = Long.MAX_VALUE;
    long INVALID_SEQUENCE_NUMBER = -1;
    long UNKNOWN_SEQUENCE_NUMBER = -1; //TODO change to 0
    long FIRST_SEQUENCE_NUMBER = 1;
    long SEQUENCE_NUMBER_INTERVAL = (Integer.MAX_VALUE / 2);

    // AODV PDU types
    byte RREQ = 1;
    byte RERR = 3;
    byte RREP = 2;
    byte RREP_GRATUITOUS = 4;
    byte DATA = 8;

    int NET_TRANVERSAL_TIME = 2800;
    int EXPIRED_TABLE = 10000;
    int EXPIRED_TIME = EXPIRED_TABLE * 2;

    byte NO_LIFE_TIME = -1;
    //alive time for a route
    int LIFE_TIME = EXPIRED_TIME;

    //the time to wait between each hello message sent
    int BROADCAST_INTERVAL = 1;

    //the amount of time to store a RREQ entry before the entry dies
    int PATH_DESCOVERY_TIME = 3000;

    //energy constants
    int FIRST_THRESHOLD = 15;
    int SECOND_THRESHOLD = 5;


}
