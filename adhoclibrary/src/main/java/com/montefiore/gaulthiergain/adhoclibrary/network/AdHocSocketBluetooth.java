package com.montefiore.gaulthiergain.adhoclibrary.network;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AdHocSocketBluetooth implements ISocket {

    private BluetoothSocket socket;

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

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }

}
