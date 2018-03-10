package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothDisabledException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers.AbstractWrapper;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers.WrapperBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers.WrapperWifi;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataLinkManager implements IDataLink {

    private final boolean v;
    private final String TAG = "[AdHoc][DataLinkBt]";
    private final ActiveConnections activeConnections;
    private final AbstractWrapper wrapper;

    /**
     * Constructor
     *
     * @param verbose              a boolean value to set the debug/verbose mode.
     * @param context              a Context object which gives global information about an
     *                             application environment.
     * @param secure               a boolean value to determine if the connection is secure.
     * @param nbThreads            a short value to determine the number of threads managed by the
     *                             server.
     * @param duration             a short value between 0 and 3600 which represents the time of
     *                             the discovery mode.
     * @param listenerAodv         a ListenerAodv object which serves as callback functions.
     * @param listenerDataLinkAodv a listenerDataLinkAodv object which serves as callback functions.
     * @throws IOException                Signals that an I/O exception of some sort has occurred.
     * @throws DeviceException            Signals that a Bluetooth Device Exception exception has occurred.
     * @throws BluetoothDisabledException Signals that a Bluetooth Disabled exception has occurred.
     * @throws BluetoothBadDuration       Signals that a Bluetooth Bad Duration exception has occurred.
     */
    public DataLinkManager(boolean verbose, Context context, boolean secure, short nbThreads,
                           short duration, ListenerAodv listenerAodv, ListenerDataLinkAodv listenerDataLinkAodv)
            throws IOException, DeviceException, BluetoothDisabledException, BluetoothBadDuration {

        this.v = verbose;
        this.activeConnections = new ActiveConnections();

        this.wrapper =
                new WrapperBluetooth(v, context, secure, nbThreads, duration, activeConnections, listenerAodv, listenerDataLinkAodv);

    }

    /**
     * Constructor
     *
     * @param verbose              a boolean value to set the debug/verbose mode.
     * @param context              a Context object which gives global information about an application
     *                             environment.
     * @param nbThreads            a short value to determine the number of threads managed by the
     *                             server.
     * @param serverPort           an integer value which represents the server list port.
     * @param listenerAodv         a ListenerAodv object which serves as callback functions.
     * @param listenerDataLinkAodv a ListenerDataLinkAodv object which serves as callback functions.
     * @throws DeviceException
     */
    public DataLinkManager(boolean verbose, Context context, boolean enable, short nbThreads, int serverPort,
                           ListenerAodv listenerAodv, final ListenerDataLinkAodv listenerDataLinkAodv)
            throws DeviceException, IOException {
        this.v = verbose;
        this.activeConnections = new ActiveConnections();

        this.wrapper =
                new WrapperWifi(v, context, enable, nbThreads, serverPort, activeConnections, listenerAodv, listenerDataLinkAodv);

    }

    @Override
    public void connect(HashMap<String, DiscoveredDevice> hashMap) {
        for (Map.Entry<String, DiscoveredDevice> entry : hashMap.entrySet()) {
            wrapper.connect(entry.getValue());
        }
    }

    @Override
    public void stopListening() throws IOException {
        wrapper.stopListening();
    }

    @Override
    public void sendMessage(MessageAdHoc message, String address) throws IOException {
        NetworkObject networkObject = activeConnections.getActivesConnections().get(address);
        networkObject.sendObjectStream(message);
        if (v) Log.d(TAG, "Send directly to " + address);
    }

    @Override
    public boolean isDirectNeighbors(String address) {
        return activeConnections.getActivesConnections().containsKey(address);
    }

    @Override
    public void broadcastExcept(String originateAddr, MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : activeConnections.getActivesConnections().entrySet()) {
            if (!entry.getKey().equals(originateAddr)) {
                entry.getValue().sendObjectStream(message);
                if (v)
                    Log.d(TAG, "Broadcast Message to " + entry.getKey());
            }
        }
    }

    @Override
    public void discovery() {
        wrapper.discovery();
    }

    @Override
    public void getPaired() {
        wrapper.getPaired();
    }

    @Override
    public void broadcast(MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : activeConnections.getActivesConnections().entrySet()) {
            entry.getValue().sendObjectStream(message);
            if (v)
                Log.d(TAG, "Broadcast Message to " + entry.getKey());
        }
    }
}
