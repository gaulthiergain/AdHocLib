package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ActiveConnections;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DataLinkHybridManager;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DiscoveredDevice;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ListenerAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

public abstract class AbstractWrapper {

    final static short ATTEMPTS = 3;

    final boolean v;
    final Context context;

    final ListenerAodv listenerAodv;
    final ActiveConnections activeConnections;
    final ListenerDataLinkAodv listenerDataLinkAodv;

    String ownMac;
    String ownName;
    boolean discoveryCompleted;
    DataLinkHybridManager.ListenerDiscovery discoveryListener;

    AbstractWrapper(boolean v, Context context, ActiveConnections activeConnections,
                    ListenerAodv listenerAodv, ListenerDataLinkAodv listenerDataLinkAodv) {

        this.v = v;
        this.context = context;
        this.discoveryCompleted = false;
        this.listenerAodv = listenerAodv;
        this.activeConnections = activeConnections;
        this.listenerDataLinkAodv = listenerDataLinkAodv;

    }

    public abstract void listenServer(short nbThreadsWifi) throws IOException;

    public abstract void discovery();

    public abstract void connect(DiscoveredDevice device);

    public abstract void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException;

    public abstract void stopListening() throws IOException;

    public abstract void getPaired();

    public abstract boolean isEnabled();

    public boolean isDiscoveryCompleted() {
        return discoveryCompleted;
    }

    public void resetDiscoveryFlag() {
        this.discoveryCompleted = false;
    }

    public void setDiscoveryListener(DataLinkHybridManager.ListenerDiscovery discoveryListener) {
        this.discoveryListener = discoveryListener;
    }

    public abstract void unregisterConnection();
}
