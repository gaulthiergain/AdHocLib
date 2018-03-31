package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.AbstractWrapper;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.WrapperBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.WrapperWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.WrapperWifiUdp;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataLinkManager {

    private static final int POOLING_DISCOVERY = 1000;

    public static final byte WIFI = 0;
    public static final byte BLUETOOTH = 1;

    private final Neighbors neighbors;
    private final ListenerApp listenerApp;
    private final AbstractWrapper wrappers[];
    private final HashMap<String, AdHocDevice> mapAddressDevice;

    public DataLinkManager(boolean verbose, Context context, Config config,
                           ListenerApp listenerApp, final ListenerDataLink listenerDataLink)
            throws IOException {

        this.listenerApp = listenerApp;
        this.neighbors = new Neighbors();
        this.mapAddressDevice = new HashMap<>();

        this.wrappers = new AbstractWrapper[2];

        if (config.isReliableTransportWifi()) {
            // TCP connection
            this.wrappers[WIFI] = new WrapperWifi(verbose, context, config, neighbors,
                    mapAddressDevice, listenerApp, listenerDataLink);
        } else {
            // UDP stream
            this.wrappers[WIFI] = new WrapperWifiUdp(verbose, context, config, neighbors,
                    mapAddressDevice, listenerApp, listenerDataLink);
        }


        this.wrappers[BLUETOOTH] = new WrapperBluetooth(verbose, context, config, neighbors,
                mapAddressDevice, listenerApp, listenerDataLink);

        // Check if data link communications are enabled (0 : all is disabled)
        checkState();
    }

    private int checkState() {
        int enabled = 0;
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                enabled++;
            }
        }
        return enabled;
    }

    public void discovery() throws DeviceException {

        int enabled = checkState();
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
                        public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice) {
                            listenerApp.onDiscoveryCompleted(mapAddressDevice);
                        }
                    });
                }
            }
        }
    }


    private void bothDiscovery() {

        @SuppressLint("HandlerLeak") final Handler mHandler = new Handler() {
            // Used handler to avoid updating views in other threads than the main thread
            public void handleMessage(Message msg) {
                listenerApp.onDiscoveryCompleted(mapAddressDevice);
            }
        };

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
                            // Used handler to avoid using runOnUiThread in main app
                            mHandler.obtainMessage(1).sendToTarget();
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

    public void connect(AdHocDevice adHocDevice) throws DeviceException {

        if (checkState() == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        switch (adHocDevice.getType()) {
            case DataLinkManager.WIFI:
                wrappers[WIFI].connect(adHocDevice);
                break;
            case DataLinkManager.BLUETOOTH:
                wrappers[BLUETOOTH].connect(adHocDevice);
                break;
        }
    }

    public void connect(HashMap<String, AdHocDevice> hashMap) throws DeviceException {

        if (checkState() == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        for (Map.Entry<String, AdHocDevice> entry : hashMap.entrySet()) {
            switch (entry.getValue().getType()) {
                case DataLinkManager.WIFI:
                    wrappers[WIFI].connect(entry.getValue());
                    break;
                case DataLinkManager.BLUETOOTH:
                    wrappers[BLUETOOTH].connect(entry.getValue());
                    break;
            }
        }
    }

    public void stopListening() throws IOException {

        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.stopListening();
                wrapper.unregisterConnection();
            }
        }
    }

    public void sendMessage(MessageAdHoc message, String address) throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.sendMessage(message, address);
            }
        }
    }

    public boolean isDirectNeighbors(String address) {
        return neighbors.getNeighbors().containsKey(address);
    }

    public void broadcastExcept(MessageAdHoc message, String excludedAddress) throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.broadcastExcept(message, excludedAddress);
            }
        }
    }

    public void broadcast(MessageAdHoc message) throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.broadcast(message);
            }
        }
    }

    public HashMap<String, AdHocDevice> getPaired() {
        if (wrappers[BLUETOOTH].isEnabled()) {
            return wrappers[BLUETOOTH].getPaired();
        }
        return null;
    }

    public void disconnect() {
        //TODO implement
    }

    public void enableAll(ListenerAdapter listenerAdapter) {
        for (AbstractWrapper wrapper : wrappers) {
            if (!wrapper.isEnabled()) {
                wrapper.enable(0, listenerAdapter);
            }
        }
    }

    public void enableWifi(ListenerAdapter listenerAdapter) {
        if (!wrappers[WIFI].isEnabled()) {
            wrappers[WIFI].enable(0, listenerAdapter);
        }
    }

    public void enableBluetooth(int duration, ListenerAdapter listenerAdapter) {
        if (!wrappers[BLUETOOTH].isEnabled()) {
            wrappers[BLUETOOTH].enable(duration, listenerAdapter);
        }
    }

    public void disableAll() {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.disable();
            }
        }
    }

    public void disableWifi() {
        if (wrappers[WIFI].isEnabled()) {
            wrappers[WIFI].disable();
        }
    }

    public void disableBluetooth() throws IOException {
        if (wrappers[BLUETOOTH].isEnabled()) {
            wrappers[BLUETOOTH].stopListening();
            wrappers[BLUETOOTH].disable();
        }
    }

    public void unregisterAdapter() {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.unregisterAdapter();
            }
        }
    }

    public boolean isWifiEnable() {
        return wrappers[WIFI].isEnabled();
    }

    public boolean isBluetoothEnable() {
        return wrappers[BLUETOOTH].isEnabled();
    }

    public interface ListenerDiscovery {
        void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice);
    }
}
