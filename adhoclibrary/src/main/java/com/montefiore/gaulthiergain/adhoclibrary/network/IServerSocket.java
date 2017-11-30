package com.montefiore.gaulthiergain.adhoclibrary.network;

import java.io.IOException;

/**
 * Created by gaulthiergain on 30/11/17.
 */

public interface IServerSocket {
    Object getSocket();

    void close() throws IOException;

    Object accept() throws IOException;
}
