package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;

import java.io.IOException;

/**
 * <p>This interface allows to define callback functions between routing protocol and data-link
 * manager.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface ListenerDataLink {

    /**
     * Method allowing to notify when device's information is initialized in underlying wrapper(s).
     *
     * @param macAddress a String value which represents the MAC address of the device.
     * @param deviceName a String value which represents the name of the device.
     */
    void initInfos(String macAddress, String deviceName);

    /**
     * Method allowing to notify when a link with a remote device is broken.
     *
     * @param remoteNode a String which represents the node address that has broken link.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    void brokenLink(String remoteNode) throws IOException;

    /**
     * Method allowing to notify when a message from a remote peer is received and processed by
     * underlying wrapper(s).
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     */
    void processMsgReceived(MessageAdHoc message);
}
