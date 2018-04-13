package com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * <p>This class allows to encapsulate a Socket object as a AdHocSocketWifi object and add
 * an abstraction by implementing the IServerSocket interface({@link IServerSocket}).</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class AdHocSocketWifi implements ISocket {

    private Socket socket;

    /**
     * @param socket a Socket object which is an endpoint for communication
     *               between two machines.
     */
    public AdHocSocketWifi(Socket socket) {
        this.socket = socket;
    }

    @Override
    public String getRemoteSocketAddress() {
        if (socket.getRemoteSocketAddress() != null) {
            return socket.getRemoteSocketAddress().toString().split(":")[0].substring(1);
        }

        return null;
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

}
