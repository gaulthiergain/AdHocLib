package com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>This class allows to encapsulate a BluetoothSocket object as a AdHocSocketBluetooth object and
 * add an abstraction by implementing the IServerSocket interface({@link IServerSocket}).</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class AdHocSocketBluetooth implements ISocket {

    private BluetoothSocket socket;

    /**
     * @param socket a Socket object which is an endpoint for communication
     *               between two machines.
     */
    public AdHocSocketBluetooth(BluetoothSocket socket) {
        this.socket = socket;
    }

    @Override
    public String getRemoteSocketAddress() {
        return socket.getRemoteDevice().getAddress();
    }

    public BluetoothSocket getSocket() {
        return socket;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

}
