package com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets;

import android.bluetooth.BluetoothServerSocket;

import java.io.IOException;

/**
 * <p>This class allows to encapsulate a BluetoothServerSocket object as a AdHocServerSocketBluetooth
 * object and add an abstraction by implementing the IServerSocket interface({@link IServerSocket}).
 * </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

public class AdHocServerSocketBluetooth implements IServerSocket {
    private BluetoothServerSocket serverSocket;

    /**
     * Constructor
     *
     * @param serverSocket a serverSocket object which is similar to TCP sockets.
     */
    public AdHocServerSocketBluetooth(BluetoothServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public Object getSocket() {
        return serverSocket;
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }

    @Override
    public Object accept() throws IOException {
        return serverSocket.accept();
    }
}
