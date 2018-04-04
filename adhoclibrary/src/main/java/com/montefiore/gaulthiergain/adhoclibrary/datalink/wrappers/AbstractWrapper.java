package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;

public abstract class AbstractWrapper {

    final static byte CONNECT_SERVER = 10;
    final static byte CONNECT_CLIENT = 11;
    final static byte CLOSE_CONNECTION = 12;

    final boolean v;
    final boolean json;
    final Context context;

    final ListenerApp listenerApp;
    final ListenerDataLink listenerDataLink;
    final HashMap<String, AdHocDevice> mapMacDevice;

    byte type;
    String label;
    String ownMac;
    String ownName;
    boolean enabled;
    boolean discoveryCompleted;
    DataLinkManager.ListenerDiscovery discoveryListener;


    AbstractWrapper(boolean v, Context context, boolean json, String label,
                    HashMap<String, AdHocDevice> mapMacDevice,
                    ListenerApp listenerApp, ListenerDataLink listenerDataLink) {

        this.v = v;
        this.json = json;
        this.enabled = true;
        this.context = context;
        this.label = label;
        this.discoveryCompleted = false;
        this.listenerApp = listenerApp;
        this.mapMacDevice = mapMacDevice;
        this.listenerDataLink = listenerDataLink;
    }

    public abstract void connect(AdHocDevice device);

    public abstract void stopListening() throws IOException;

    public abstract void discovery();

    public abstract HashMap<String, AdHocDevice> getPaired();

    public abstract void enable(int duration, ListenerAdapter listenerAdapter);

    public abstract void disable();

    public abstract void unregisterConnection();

    public abstract void updateName(String name);

    public abstract void listenServer() throws IOException;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDiscoveryCompleted() {
        return discoveryCompleted;
    }

    public void resetDiscoveryFlag() {
        this.discoveryCompleted = false;
    }

    public void setDiscoveryListener(DataLinkManager.ListenerDiscovery discoveryListener) {
        this.discoveryListener = discoveryListener;
    }

    public abstract void unregisterAdapter();

    public abstract void sendMessage(MessageAdHoc message, String address) throws IOException;

    public abstract boolean isDirectNeighbors(String address);

    public abstract void broadcastExcept(MessageAdHoc message, String excludedAddress) throws IOException;

    public abstract void broadcast(MessageAdHoc message) throws IOException;

    public abstract void disconnect(String remoteDest) throws IOException, NoConnectionException;

    public abstract void disconnectAll() throws IOException, NoConnectionException;
}
