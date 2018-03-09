package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothDisabledException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers.WrapperBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataLinkBtManager implements IDataLink {

    private final boolean v;
    private final String TAG = "[AdHoc][DataLinkBt]";
    private final ActiveConnections activeConnections;
    private final WrapperBluetooth wrapperBt;

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
    public DataLinkBtManager(boolean verbose, Context context, boolean secure, short nbThreads,
                             short duration, ListenerAodv listenerAodv, ListenerDataLinkAodv listenerDataLinkAodv)
            throws IOException, DeviceException, BluetoothDisabledException, BluetoothBadDuration {

        this.v = verbose;
        this.activeConnections = new ActiveConnections();

        this.wrapperBt =
                new WrapperBluetooth(v, context, secure, nbThreads, duration, activeConnections, listenerAodv, listenerDataLinkAodv);

    }


    @Override
    public void connect(HashMap<String, DiscoveredDevice> hashMap) {
        wrapperBt.connect();
    }

    @Override
    public void stopListening() throws IOException {
        wrapperBt.stopListening();
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
        wrapperBt.discovery();
    }

    @Override
    public void getPaired() {
        wrapperBt.getPaired();
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
