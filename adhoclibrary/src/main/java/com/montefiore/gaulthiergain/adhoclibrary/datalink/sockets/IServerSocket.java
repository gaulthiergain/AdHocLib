package com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets;

import java.io.IOException;

/**
 * <p>This interface must be implemented by the {@link AdHocServerSocketBluetooth} and
 * {@link AdHocServerSocketWifi} classes.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface IServerSocket {

    /**
     * Method allowing to get a BluetoothSocket/Socket object.
     *
     * @return a BluetoothSocket/Socket object depending the type of connection.
     */
    Object getSocket();

    /**
     * Method allowing to close a connection.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    void close() throws IOException;

    /**
     * Method allowing to accept an incoming connection.
     *
     * @return a BluetoothSocket/Socket object depending the type of connection.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    Object accept() throws IOException;
}
