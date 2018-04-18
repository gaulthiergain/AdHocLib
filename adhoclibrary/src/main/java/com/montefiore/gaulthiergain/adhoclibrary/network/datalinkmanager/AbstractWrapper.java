package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

abstract class AbstractWrapper {

    final static byte CONNECT_SERVER = 10;
    final static byte CONNECT_CLIENT = 11;
    final static byte CONNECT_BROADCAST = 12;
    final static byte DISCONNECT_BROADCAST = 13;
    final static byte BROADCAST = 14;

    int type;
    final boolean v;
    final boolean json;
    final boolean connectionFlooding;

    final ListenerDataLink listenerDataLink;
    final HashMap<String, AdHocDevice> mapMacDevices;

    String label;
    String ownMac;
    String ownName;
    boolean enabled;
    boolean discoveryCompleted;
    ListenerApp listenerApp;
    DataLinkManager.ListenerBothDiscovery listenerBothDiscovery;

    Set<String> setFloodEvents;

    AbstractWrapper(boolean v, Config config, HashMap<String, AdHocDevice> mapMacDevices,
                    ListenerApp listenerApp, ListenerDataLink listenerDataLink) {

        this.v = v;
        this.json = config.isJson();
        this.enabled = true;
        this.connectionFlooding = config.isConnectionFlooding();
        this.setFloodEvents = new HashSet<>();
        this.label = config.getLabel();
        this.discoveryCompleted = false;
        this.listenerApp = listenerApp;
        this.mapMacDevices = mapMacDevices;
        this.listenerDataLink = listenerDataLink;
    }

    abstract void connect(short attemps, AdHocDevice device) throws DeviceException;

    abstract void stopListening() throws IOException;

    abstract void discovery(DiscoveryListener discoveryListener);

    abstract HashMap<String, AdHocDevice> getPaired();

    abstract void enable(Context context, int duration, ListenerAdapter listenerAdapter) throws BluetoothBadDuration;

    abstract void disable();

    abstract void updateContext(Context context);

    abstract void unregisterConnection();

    abstract void resetDeviceName();

    abstract boolean updateDeviceName(String name);

    abstract void init(Config config, Context context) throws IOException;

    abstract void unregisterAdapter();

    abstract void sendMessage(MessageAdHoc message, String address) throws IOException;

    abstract boolean isDirectNeighbors(String address);

    abstract void broadcastExcept(MessageAdHoc message, String excludedAddress) throws IOException;

    abstract void broadcast(MessageAdHoc message) throws IOException;

    abstract void disconnect(String remoteDest) throws IOException;

    abstract void disconnectAll() throws IOException;

    abstract String getAdapterName();

    public boolean isEnabled() {
        return enabled;
    }

    boolean isDiscoveryCompleted() {
        return discoveryCompleted;
    }

    void resetDiscoveryFlag() {
        this.discoveryCompleted = false;
    }

    void setDiscoveryListener(DataLinkManager.ListenerBothDiscovery listenerBothDiscovery) {
        this.listenerBothDiscovery = listenerBothDiscovery;
    }

    int getType() {
        return type;
    }

    String getMac() {
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

    void updateListener(ListenerApp listenerApp) {
        this.listenerApp = listenerApp;
    }
}
