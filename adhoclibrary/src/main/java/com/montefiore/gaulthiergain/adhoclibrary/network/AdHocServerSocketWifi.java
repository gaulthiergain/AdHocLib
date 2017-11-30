package com.montefiore.gaulthiergain.adhoclibrary.network;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by gaulthiergain on 30/11/17.
 */

public class AdHocServerSocketWifi implements IServerSocket {

    private ServerSocket serverSocket;

    public AdHocServerSocketWifi(ServerSocket serverSocket) {
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
