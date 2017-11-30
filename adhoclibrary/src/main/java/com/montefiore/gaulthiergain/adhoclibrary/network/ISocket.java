package com.montefiore.gaulthiergain.adhoclibrary.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ISocket {

    String getRemoteSocketAddress();
    Object getSocket();

    void close() throws IOException;

    OutputStream getOutputStream() throws IOException;
    InputStream getInputStream() throws IOException;

    boolean isConnected();
}
