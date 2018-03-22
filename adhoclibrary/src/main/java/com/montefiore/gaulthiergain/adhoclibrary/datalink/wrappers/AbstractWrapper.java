package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.applayer.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ActiveConnections;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DiscoveredDevice;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;

public abstract class AbstractWrapper {

    final static short ATTEMPTS = 3;

    final static byte CONNECT_SERVER = 0;
    final static byte CONNECT_CLIENT = 1;

    final boolean v;
    final Context context;
    final ListenerApp listenerApp;
    final ActiveConnections activeConnections;
    final HashMap<String, String> mapLabelAddr;
    final ListenerDataLink listenerDataLink;
    final HashMap<String, DiscoveredDevice> mapAddressDevice;

    String label;
    String ownMac;
    String ownName;
    boolean enabled;
    boolean discoveryCompleted;
    DataLinkManager.ListenerDiscovery discoveryListener;

    AbstractWrapper(boolean v, Context context, String label,
                    HashMap<String, DiscoveredDevice> mapAddressDevice,
                    ActiveConnections activeConnections,
                    ListenerApp listenerApp, ListenerDataLink listenerDataLink) {

        this.v = v;
        this.enabled = true;
        this.context = context;
        this.label = label;
        this.discoveryCompleted = false;
        this.listenerApp = listenerApp;
        this.mapLabelAddr = new HashMap<>();
        this.mapAddressDevice = mapAddressDevice;
        this.activeConnections = activeConnections;
        this.listenerDataLink = listenerDataLink;
    }

    public abstract void discovery();

    public abstract void disconnect();

    public abstract void connect(DiscoveredDevice device);

    public abstract void stopListening() throws IOException;

    public abstract void getPaired();

    public abstract boolean isEnabled();

    public boolean isDiscoveryCompleted() {
        return discoveryCompleted;
    }

    public void resetDiscoveryFlag() {
        this.discoveryCompleted = false;
    }

    public void setDiscoveryListener(DataLinkManager.ListenerDiscovery discoveryListener) {
        this.discoveryListener = discoveryListener;
    }

    public abstract void unregisterConnection();

    public abstract void enable(int duration);
}
