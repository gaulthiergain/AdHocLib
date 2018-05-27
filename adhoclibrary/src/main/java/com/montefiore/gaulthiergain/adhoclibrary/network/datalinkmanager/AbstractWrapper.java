package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


/**
 * <p>This class defines the constants, parameters and methods for managing connections, and
 * messages handling and aims to serve as a common interface for service {@link WrapperBluetooth},
 * {@link WrapperWifi} and {@link WrapperWifiUdp}classes. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
abstract class AbstractWrapper {

    final static byte CONNECT_SERVER = 10;
    final static byte CONNECT_CLIENT = 11;
    final static byte CONNECT_BROADCAST = 12;
    final static byte DISCONNECT_BROADCAST = 13;
    final static byte BROADCAST = 14;

    int type;
    final int timeout;
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

    HashSet<AdHocDevice> setRemoteDevices;

    /**
     * Constructor
     *
     * @param v                a boolean value to set the debug/verbose mode.
     * @param config           a Config object which contains specific configurations.
     * @param mapMacDevices    a HashMap<String, AdHocDevice> which maps a MAC address entry to an
     *                         AdHocDevice object.
     * @param listenerApp      a ListenerApp object which contains callback functions.
     * @param listenerDataLink a ListenerDataLink object which contains callback functions.
     */
    AbstractWrapper(boolean v, Config config, HashMap<String, AdHocDevice> mapMacDevices,
                    ListenerApp listenerApp, ListenerDataLink listenerDataLink) {

        this.v = v;
        this.timeout = config.getTimeout();
        this.json = config.isJson();
        this.enabled = true;
        this.connectionFlooding = config.isConnectionFlooding();
        this.setFloodEvents = new HashSet<>();
        this.label = config.getLabel();
        this.discoveryCompleted = false;
        this.listenerApp = listenerApp;
        this.mapMacDevices = mapMacDevices;
        this.listenerDataLink = listenerDataLink;
        this.setRemoteDevices = new HashSet<>();
    }

    /**
     * Method allowing to connect to a remote peer.
     *
     * @param attempts    an integer value which represents the number of attempts to try to connect
     *                    to the remote peer.
     * @param adHocDevice an AdHocDevice object which represents the remote peer.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    abstract void connect(short attempts, AdHocDevice adHocDevice) throws DeviceException;

    /**
     * Method allowing to stop a listening on incoming connections.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    abstract void stopListening() throws IOException;

    /**
     * Method allowing to perform a discovery depending the technology used. If the Bluetooth and
     * Wi-Fi is enabled, the two discoveries are performed in parallel. A discovery stands for at
     * least 10/12 seconds.
     *
     * @param discoveryListener a DiscoveryListener object which contains callback function.
     */
    abstract void discovery(DiscoveryListener discoveryListener);

    /**
     * Method allowing to get all the Bluetooth devices which are already paired.
     *
     * @return a HashMap<String, AdHocDevice> object which contains all paired Bluetooth devices.
     */
    abstract HashMap<String, AdHocDevice> getPaired();

    /**
     * Method allowing to enabled a particular technology.
     *
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param duration        an integer value which is used to set up the time of the bluetooth discovery.
     * @param listenerAdapter a ListenerAdapter object which contains callback functions.
     * @throws BluetoothBadDuration signals that the duration for the bluetooth discovery is invalid.
     */
    abstract void enable(Context context, int duration, ListenerAdapter listenerAdapter) throws BluetoothBadDuration;

    /**
     * Method allowing to disabled a particular technology.
     */
    abstract void disable();

    /**
     * Method allowing to update the current context.
     *
     * @param context a Context object which gives global information about an application
     *                environment.
     */
    abstract void updateContext(Context context);

    /**
     * Method allowing to unregister broadcast receivers for Bluetooth and Wi-FI adapters.
     */
    abstract void unregisterConnection();

    /**
     * Method allowing to reset the adapter name of a particular technology.
     */
    abstract void resetDeviceName();

    /**
     * Method allowing to update the name of a particular technology.
     *
     * @param name a String value which represents the new name of the device adapter.
     * @return a boolean value which is true if the name was correctly updated. Otherwise, false.
     */
    abstract boolean updateDeviceName(String name);

    /**
     * Method allowing to initialize internal parameters.
     *
     * @param config  a Config object which contains specific configurations.
     * @param context a Context object which gives global information about an application
     *                environment.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    abstract void init(Config config, Context context) throws IOException;

    /**
     * Method allowing to unregister broadcast receivers for Bluetooth and Wi-FI adapters.
     */
    abstract void unregisterAdapter();

    /**
     * Method allowing to get the direct neighbors of the current device.
     *
     * @return an ArrayList<AdHocDevice> which contains the direct neighbors of the current device.
     */
    abstract ArrayList<AdHocDevice> getDirectNeighbors();

    /**
     * Method allowing to send a message to a remote peer.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @param address a String value which represents the address of the remote device.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    abstract void sendMessage(MessageAdHoc message, String address) throws IOException;

    /**
     * Method allowing to check if a node is a direct neighbour.
     *
     * @param address a String value which represents the address of the remote device.
     * @return a boolean value which is true if the device is a direct neighbors. Otherwise, false.
     */
    abstract boolean isDirectNeighbors(String address);

    /**
     * Method allowing to broadcast a message to all directly connected nodes excepted the excluded
     * node.
     *
     * @param message         a MessageAdHoc object which represents the message to send through
     *                        the network.
     * @param excludedAddress a String value which represents the excluded address.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    abstract boolean broadcastExcept(MessageAdHoc message, String excludedAddress) throws IOException;

    /**
     * Method allowing to broadcast a message to all directly connected nodes.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    abstract boolean broadcast(MessageAdHoc message) throws IOException;

    /**
     * Method allowing to disconnect the mobile from a a particular destination.
     *
     * @param remoteDest a String value which represents the current address of the destination.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    abstract void disconnect(String remoteDest) throws IOException;

    /**
     * Method allowing to disconnect all the outgoing connections.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    abstract void disconnectAll() throws IOException;

    /**
     * Method allowing to get a particular adapter name.
     *
     * @return a String which represents the name of a particular adapter name.
     */
    abstract String getAdapterName();

    /**
     * Method allowing to check if a particular technology is enabled.
     *
     * @return a boolean value which is true is enabled. Otherwise, false.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Method allowing to notify when the discovery of a wrapper is completed.
     *
     * @return true if the discovery is completed. Otherwise, false.
     */
    boolean isDiscoveryCompleted() {
        return discoveryCompleted;
    }

    /**
     * Method allowing to reset the discovery flag.
     */
    void resetDiscoveryFlag() {
        this.discoveryCompleted = false;
    }

    /**
     * Method allowing set the discovery listener for both technologies.
     *
     * @param listenerBothDiscovery a ListenerBothDiscovery object which contains callback functions.
     */
    void setDiscoveryListener(DataLinkManager.ListenerBothDiscovery listenerBothDiscovery) {
        this.listenerBothDiscovery = listenerBothDiscovery;
    }

    /**
     * Method allowing to return the type of the device (Bluetooth/Wi-Fi/both).
     *
     * @return a String which represents the type address of the device.
     */
    int getType() {
        return type;
    }

    /**
     * Method allowing to return the MAC address of the device.
     *
     * @return a String which represents the MAC address of the device.
     */
    String getMac() {
        return ownMac;
    }

    /**
     * Method allowing to check if a message is already received for the connection flooding option.
     *
     * @param id an integer value which represents the identifier of the message.
     * @return a boolean value which is true if the message is received for the first time.
     * Otherwise, false.
     */
    boolean checkFloodEvent(String id) {

        if (!setFloodEvents.contains(id)) {
            setFloodEvents.add(id);

            return true;
        }

        return false;
    }

    /**
     * Method allowing to update the listenerApp object.
     *
     * @param listenerApp a ListenerApp object which contains callback functions.
     */
    void updateListener(ListenerApp listenerApp) {
        this.listenerApp = listenerApp;
    }
}
