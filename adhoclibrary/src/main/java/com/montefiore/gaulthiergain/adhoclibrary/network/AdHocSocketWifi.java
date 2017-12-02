package com.montefiore.gaulthiergain.adhoclibrary.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class AdHocSocketWifi implements ISocket {

    private Socket socket;

    public AdHocSocketWifi(Socket socket) {
        this.socket = socket;
    }

    @Override
    public String getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress().toString().split(":")[0].substring(1);
    }

    public Socket getSocket() {
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
