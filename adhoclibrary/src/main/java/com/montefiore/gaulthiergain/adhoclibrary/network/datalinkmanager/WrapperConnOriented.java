package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

abstract class WrapperConnOriented extends AbstractWrapper {

    final short nbThreads;
    final HashMap<String, SocketManager> mapAddrNetwork;
    final Neighbors neighbors;

    private final HashMap<String, AdHocDevice> mapAddrDevices;

    short attemps;
    ServiceServer serviceServer;

    WrapperConnOriented(boolean v, Config config, short nbThreads, HashMap<String, AdHocDevice> mapAddressDevice,
                        ListenerApp listenerApp, ListenerDataLink listenerDataLink) {
        super(v, config, mapAddressDevice, listenerApp, listenerDataLink);
        this.neighbors = new Neighbors();
        this.nbThreads = nbThreads;
        this.mapAddrDevices = new HashMap<>();
        this.mapAddrNetwork = new HashMap<>();
    }

    void disconnect(String remoteLabel) throws IOException {

        SocketManager socketManager = neighbors.getNeighbor(remoteLabel);
        if (socketManager != null) {
            socketManager.closeConnection();
            neighbors.remove(remoteLabel);
        }
    }

    boolean isDirectNeighbors(String address) {
        return neighbors.getNeighbors().containsKey(address);
    }

    void disconnectAll() throws IOException {

        if (neighbors.getNeighbors().size() > 0) {
            for (Map.Entry<String, SocketManager> entry : neighbors.getNeighbors().entrySet()) {
                entry.getValue().closeConnection();
            }
            neighbors.clear();
        }
    }

    void sendMessage(MessageAdHoc message, String address) throws IOException {

        SocketManager socketManager = neighbors.getNeighbors().get(address);
        if (socketManager != null) {
            socketManager.sendMessage(message);
        }
    }

    boolean broadcastExcept(MessageAdHoc message, String excludedAddress) throws IOException {

        if (neighbors.getNeighbors().size() > 0) {
            for (Map.Entry<String, SocketManager> entry : neighbors.getNeighbors().entrySet()) {
                if (!entry.getKey().equals(excludedAddress)) {
                    entry.getValue().sendMessage(message);
                }
            }

            return true;
        }

        return false;
    }

    boolean broadcast(MessageAdHoc message) throws IOException {
        if (neighbors.getNeighbors().size() > 0) {
            for (Map.Entry<String, SocketManager> entry : neighbors.getNeighbors().entrySet()) {
                entry.getValue().sendMessage(message);
            }
            return true;
        }
        return false;
    }

    public ArrayList<AdHocDevice> getDirectNeighbors() {
        ArrayList<AdHocDevice> arrayList = new ArrayList<>();
        for (String mac : neighbors.getLabelMac().values()) {
            arrayList.add(mapAddrDevices.get(mac));
        }
        return arrayList;
    }

    void connectionClosed(String remoteAddress) throws IOException, NoConnectionException {

        // Get adHocDevice from address
        AdHocDevice adHocDevice = mapAddrDevices.get(remoteAddress);
        if (adHocDevice != null) {

            // Remove device from neighbors and devices hashmap
            neighbors.remove(adHocDevice.getLabel());
            mapAddrDevices.remove(remoteAddress);

            // Remove device from Network hashmap
            if (mapAddrNetwork.containsKey(remoteAddress)) {
                mapAddrNetwork.remove(remoteAddress);
            }

            // Callback broken link (network)
            listenerDataLink.brokenLink(adHocDevice.getLabel());

            // Callback connection closed
            listenerApp.onConnectionClosed(adHocDevice);

            // If connectionFlooding option is enable, flood disconnect events
            if (connectionFlooding) {
                String id = adHocDevice.getLabel() + System.currentTimeMillis();
                setFloodEvents.add(id);
                Header header = new Header(AbstractWrapper.DISCONNECT_BROADCAST,
                        adHocDevice.getMacAddress(), adHocDevice.getLabel(), adHocDevice.getDeviceName(),
                        adHocDevice.getType());
                broadcastExcept(new MessageAdHoc(header, id), adHocDevice.getLabel());

                // Update set
                if (setRemoteDevices.contains(adHocDevice)) {
                    setRemoteDevices.remove(adHocDevice);
                }
            }
        } else {
            throw new NoConnectionException("Error while closing connection");
        }
    }

    void receivedPeerMsg(Header header, SocketManager socketManager) throws IOException {

        boolean event = false;

        AdHocDevice device = new AdHocDevice(header.getLabel(), header.getMac(),
                header.getName(), type);

        // Add mapping address (UUID/IP) - AdHoc device
        mapAddrDevices.put(header.getMac(), device);

        // Check if the device is already in neighbors
        if (!neighbors.getNeighbors().containsKey(header.getLabel())) {
            event = true;
        }

        if (event) {

            // Add the active connection into the neighbors object
            neighbors.addNeighbors(header.getLabel(), header.getMac(), socketManager);

            // Callback connection
            listenerApp.onConnection(device);

            // Update set
            setRemoteDevices.add(device);

            // If connectionFlooding option is enable, flood connect events
            if (connectionFlooding) {
                String id = header.getLabel() + System.currentTimeMillis();
                setFloodEvents.add(id);
                header.setType(AbstractWrapper.CONNECT_BROADCAST);
                broadcastExcept(new MessageAdHoc(header, new FloodMsg(id, setRemoteDevices)), header.getLabel());

                header.setType(AbstractWrapper.CONNECT_BROADCAST);
                sendMessage(new MessageAdHoc(header, new FloodMsg(id, setRemoteDevices)), header.getLabel());
            }

        }
    }
}
