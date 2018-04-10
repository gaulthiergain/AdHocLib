package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.DeviceAlreadyConnectedException;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractWrapper {

    final static byte CONNECT_SERVER = 10;
    final static byte CONNECT_CLIENT = 11;
    final static byte CONNECT_BROADCAST = 12;
    final static byte DISCONNECT_BROADCAST = 13;

    public final static byte BROADCAST = 14;

    int type;
    final boolean v;
    final boolean json;
    final Context context;
    final boolean connectionFlooding;

    final ListenerDataLink listenerDataLink;
    final HashMap<String, AdHocDevice> mapMacDevices;

    String label;
    String ownMac;
    String ownName;
    boolean enabled;
    boolean discoveryCompleted;
    ListenerApp listenerApp;
    DataLinkManager.ListenerDiscovery discoveryListener;

    Set<String> setFloodEvents;

    AbstractWrapper(boolean v, Context context, Config config, HashMap<String, AdHocDevice> mapMacDevices,
                    ListenerApp listenerApp, ListenerDataLink listenerDataLink) {

        this.v = v;
        this.json = config.isJson();
        this.enabled = true;
        this.connectionFlooding = config.isConnectionFlooding();
        if (connectionFlooding) {
            // Use set only if connectionFlooding option is enabled
            this.setFloodEvents = new HashSet<>();
        }
        this.context = context;
        this.label = config.getLabel();
        this.discoveryCompleted = false;
        this.listenerApp = listenerApp;
        this.mapMacDevices = mapMacDevices;
        this.listenerDataLink = listenerDataLink;
    }

    public abstract void connect(AdHocDevice device) throws DeviceAlreadyConnectedException;

    public abstract void stopListening() throws IOException;

    public abstract void discovery();

    public abstract HashMap<String, AdHocDevice> getPaired();

    public abstract void enable(int duration, ListenerAdapter listenerAdapter) throws BluetoothBadDuration;

    public abstract void disable();

    public abstract void unregisterConnection();

    public abstract void resetDeviceName();

    public abstract boolean updateDeviceName(String name);

    public abstract void init(Config config) throws IOException;

    public abstract void unregisterAdapter();

    public abstract void sendMessage(MessageAdHoc message, String address) throws IOException;

    public abstract boolean isDirectNeighbors(String address);

    public abstract void broadcastExcept(MessageAdHoc message, String excludedAddress) throws IOException;

    public abstract void broadcast(MessageAdHoc message) throws IOException;

    public abstract void disconnect(String remoteDest) throws IOException;

    public abstract void disconnectAll() throws IOException;

    public abstract String getAdapterName();

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

    public int getType() {
        return type;
    }

    public String getMac() {
        return ownMac;
    }

    boolean checkFloodEvent(MessageAdHoc message) throws IOException {

        String id = (String) message.getPdu();

        if (!setFloodEvents.contains(id)) {
            setFloodEvents.add(id);
            broadcastExcept(message, message.getHeader().getLabel());

            return true;
        }

        return false;
    }

    public void updateListener(ListenerApp listenerApp) {
        this.listenerApp = listenerApp;
    }
}
