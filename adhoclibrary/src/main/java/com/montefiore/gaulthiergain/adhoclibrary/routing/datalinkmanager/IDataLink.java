package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;

/**
 * This interface allows to perform management of data layer from routing layer.
 */
public interface IDataLink {

    /**
     * Method allowing to connect to other devices
     *
     * @param hashMap
     */
    void connect(HashMap<String, DiscoveredDevice> hashMap);

    /**
     * Method allowing to stop the threads.
     *
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    void stopListening() throws IOException;

    /**
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @param address a String value which represents the destination address.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    void sendMessage(MessageAdHoc message, String address) throws IOException;

    /**
     * Method allowing to check if the address is a neighbor of the current node.
     *
     * @param address a String value which represents the address of a remove node.
     * @return a boolean value which is true if the address is from a neighbor, otherwise false.
     */
    boolean isDirectNeighbors(String address);

    /**
     * Method allowing to broadcast a message to all remote nodes.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    void broadcast(MessageAdHoc message) throws IOException;

    /**
     * @param originateAddr a String value which represents the address of a originator node.
     * @param message       a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    void broadcastExcept(String originateAddr, MessageAdHoc message)
            throws IOException;


    void discovery();

    void getPaired();


}
