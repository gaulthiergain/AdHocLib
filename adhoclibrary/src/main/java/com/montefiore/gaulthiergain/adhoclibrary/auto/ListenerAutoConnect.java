package com.montefiore.gaulthiergain.adhoclibrary.auto;

import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;

import java.io.IOException;
import java.util.UUID;

public interface ListenerAutoConnect {
    void connected(UUID uuid, NetworkObject network) throws IOException, NoConnectionException;
}
