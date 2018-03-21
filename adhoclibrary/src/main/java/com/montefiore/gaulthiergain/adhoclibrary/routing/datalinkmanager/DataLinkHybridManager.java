package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.AbstractWrapper;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.WrapperHybridBt;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.WrapperHybridWifi;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataLinkHybridManager {

    private static final String TAG = "[AdHoc][DataLink]";

    private final boolean v;
    private final AbstractWrapper wrappers[];
    private final ActiveConnections activeConnections;
    private final HashMap<String, DiscoveredDevice> mapAddressDevice;

    private short enabled;
    private ListenerAodv listenerAodv;

    public DataLinkHybridManager(boolean verbose, Context context, short nbThreadsWifi,
                                 int serverPort, boolean secure, short nbThreadsBt,
                                 ListenerAodv listenerAodv, final ListenerDataLinkAodv listenerDataLinkAodv)
            throws IOException, DeviceException {

        this.v = verbose;
        this.enabled = 0;
        this.listenerAodv = listenerAodv;
        this.activeConnections = new ActiveConnections();
        this.mapAddressDevice = new HashMap<>();

        //todo update this with random address
        String label = BluetoothUtil.getCurrentMac(context).replace(":", "").toLowerCase();
        listenerDataLinkAodv.getDeviceAddress(label);
        listenerDataLinkAodv.getDeviceName(label);

        this.wrappers = new AbstractWrapper[2];
        this.wrappers[0] = new WrapperHybridWifi(v, context, nbThreadsWifi, serverPort, label,
                activeConnections, mapAddressDevice, listenerAodv, listenerDataLinkAodv);
        this.wrappers[1] = new WrapperHybridBt(v, context, secure, nbThreadsBt, label,
                activeConnections, mapAddressDevice, listenerAodv, listenerDataLinkAodv);

        // Check if data link communications are enabled (0 : all is disabled)
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                enabled++;
            }
        }

        if (enabled == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }
    }

    public void discovery() throws DeviceException {

        if (enabled == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        if (enabled == wrappers.length) {
            // Both data link communications are enabled
            bothDiscovery();
        } else {
            // Discovery one by one depending their status
            for (AbstractWrapper wrapper : wrappers) {
                if (wrapper.isEnabled()) {
                    wrapper.discovery();
                    wrapper.setDiscoveryListener(new ListenerDiscovery() {
                        @Override
                        public void onDiscoveryCompleted(HashMap<String, DiscoveredDevice> mapAddressDevice) {
                            listenerAodv.onDiscoveryCompleted(mapAddressDevice);
                        }
                    });
                }
            }
        }
    }

    private void bothDiscovery() {

        for (AbstractWrapper wrapper : wrappers) {
            wrapper.discovery();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                short finished = 0;
                try {
                    // Use pooling to check if the discovery is completed
                    while (true) {
                        Thread.sleep(1000);

                        for (AbstractWrapper wrapper : wrappers) {
                            if (wrapper.isDiscoveryCompleted()) {
                                finished++;
                            }
                        }

                        if (finished == wrappers.length) {
                            listenerAodv.onDiscoveryCompleted(mapAddressDevice);
                            break;
                        }
                    }

                    // Reset flag to perform a new discovery
                    for (AbstractWrapper wrapper : wrappers) {
                        wrapper.resetDiscoveryFlag();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public void connect(HashMap<String, DiscoveredDevice> hashMap) throws DeviceException {

        if (enabled == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        for (Map.Entry<String, DiscoveredDevice> entry : hashMap.entrySet()) {
            switch (entry.getValue().getType()) {
                case DiscoveredDevice.WIFI:
                    wrappers[0].connect(entry.getValue());
                    break;
                case DiscoveredDevice.BLUETOOTH:
                    wrappers[1].connect(entry.getValue());
                    break;
            }
        }
    }

    public void stopListening() throws IOException, DeviceException {

        if (enabled == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.stopListening();
                wrapper.unregisterConnection();
            }
        }
    }

    public void sendMessage(MessageAdHoc message, String address) throws IOException {
        NetworkManager networkManager = activeConnections.getActivesConnections().get(address);
        networkManager.sendMessage(message);
        if (v) Log.d(TAG, "Send directly to " + address);
    }

    public boolean isDirectNeighbors(String address) {
        return activeConnections.getActivesConnections().containsKey(address);
    }

    public void broadcastExcept(String originateAddr, MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkManager> entry : activeConnections.getActivesConnections().entrySet()) {
            if (!entry.getKey().equals(originateAddr)) {
                entry.getValue().sendMessage(message);
                if (v)
                    Log.d(TAG, "Broadcast Message to " + entry.getKey());
            }
        }
    }

    public void broadcast(MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkManager> entry : activeConnections.getActivesConnections().entrySet()) {
            entry.getValue().sendMessage(message);
            if (v)
                Log.d(TAG, "Broadcast Message to " + entry.getKey());
        }
    }

    public void getPaired() {
        wrappers[1].getPaired();
    }

    public void disconnect() {
        //TODO implement
    }

    public interface ListenerDiscovery {
        void onDiscoveryCompleted(HashMap<String, DiscoveredDevice> mapAddressDevice);
    }
}
