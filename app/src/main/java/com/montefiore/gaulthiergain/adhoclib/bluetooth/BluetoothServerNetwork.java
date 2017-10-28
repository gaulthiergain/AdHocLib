package com.montefiore.gaulthiergain.adhoclib.bluetooth;

/**
 * Created by gaulthiergain on 28/10/17.
 */


import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class provides methods to manage network communications. It allows to
 * open/close connections and to send/receive messages.
 *
 * @author Gain Gaulthier
 */
public class BluetoothServerNetwork {

    private BluetoothServerSocket serverSocket;

    /**
     * Constructor.
     *
     * @param serverSocket a stream socket connected to the specified client.
     */
    public BluetoothServerNetwork(BluetoothServerSocket serverSocket) throws IOException {
        this.serverSocket = serverSocket;
    }

    public void closeSocket() throws IOException {
        serverSocket.close();
    }

    public BluetoothSocket accept() throws IOException {
        return serverSocket.accept();
    }

    public BluetoothServerSocket getServerSocket() {
        return serverSocket;
    }
}
