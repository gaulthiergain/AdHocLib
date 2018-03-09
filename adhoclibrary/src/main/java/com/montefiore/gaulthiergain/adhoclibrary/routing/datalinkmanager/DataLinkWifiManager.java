package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers.WrapperWifi;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataLinkWifiManager implements IDataLink {


    public static final String ID_APP = "#e091#";
    private static final String TAG = "[AdHoc][DataLinkWifi]";

    private final boolean v;
    private final WrapperWifi wrapperWifi;
    private final ActiveConnections activeConnections;


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
    public DataLinkWifiManager(boolean verbose, Context context, boolean enable, short nbThreads, int serverPort,
                               ListenerAodv listenerAodv, final ListenerDataLinkAodv listenerDataLinkAodv)
            throws DeviceException, IOException {
        this.v = verbose;
        this.activeConnections = new ActiveConnections();

        this.wrapperWifi =
                new WrapperWifi(v, context, enable, nbThreads, serverPort, activeConnections, listenerAodv, listenerDataLinkAodv);

    }


    @Override
    public void connect(HashMap<String, DiscoveredDevice> hashMap) {
        wrapperWifi.connect();
    }

    @Override
    public void stopListening() throws IOException {
        wrapperWifi.stopListening();
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
    public void broadcast(MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : activeConnections.getActivesConnections().entrySet()) {
            entry.getValue().sendObjectStream(message);
            if (v)
                Log.d(TAG, "Broadcast Message to " + entry.getKey());
        }
    }

    @Override
    public void discovery() {
        wrapperWifi.discovery();
    }

    @Override
    public void getPaired() {

    }
}
