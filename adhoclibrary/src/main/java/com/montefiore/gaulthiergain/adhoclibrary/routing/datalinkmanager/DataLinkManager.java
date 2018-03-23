package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.applayer.Config;
import com.montefiore.gaulthiergain.adhoclibrary.applayer.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.AbstractWrapper;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.WrapperBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.WrapperWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.WrapperWifiUdp;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataLinkManager {

    private static final String TAG = "[AdHoc][DataLink]";
    private static final int POOLING_DISCOVERY = 1000;

    private final boolean v;
    private final ListenerApp listenerApp;
    private final AbstractWrapper wrappers[];
    private final ActiveConnections activeConnections;
    private final HashMap<String, DiscoveredDevice> mapAddressDevice;

    private short enabled;


    //todo update
    private boolean udp;

    public DataLinkManager(boolean verbose, Context context, Config config,
                           ListenerApp listenerApp, final ListenerDataLink listenerDataLink)
            throws IOException, DeviceException {

        this.v = verbose;
        this.enabled = 0;
        this.listenerApp = listenerApp;
        this.activeConnections = new ActiveConnections();
        this.mapAddressDevice = new HashMap<>();

        String label = config.getLabel();
        this.wrappers = new AbstractWrapper[2];

        if (config.isReliableTransportWifi()) {
            // TCP connection
            udp = false;
            this.wrappers[0] = new WrapperWifi(v, context, config.getNbThreadWifi(), config.getServerPort(), label,
                    activeConnections, mapAddressDevice, listenerApp, listenerDataLink);
        } else {
            // UDP stream
            udp = true;
            this.wrappers[0] = new WrapperWifiUdp(v, context, config.getNbThreadWifi(), config.getServerPort(), label,
                    activeConnections, mapAddressDevice, listenerApp, listenerDataLink);
        }


        this.wrappers[1] = new WrapperBluetooth(v, context, config.getSecure(), config.getNbThreadBt(), label,
                activeConnections, mapAddressDevice, listenerApp, listenerDataLink);

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
                            listenerApp.onDiscoveryCompleted(mapAddressDevice);
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

                try {
                    // Use pooling to check if the discovery is completed
                    while (true) {
                        Thread.sleep(POOLING_DISCOVERY);

                        boolean finished = true;
                        for (AbstractWrapper wrapper : wrappers) {
                            if (!wrapper.isDiscoveryCompleted()) {
                                finished = false;
                                break;
                            }
                        }

                        if (finished) {
                            listenerApp.onDiscoveryCompleted(mapAddressDevice);
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

        if (!udp) {
            NetworkManager networkManager = activeConnections.getActivesConnections().get(address);
            networkManager.sendMessage(message);
            if (v) Log.d(TAG, "Send directly to " + address);
        } else {
            WrapperWifiUdp wrapperWifiUdp = (WrapperWifiUdp) wrappers[0];
            wrapperWifiUdp.sendMessage(message, address);
        }

    }

    public boolean isDirectNeighbors(String address) {
        if (!udp) {
            return activeConnections.getActivesConnections().containsKey(address);
        } else {
            WrapperWifiUdp wrapperWifiUdp = (WrapperWifiUdp) wrappers[0];
            return wrapperWifiUdp.isDirectNeighbors(address);
        }
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

    public void enable(int duration) {

        for (AbstractWrapper wrapper : wrappers) {
            if (!wrapper.isEnabled()) {
                wrapper.enable(duration);
            }
        }
    }

    public interface ListenerDiscovery {
        void onDiscoveryCompleted(HashMap<String, DiscoveredDevice> mapAddressDevice);
    }
}
