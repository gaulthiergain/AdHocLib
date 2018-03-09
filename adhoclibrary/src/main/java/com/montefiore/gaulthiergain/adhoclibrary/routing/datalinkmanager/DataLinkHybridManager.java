package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothDisabledException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers.WrapperHybridBt;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers.WrapperHybridWifi;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataLinkHybridManager implements IDataLink {

    private static final String TAG = "[AdHoc][DataLinkHybrid]";

    private final boolean v;
    private final WrapperHybridBt wrapperBluetooth;
    private final WrapperHybridWifi wrapperWifi;
    private final ActiveConnections activeConnections;


    private final HashMap<String, DiscoveredDevice> mapAddressDevice;
    private ListenerAodv listenerAodv;

    public DataLinkHybridManager(boolean verbose, Context context, short nbThreadsWifi,
                                 int serverPort, boolean secure, short nbThreadsBt, short duration,
                                 ListenerAodv listenerAodv, final ListenerDataLinkAodv listenerDataLinkAodv)
            throws DeviceException, IOException, BluetoothDisabledException, BluetoothBadDuration {

        this.v = verbose;
        this.listenerAodv = listenerAodv;
        this.activeConnections = new ActiveConnections();
        this.mapAddressDevice = new HashMap<>();

        //todo update this with random address
        String label = BluetoothUtil.getCurrentMac(context).replace(":", "").toLowerCase();
        listenerDataLinkAodv.getDeviceAddress(label);
        listenerDataLinkAodv.getDeviceName(label);

        wrapperWifi =
                new WrapperHybridWifi(v, context, nbThreadsWifi, serverPort, label,
                        activeConnections, mapAddressDevice, listenerAodv, listenerDataLinkAodv);

        if (wrapperWifi.isWifiEnabled()) {
            /*wrapperWifi.setListenerConnection(new WrapperHybridWifi.ListenerConnection() {
                @Override
                public void onConnect() {
                    wrapperBluetooth.connect();
                }
            });*/ //todo remove
        }

        wrapperBluetooth =
                new WrapperHybridBt(v, context, secure, nbThreadsBt, duration, label,
                        activeConnections, mapAddressDevice, listenerAodv, listenerDataLinkAodv);

        //wrapperBluetooth.updateName(label);
    }

    @Override
    public void discovery() {
        wrapperBluetooth.discovery();
        wrapperWifi.discovery();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(1000);
                        if (wrapperBluetooth.isFinishDiscovery() && wrapperWifi.isFinishDiscovery()) {
                            listenerAodv.onDiscoveryCompleted(mapAddressDevice);
                            break;
                        }
                    }
                    wrapperWifi.setFinishDiscovery(false);
                    wrapperBluetooth.setFinishDiscovery(false);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void connect(HashMap<String, DiscoveredDevice> hashMap) {
        for (Map.Entry<String, DiscoveredDevice> entry : hashMap.entrySet()) {
            if (entry.getValue().getType() == DiscoveredDevice.BLUETOOTH) {
                wrapperBluetooth.connect(entry.getValue());
            } else {
                wrapperWifi.connect(entry.getValue());
            }
        }
    }

    @Override
    public void stopListening() throws IOException {
        if (wrapperWifi.isWifiEnabled()) {
            wrapperWifi.stopListening();
            wrapperWifi.unregisterConnection();
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
    public void getPaired() {
        wrapperBluetooth.getPaired();
    }
}
