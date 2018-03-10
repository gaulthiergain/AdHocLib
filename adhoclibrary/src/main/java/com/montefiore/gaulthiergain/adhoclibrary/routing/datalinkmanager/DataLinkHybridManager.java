package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers.WrapperHybridBt;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers.WrapperHybridWifi;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataLinkHybridManager {

    private static final String TAG = "[AdHoc][DataLinkHybrid]";

    private boolean wifiEnabled;
    private boolean bluetoothEnabled;

    private final boolean v;
    private WrapperHybridBt wrapperBluetooth;
    private WrapperHybridWifi wrapperWifi;
    private final ActiveConnections activeConnections;


    private final HashMap<String, DiscoveredDevice> mapAddressDevice;
    private ListenerAodv listenerAodv;

    public DataLinkHybridManager(boolean verbose, Context context, short nbThreadsWifi,
                                 int serverPort, boolean secure, short nbThreadsBt,
                                 ListenerAodv listenerAodv, final ListenerDataLinkAodv listenerDataLinkAodv)
            throws IOException, DeviceException {

        this.v = verbose;
        this.listenerAodv = listenerAodv;
        this.activeConnections = new ActiveConnections();
        this.mapAddressDevice = new HashMap<>();

        //todo update this with random address
        String label = BluetoothUtil.getCurrentMac(context).replace(":", "").toLowerCase();
        listenerDataLinkAodv.getDeviceAddress(label);
        listenerDataLinkAodv.getDeviceName(label);

        try {
            wrapperWifi =
                    new WrapperHybridWifi(v, context, nbThreadsWifi, serverPort, label,
                            activeConnections, mapAddressDevice, listenerAodv, listenerDataLinkAodv);
            wifiEnabled = true;
        } catch (DeviceException e) {
            wifiEnabled = false;
        }

        try {
            wrapperBluetooth =
                    new WrapperHybridBt(v, context, secure, nbThreadsBt, label,
                            activeConnections, mapAddressDevice, listenerAodv, listenerDataLinkAodv);
            //wrapperBluetooth.updateName(label);
            bluetoothEnabled = true;
        } catch (DeviceException e) {
            bluetoothEnabled = false;
        }

        if (!bluetoothEnabled && !wifiEnabled) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }
    }

    public void discovery() throws DeviceException {

        if (!bluetoothEnabled && !wifiEnabled) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        if (bluetoothEnabled && wifiEnabled) {
            wifiBtDiscovery();
        } else {
            if (bluetoothEnabled) {
                wrapperBluetooth.discovery();
                wrapperBluetooth.setDiscoveryListener(new ListenerDiscovery() {
                    @Override
                    public void onDiscoveryCompleted(HashMap<String, DiscoveredDevice> mapAddressDevice) {
                        listenerAodv.onDiscoveryCompleted(mapAddressDevice);
                    }
                });

            }

            if (wifiEnabled) {
                wrapperWifi.discovery();
                wrapperWifi.setDiscoveryListener(new ListenerDiscovery() {
                    @Override
                    public void onDiscoveryCompleted(HashMap<String, DiscoveredDevice> mapAddressDevice) {
                        listenerAodv.onDiscoveryCompleted(mapAddressDevice);
                    }
                });
            }
        }
    }

    private void wifiBtDiscovery() {

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


    public void connect(HashMap<String, DiscoveredDevice> hashMap) throws DeviceException {

        if (!bluetoothEnabled && !wifiEnabled) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        for (Map.Entry<String, DiscoveredDevice> entry : hashMap.entrySet()) {
            if (entry.getValue().getType() == DiscoveredDevice.BLUETOOTH) {
                wrapperBluetooth.connect(entry.getValue());
            } else {
                wrapperWifi.connect(entry.getValue());
            }
        }
    }

    public void stopListening() throws IOException, DeviceException {

        if (!bluetoothEnabled && !wifiEnabled) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        if (wifiEnabled) {
            wrapperWifi.stopListening();
            wrapperWifi.unregisterConnection();
        }

        if (bluetoothEnabled) {
            wrapperBluetooth.stopListening();
        }
    }

    public void sendMessage(MessageAdHoc message, String address) throws IOException {
        NetworkObject networkObject = activeConnections.getActivesConnections().get(address);
        networkObject.sendObjectStream(message);
        if (v) Log.d(TAG, "Send directly to " + address);
    }

    public boolean isDirectNeighbors(String address) {
        return activeConnections.getActivesConnections().containsKey(address);
    }

    public void broadcastExcept(String originateAddr, MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : activeConnections.getActivesConnections().entrySet()) {
            if (!entry.getKey().equals(originateAddr)) {
                entry.getValue().sendObjectStream(message);
                if (v)
                    Log.d(TAG, "Broadcast Message to " + entry.getKey());
            }
        }
    }

    public void broadcast(MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : activeConnections.getActivesConnections().entrySet()) {
            entry.getValue().sendObjectStream(message);
            if (v)
                Log.d(TAG, "Broadcast Message to " + entry.getKey());
        }
    }

    public void getPaired() {
        wrapperBluetooth.getPaired();
    }

    public interface ListenerDiscovery {
        void onDiscoveryCompleted(HashMap<String, DiscoveredDevice> mapAddressDevice);
    }
}
