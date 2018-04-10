package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.AbstractWrapper;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.WrapperBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.WrapperWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.WrapperWifiUdp;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.DeviceAlreadyConnectedException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DataLinkManager {

    private static final int POOLING_DISCOVERY = 1000;

    private final AbstractWrapper wrappers[];
    private final HashMap<String, AdHocDevice> mapAddressDevice;

    private Config config;
    private ListenerApp listenerApp;

    public DataLinkManager(boolean verbose, Context context, Config config,
                           ListenerApp listenerApp, final ListenerDataLink listenerDataLink)
            throws IOException {

        this.config = config;
        this.listenerApp = listenerApp;
        this.mapAddressDevice = new HashMap<>();
        this.wrappers = new AbstractWrapper[2];

        if (config.isReliableTransportWifi()) {
            // TCP connection
            this.wrappers[Service.WIFI] = new WrapperWifi(verbose, context, config, mapAddressDevice,
                    listenerApp, listenerDataLink);
        } else {
            // UDP stream
            this.wrappers[Service.WIFI] = new WrapperWifiUdp(verbose, context, config, mapAddressDevice,
                    listenerApp, listenerDataLink);
        }


        this.wrappers[Service.BLUETOOTH] = new WrapperBluetooth(verbose, context, config, mapAddressDevice,
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

        @SuppressLint("HandlerLeak") final Handler mHandler = new Handler(Looper.getMainLooper()) {
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

    public void connect(AdHocDevice adHocDevice) throws DeviceException, DeviceAlreadyConnectedException {

        if (checkState() == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        switch (adHocDevice.getType()) {
            case Service.WIFI:
                wrappers[Service.WIFI].connect(adHocDevice);
                break;
            case Service.BLUETOOTH:
                wrappers[Service.BLUETOOTH].connect(adHocDevice);
                break;
        }
    }

    public void connect(HashMap<String, AdHocDevice> hashMap) throws DeviceException, DeviceAlreadyConnectedException {

        if (checkState() == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        for (Map.Entry<String, AdHocDevice> entry : hashMap.entrySet()) {
            switch (entry.getValue().getType()) {
                case Service.WIFI:
                    wrappers[Service.WIFI].connect(entry.getValue());
                    break;
                case Service.BLUETOOTH:
                    wrappers[Service.BLUETOOTH].connect(entry.getValue());
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

    public void broadcast(MessageAdHoc message) throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.broadcast(message);
            }
        }
    }

    public void broadcast(Object object) throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                Header header = new Header(AbstractWrapper.BROADCAST, wrapper.getMac(),
                        config.getLabel(), wrapper.getAdapterName(), wrapper.getType());
                wrapper.broadcast(new MessageAdHoc(header, object));
            }
        }
    }

    public void broadcastExcept(MessageAdHoc message, String excludedAddress) throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.broadcastExcept(message, excludedAddress);
            }
        }
    }

    public void broadcastExcept(Object object, String excludedAddress) throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                Header header = new Header(AbstractWrapper.BROADCAST, wrapper.getMac(),
                        config.getLabel(), wrapper.getAdapterName(), wrapper.getType());
                wrapper.broadcastExcept(new MessageAdHoc(header, object), excludedAddress);
            }
        }
    }

    public HashMap<String, AdHocDevice> getPaired() {
        if (wrappers[Service.BLUETOOTH].isEnabled()) {
            return wrappers[Service.BLUETOOTH].getPaired();
        }
        return null;
    }

    public void enableAll(final ListenerAdapter listenerAdapter) throws BluetoothBadDuration {
        for (AbstractWrapper wrapper : wrappers) {
            enable(0, wrapper.getType(), listenerAdapter);
        }
    }

    public void enable(int duration, final int type, final ListenerAdapter listenerAdapter) throws BluetoothBadDuration {

        if (!wrappers[type].isEnabled()) {
            wrappers[type].enable(duration, new ListenerAdapter() {
                @Override
                public void onEnableBluetooth(boolean success) {
                    processListenerAdapter(type, success, listenerAdapter);
                }

                @Override
                public void onEnableWifi(boolean success) {
                    processListenerAdapter(type, success, listenerAdapter);
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

    public void disable(int type) throws IOException {
        if (wrappers[type].isEnabled()) {
            wrappers[type].stopListening();
            wrappers[type].disable();
        }
    }

    public boolean isEnable(int type) {
        return wrappers[type].isEnabled();
    }

    public boolean updateAdapterName(int type, String newName) throws DeviceException {
        if (wrappers[type].isEnabled()) {
            return wrappers[type].updateDeviceName(newName);
        } else {
            throw new DeviceException(getTypeString(type) + " adapter is not enabled");
        }
    }

    public void resetAdapterName(int type) throws DeviceException {
        if (wrappers[type].isEnabled()) {
            wrappers[type].resetDeviceName();
        } else {
            throw new DeviceException(getTypeString(type) + " adapter is not enabled");
        }
    }

    public void disconnectAll() throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.disconnectAll();
            }
        }
    }

    public void disconnect(String remoteDest) throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.disconnect(remoteDest);
            }
        }
    }

    public ArrayList<String> getActifAdapterNames() {
        ArrayList<String> adapterNames = new ArrayList<>();
        for (AbstractWrapper wrapper : wrappers) {
            String name = getAdapterName(wrapper.getType());
            if (name != null) {
                adapterNames.add(name);
            }
        }

        return adapterNames;
    }

    public String getAdapterName(int type) {
        if (wrappers[type].isEnabled()) {
            return wrappers[type].getAdapterName();
        }
        return null;
    }

    public void updateListener(ListenerApp listenerApp) {
        this.listenerApp = listenerApp;
        for (AbstractWrapper wrapper : wrappers) {
            wrapper.updateListener(listenerApp);
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
                if (type == Service.BLUETOOTH) {
                    listenerAdapter.onEnableBluetooth(true);
                } else {
                    listenerAdapter.onEnableWifi(true);
                }
            } catch (IOException e) {
                if (type == Service.BLUETOOTH) {
                    listenerAdapter.onEnableBluetooth(false);
                } else {
                    listenerAdapter.onEnableWifi(false);
                }
            }
        } else {
            if (type == Service.BLUETOOTH) {
                listenerAdapter.onEnableBluetooth(false);
            } else {
                listenerAdapter.onEnableWifi(false);
            }
        }
    }

    private String getTypeString(int type) {
        switch (type) {
            case Service.BLUETOOTH:
                return "Bluetooth";
            case Service.WIFI:
                return "WiFi";
            default:
                return "Unknown";
        }
    }

}
