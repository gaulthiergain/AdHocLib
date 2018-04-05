package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
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

    private final ListenerApp listenerApp;
    private final AbstractWrapper wrappers[];
    private final HashMap<String, AdHocDevice> mapAddressDevice;
    private Config config;

    public DataLinkManager(boolean verbose, Context context, Config config,
                           ListenerApp listenerApp, final ListenerDataLink listenerDataLink)
            throws IOException {

        this.config = config;
        this.listenerApp = listenerApp;
        this.mapAddressDevice = new HashMap<>();

        this.wrappers = new AbstractWrapper[2];

        if (config.isReliableTransportWifi()) {
            // TCP connection
            this.wrappers[WIFI] = new WrapperWifi(verbose, context, config, mapAddressDevice,
                    listenerApp, listenerDataLink);
        } else {
            // UDP stream
            this.wrappers[WIFI] = new WrapperWifiUdp(verbose, context, config, mapAddressDevice,
                    listenerApp, listenerDataLink);
        }


        this.wrappers[BLUETOOTH] = new WrapperBluetooth(verbose, context, config, mapAddressDevice,
                listenerApp, listenerDataLink);

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

        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                if (wrapper.isDirectNeighbors(address)) {
                    return true;
                }
            }
        }

        return false;
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

    public void enableAll(final ListenerAdapter listenerAdapter) {
        for (AbstractWrapper wrapper : wrappers) {
            if (!wrapper.isEnabled()) {
                wrapper.enable(0, new ListenerAdapter() {
                    @Override
                    public void onEnableBluetooth(boolean success) {
                        processListenerAdapter(BLUETOOTH, success, listenerAdapter);
                    }

                    @Override
                    public void onEnableWifi(boolean success) {
                        processListenerAdapter(WIFI, success, listenerAdapter);
                    }
                });
            }
        }
    }

    public void enableWifi(final ListenerAdapter listenerAdapter) {
        if (!wrappers[WIFI].isEnabled()) {
            wrappers[WIFI].enable(0, new ListenerAdapter() {
                @Override
                public void onEnableBluetooth(boolean success) {
                    //do nothing in this case
                    listenerAdapter.onEnableBluetooth(success);
                }

                @Override
                public void onEnableWifi(boolean success) {
                    processListenerAdapter(WIFI, success, listenerAdapter);
                }
            });
        }
    }

    public void enableBluetooth(int duration, final ListenerAdapter listenerAdapter) {
        if (!wrappers[BLUETOOTH].isEnabled()) {
            wrappers[BLUETOOTH].enable(duration, new ListenerAdapter() {
                @Override
                public void onEnableBluetooth(boolean success) {
                    processListenerAdapter(BLUETOOTH, success, listenerAdapter);
                }

                @Override
                public void onEnableWifi(boolean success) {
                    //do nothing in this case
                    listenerAdapter.onEnableWifi(success);
                }
            });

        }
    }

    public void disableAll() throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.stopListening();
                wrapper.disable();
            }
        }
    }

    public void disableWifi() throws IOException {
        if (wrappers[WIFI].isEnabled()) {
            wrappers[WIFI].stopListening();
            wrappers[WIFI].disable();
        }
    }

    public void disableBluetooth() throws IOException {
        if (wrappers[BLUETOOTH].isEnabled()) {
            wrappers[BLUETOOTH].stopListening();
            wrappers[BLUETOOTH].disable();
        }
    }

    public boolean isWifiEnable() {
        return wrappers[WIFI].isEnabled();
    }

    public boolean isBluetoothEnable() {
        return wrappers[BLUETOOTH].isEnabled();
    }

    public boolean updateBluetoothName(String newName) throws DeviceException {
        if (wrappers[BLUETOOTH].isEnabled()) {
            return wrappers[BLUETOOTH].updateDeviceName(newName);
        } else {
            throw new DeviceException("Bluetooth adapter is not enabled");
        }
    }

    public boolean updateWifiName(String newName) throws DeviceException {
        if (wrappers[WIFI].isEnabled()) {
            return wrappers[WIFI].updateDeviceName(newName);
        } else {
            throw new DeviceException("WiFi adapter is not enabled");
        }
    }

    public void resetBluetoothName() throws DeviceException {
        if (wrappers[BLUETOOTH].isEnabled()) {
            wrappers[BLUETOOTH].resetDeviceName();
        } else {
            throw new DeviceException("Bluetooth adapter is not enabled");
        }
    }

    public void resetWifiName() throws DeviceException {
        if (wrappers[WIFI].isEnabled()) {
            wrappers[WIFI].resetDeviceName();
        } else {
            throw new DeviceException("WiFi adapter is not enabled");
        }
    }

    public void disconnectAll() throws IOException, NoConnectionException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.disconnectAll();
            }
        }
    }

    public void disconnect(String remoteDest) throws IOException, NoConnectionException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.disconnect(remoteDest);
            }
        }
    }

    public interface ListenerDiscovery {
        void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice);
    }

    private void processListenerAdapter(int type, boolean success, final ListenerAdapter listenerAdapter) {
        if (success) {
            try {
                wrappers[type].init(config);
                wrappers[type].unregisterAdapter();
                if (type == BLUETOOTH) {
                    listenerAdapter.onEnableBluetooth(true);
                } else {
                    listenerAdapter.onEnableWifi(true);
                }
            } catch (IOException e) {
                if (type == BLUETOOTH) {
                    listenerAdapter.onEnableBluetooth(false);
                } else {
                    listenerAdapter.onEnableWifi(false);
                }
            }
        } else {
            if (type == BLUETOOTH) {
                listenerAdapter.onEnableBluetooth(false);
            } else {
                listenerAdapter.onEnableWifi(false);
            }
        }
    }

}
