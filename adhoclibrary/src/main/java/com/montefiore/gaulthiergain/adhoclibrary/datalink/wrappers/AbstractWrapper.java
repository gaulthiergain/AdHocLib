package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.Neighbors;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractWrapper {

    final static short ATTEMPTS = 3;

    final static byte CONNECT_SERVER = 10;
    final static byte CONNECT_CLIENT = 11;

    final boolean v;
    final boolean json;
    final Context context;
    final short nbThreads;
    final boolean background;
    final ListenerApp listenerApp;
    final Neighbors neighbors;
    final HashMap<String, String> mapLabelAddr;
    final ListenerDataLink listenerDataLink;
    final HashMap<String, AdHocDevice> mapAddressDevice;

    byte type;
    String label;
    String ownMac;
    String ownName;
    boolean enabled;
    boolean discoveryCompleted;
    DataLinkManager.ListenerDiscovery discoveryListener;

    AbstractWrapper(boolean v, Context context, boolean json, short nbThreads, boolean background, String label,
                    HashMap<String, AdHocDevice> mapAddressDevice,
                    Neighbors neighbors,
                    ListenerApp listenerApp, ListenerDataLink listenerDataLink) {

        this.v = v;
        this.json = json;
        this.background = background;
        this.enabled = true;
        this.context = context;
        this.label = label;
        this.nbThreads = nbThreads;
        this.discoveryCompleted = false;
        this.listenerApp = listenerApp;
        this.mapLabelAddr = new HashMap<>();
        this.mapAddressDevice = mapAddressDevice;
        this.neighbors = neighbors;
        this.listenerDataLink = listenerDataLink;
    }

    public abstract void connect(AdHocDevice device);

    public abstract void stopListening() throws IOException;

    public abstract void discovery();

    public abstract HashMap<String, AdHocDevice> getPaired();

    public abstract void enable(int duration, ListenerAdapter listenerAdapter);

    public abstract void disable();

    public abstract void unregisterConnection();

    public abstract void disconnect();

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

    public void sendMessage(MessageAdHoc message, String address) throws IOException {

        NetworkObject networkObject = neighbors.getNeighbors().get(address);
        if (networkObject != null && networkObject.getType() == type) {
            SocketManager socketManager = (SocketManager) networkObject.getSocketManager();
            socketManager.sendMessage(message);
        }
    }

    public void broadcastExcept(MessageAdHoc message, String excludedAddress) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : neighbors.getNeighbors().entrySet()) {
            if (entry.getValue().getType() == type) {
                if (!entry.getKey().equals(excludedAddress)) {
                    SocketManager socketManager = (SocketManager) entry.getValue().getSocketManager();
                    socketManager.sendMessage(message);
                }
            }
        }
    }

    public void broadcast(MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : neighbors.getNeighbors().entrySet()) {
            if (entry.getValue().getType() == type) {
                SocketManager socketManager = (SocketManager) entry.getValue().getSocketManager();
                socketManager.sendMessage(message);
            }
        }
    }

    public abstract void unregisterAdapter();
}
