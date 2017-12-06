package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import java.net.InetAddress;

/**
 * Created by gaulthiergain on 26/10/17.
 */

public interface ConnectionListener {
    void onConnectionStarted();

    void onConnectionFailed(int reasonCode);

    void onGroupOwner(InetAddress groupOwnerAddr);

    void onClient(InetAddress groupOwnerAddr);
}
