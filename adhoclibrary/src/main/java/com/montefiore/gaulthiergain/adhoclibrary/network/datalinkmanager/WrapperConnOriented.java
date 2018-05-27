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

/**
 * <p>This class contains common variables and methods between connection-oriented wrapper
 * classes.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
abstract class WrapperConnOriented extends AbstractWrapper {

    final short nbThreads;
    final HashMap<String, SocketManager> mapAddrNetwork;
    final Neighbors neighbors;

    private final HashMap<String, AdHocDevice> mapAddrDevices;

    short attempts;
    ServiceServer serviceServer;

    /**
     * Constructor
     *
     * @param v                a boolean value to set the debug/verbose mode.
     * @param config           a Config object which contains specific configurations.
     * @param nbThreads        a short value to determine the number of threads managed by the
     *                         server.
     * @param mapAddressDevice a HashMap<String, AdHocDevice> which maps a UUID address entry to an
     *                         AdHocDevice object.
     * @param listenerApp      a ListenerApp object which contains callback functions.
     * @param listenerDataLink a ListenerDataLink object which contains callback functions.
     */
    WrapperConnOriented(boolean v, Config config, short nbThreads, HashMap<String, AdHocDevice> mapAddressDevice,
                        ListenerApp listenerApp, ListenerDataLink listenerDataLink) {
        super(v, config, mapAddressDevice, listenerApp, listenerDataLink);
        this.neighbors = new Neighbors();
        this.nbThreads = nbThreads;
        this.mapAddrDevices = new HashMap<>();
        this.mapAddrNetwork = new HashMap<>();
    }

    /**
     * Method allowing to disconnect the mobile from a a particular destination.
     *
     * @param remoteDest a String value which represents the current address of the destination.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    void disconnect(String remoteDest) throws IOException {

        SocketManager socketManager = neighbors.getNeighbor(remoteDest);
        if (socketManager != null) {
            socketManager.closeConnection();
            neighbors.remove(remoteDest);
        }
    }

    /**
     * Method allowing to check if a node is a direct neighbour.
     *
     * @param address a String value which represents the address of the remote device.
     * @return a boolean value which is true if the device is a direct neighbors. Otherwise, false.
     */
    boolean isDirectNeighbors(String address) {
        return neighbors.getNeighbors().containsKey(address);
    }

    /**
     * Method allowing to disconnect the mobile from a remote mobile.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    void disconnectAll() throws IOException {

        if (neighbors.getNeighbors().size() > 0) {
            for (Map.Entry<String, SocketManager> entry : neighbors.getNeighbors().entrySet()) {
                entry.getValue().closeConnection();
            }
            neighbors.clear();
        }
    }

    /**
     * Method allowing to send a message to a remote peer.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @param address a String value which represents the address of the remote device.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    void sendMessage(MessageAdHoc message, String address) throws IOException {

        SocketManager socketManager = neighbors.getNeighbors().get(address);
        if (socketManager != null) {
            socketManager.sendMessage(message);
        }
    }

    /**
     * Method allowing to broadcast a message to all directly connected nodes excepted the excluded
     * node.
     *
     * @param message         a MessageAdHoc object which represents the message to send through
     *                        the network.
     * @param excludedAddress a String value which represents the excluded address.
     * @return a boolean value which is true if the broadcast was successful. Otherwise, false.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
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

    /**
     * Method allowing to broadcast a message to all directly connected nodes.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    boolean broadcast(MessageAdHoc message) throws IOException {
        if (neighbors.getNeighbors().size() > 0) {
            for (Map.Entry<String, SocketManager> entry : neighbors.getNeighbors().entrySet()) {
                entry.getValue().sendMessage(message);
            }
            return true;
        }
        return false;
    }

    /**
     * Method allowing to get the direct neighbours of the current mobile.
     *
     * @return an ArrayList<AdHocDevice> object which represents the direct neighbours of the current mobile.
     */
    public ArrayList<AdHocDevice> getDirectNeighbors() {
        ArrayList<AdHocDevice> arrayList = new ArrayList<>();
        for (String mac : neighbors.getLabelMac().values()) {
            arrayList.add(mapAddrDevices.get(mac));
        }
        return arrayList;
    }

    /**
     * Method allowing to process the disconnection of a remote node.
     *
     * @param remoteAddress a String value which represents the address of the remote device.
     * @throws IOException           signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException signals that a no connection exception has occurred.
     */
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

    /**
     * Method allowing to process the connection of a remote node.
     *
     * @param header        an Header object which represents the header of a ad hoc message.
     * @param socketManager a SocketManager object which contains the remote connection socket.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
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
