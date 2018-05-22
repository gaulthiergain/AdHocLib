package com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>This interface must be implemented by the {@link AdHocSocketBluetooth} and
 * {@link AdHocSocketWifi} classes.
 * </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface ISocket {

    /**
     * Method allowing to get a BluetoothSocket/Socket object.
     *
     * @return a BluetoothSocket/Socket object depending the type of connection.
     */
    Object getSocket();

    /**
     * Method allowing to close a remote connection.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    void close() throws IOException;

    /**
     * Method allowing to return an output stream for this socket.
     *
     * @return an OutputStream object.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    OutputStream getOutputStream() throws IOException;

    /**
     * Method allowing to return an input stream for this socket.
     *
     * @return an OutputStream object.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    InputStream getInputStream() throws IOException;

    /**
     * Method allowing to return remote the socket address.
     *
     * @return a String value which represents the remote socket address.
     */
    String getRemoteSocketAddress();
}
