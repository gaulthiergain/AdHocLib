package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public abstract class WrapperConnOriented extends AbstractWrapper {

    final static short ATTEMPTS = 3;

    final Neighbors neighbors;
    final short nbThreads;
    final boolean background;
    final HashMap<String, String> mapAddrLabel;
    final HashMap<String, SocketManager> mapAddrNetwork;

    ServiceServer serviceServer;

    WrapperConnOriented(boolean v, Context context, boolean json, short nbThreads, boolean background,
                        String label, HashMap<String, AdHocDevice> mapAddressDevice,
                        ListenerApp listenerApp, ListenerDataLink listenerDataLink) {
        super(v, context, json, label, mapAddressDevice, listenerApp, listenerDataLink);
        this.neighbors = new Neighbors();
        this.nbThreads = nbThreads;
        this.background = background;
        this.mapAddrLabel = new HashMap<>();
        this.mapAddrNetwork = new HashMap<>();
    }

    public void disconnect(String remoteLabel) throws IOException, NoConnectionException {

        SocketManager socketManager = neighbors.getNeighbor(remoteLabel);
        if (socketManager != null) {
            socketManager.closeConnection();
            neighbors.remove(remoteLabel);
        } else {
            throw new NoConnectionException("No connection to " + remoteLabel);
        }
    }

    public boolean isDirectNeighbors(String address) {
        return neighbors.getNeighbors().containsKey(address);
    }

    public void disconnectAll() throws IOException, NoConnectionException {

        if (neighbors.getNeighbors().size() > 0) {
            for (Map.Entry<String, SocketManager> entry : neighbors.getNeighbors().entrySet()) {
                entry.getValue().closeConnection();
            }
            neighbors.getNeighbors().clear();
        } else {
            throw new NoConnectionException("No connection");
        }
    }

    public void sendMessage(MessageAdHoc message, String address) throws IOException {

        SocketManager socketManager = neighbors.getNeighbors().get(address);
        if (socketManager != null) {
            socketManager.sendMessage(message);
        }
    }

    public void broadcastExcept(MessageAdHoc message, String excludedAddress) throws IOException {

        if (neighbors.getNeighbors().size() > 0) {
            for (Map.Entry<String, SocketManager> entry : neighbors.getNeighbors().entrySet()) {
                if (!entry.getKey().equals(excludedAddress)) {
                    entry.getValue().sendMessage(message);
                }
            }
        }
    }

    public void broadcast(MessageAdHoc message) throws IOException {
        if (neighbors.getNeighbors().size() > 0) {
            for (Map.Entry<String, SocketManager> entry : neighbors.getNeighbors().entrySet()) {
                entry.getValue().sendMessage(message);
            }
        }
    }
}
