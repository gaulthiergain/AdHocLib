package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothDisabledException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers.WrapperBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers.WrapperHybridWifi;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers.WrapperWifi;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.Map;

public class DataLinkHybridManager implements IDataLink {

    public static final String ID_APP = "#e091#";
    private static final String TAG = "[AdHoc][DataLinkHybrid]";

    private final boolean v;
    private final WrapperBluetooth wrapperBluetooth;
    private final WrapperHybridWifi wrapperWifi;
    private final ActiveConnections activeConnections;


    public DataLinkHybridManager(boolean verbose, Context context, short nbThreadsWifi,
                                 int serverPort, boolean secure, short nbThreadsBt, short duration,
                                 ListenerAodv listenerAodv, final ListenerDataLinkAodv listenerDataLinkAodv)
            throws DeviceException, IOException, BluetoothDisabledException, BluetoothBadDuration {

        this.v = verbose;
        this.activeConnections = new ActiveConnections();

        //todo update this
        String loopbackAddress = BluetoothUtil.getCurrentMac(context).replace(":", "").toLowerCase();
        listenerDataLinkAodv.getDeviceAddress(loopbackAddress);

        this.wrapperWifi =
                new WrapperHybridWifi(v, context, nbThreadsWifi, serverPort, loopbackAddress,
                        activeConnections, listenerAodv, listenerDataLinkAodv);
        this.wrapperBluetooth =
                new WrapperBluetooth(v, context, secure, nbThreadsBt, duration, loopbackAddress,
                        activeConnections, listenerAodv, listenerDataLinkAodv);

    }

    @Override
    public void connect() {
        if (wrapperWifi.isWifiEnabled()) {
            wrapperWifi.connect();
        } else {
            wrapperBluetooth.connect();
        }
    }

    @Override
    public void stopListening() throws IOException {
        if (wrapperWifi.isWifiEnabled()) {
            wrapperWifi.stopListening();
        }
        wrapperBluetooth.stopListening();
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

        new Thread(new Runnable() {
            @Override
            public void run() {
                wrapperBluetooth.discovery();
            }
        }).start();

        if (wrapperWifi.isWifiEnabled()) {
            wrapperWifi.discovery();
        }
    }


    @Override
    public void getPaired() {
        wrapperBluetooth.getPaired();
    }
}
