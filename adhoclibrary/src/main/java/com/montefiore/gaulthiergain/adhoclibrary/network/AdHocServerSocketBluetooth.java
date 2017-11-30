package com.montefiore.gaulthiergain.adhoclibrary.network;

import android.bluetooth.BluetoothServerSocket;

import java.io.IOException;

/**
 * Created by gaulthiergain on 30/11/17.
 */

public class AdHocServerSocketBluetooth implements IServerSocket {
    private BluetoothServerSocket serverSocket;

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
