package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class WrapperConnOriented extends AbstractWrapper {

    final short attemps;
    final short nbThreads;
    final boolean background;
    final Neighbors neighbors;
    final HashMap<String, SocketManager> mapAddrNetwork;

    private final HashMap<String, AdHocDevice> mapAddrDevices;

    ServiceServer serviceServer;

    WrapperConnOriented(boolean v, Context context, Config config, short nbThreads, HashMap<String, AdHocDevice> mapAddressDevice,
                        ListenerApp listenerApp, ListenerDataLink listenerDataLink) {
        super(v, context, config.isJson(), config.getLabel(),
                mapAddressDevice, listenerApp, listenerDataLink);
        this.neighbors = new Neighbors();
        this.attemps = config.getAttemps();
        this.nbThreads = nbThreads;
        this.background = config.isBackground();
        this.mapAddrDevices = new HashMap<>();
        this.mapAddrNetwork = new HashMap<>();
    }

    public void disconnect(String remoteLabel) throws IOException {

        SocketManager socketManager = neighbors.getNeighbor(remoteLabel);
        if (socketManager != null) {
            socketManager.closeConnection();
            neighbors.remove(remoteLabel);
        }
    }

    public boolean isDirectNeighbors(String address) {
        return neighbors.getNeighbors().containsKey(address);
    }

    public void disconnectAll() throws IOException {

        if (neighbors.getNeighbors().size() > 0) {
            for (Map.Entry<String, SocketManager> entry : neighbors.getNeighbors().entrySet()) {
                entry.getValue().closeConnection();
            }
            neighbors.getNeighbors().clear();
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

    void connectionClosed(String remoteAddress) throws IOException, NoConnectionException {

        // Get adHocDevice from address
        AdHocDevice adHocDevice = mapAddrDevices.get(remoteAddress);
        if (adHocDevice != null) {

            // Remove device from neighbors and devices hashmap
            neighbors.getNeighbors().remove(adHocDevice.getLabel());
            mapAddrDevices.remove(remoteAddress);

            // Remove device from Network hashmap
            if (mapAddrNetwork.containsKey(remoteAddress)) {
                mapAddrNetwork.remove(remoteAddress);
            }

            // Callback broken link (network)
            listenerDataLink.brokenLink(adHocDevice.getLabel());

            // Callback connection closed
            listenerApp.onConnectionClosed(adHocDevice);
        } else {
            throw new NoConnectionException("Error while closing connection");
        }
    }

    void receivedPeerMsg(Header header, SocketManager socketManager) {

        AdHocDevice device = new AdHocDevice(header.getLabel(), header.getMac(),
                header.getName(), type);

        // Add mapping address (UUID) - AdHoc device
        mapAddrDevices.put(header.getMac(), device);

        // Add the active connection into the neighbors object
        neighbors.addNeighbors(header.getLabel(), socketManager);

        // Callback connection
        listenerApp.onConnection(device);
    }
}
