package com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * <p>This class allows to encapsulate a ServerSocket object as a AdHocServerSocketWifi object and
 * add an abstraction by implementing the IServerSocket interface({@link IServerSocket}).</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class AdHocServerSocketWifi implements IServerSocket {

    private ServerSocket serverSocket;

    /**
     * Constructor
     *
     * @param serverSocket a serverSocket object which is similar to TCP sockets.
     */
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
