package com.montefiore.gaulthiergain.adhoclibrary.auto;

import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by gaulthiergain on 8/02/18.
 */

public interface ListenerAutoConnect {
    void connected(UUID uuid, NetworkObject network) throws IOException, NoConnectionException;
}
