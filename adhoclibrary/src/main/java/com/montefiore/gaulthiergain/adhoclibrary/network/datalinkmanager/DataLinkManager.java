package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAction;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class DataLinkManager {

    private static final int POOLING_DISCOVERY = 1000;

    private Config config;
    private final AbstractWrapper wrappers[];
    private final HashMap<String, AdHocDevice> mapAddressDevice;

    public DataLinkManager(boolean verbose, Context context, Config config,
                           final ListenerApp listenerApp, final ListenerDataLink listenerDataLink)
            throws IOException {

        this.config = config;
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

    public void discovery(final DiscoveryListener discovery) throws DeviceException {

        int enabled = checkState();
        if (enabled == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        if (enabled == wrappers.length) {
            // Both data link communications are enabled
            bothDiscovery(discovery);
        } else {
            // Discovery one by one depending their status
            for (AbstractWrapper wrapper : wrappers) {
                if (wrapper.isEnabled()) {
                    wrapper.setDiscoveryListener(new ListenerBothDiscovery() {
                        @Override
                        public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice) {
                            discovery.onDiscoveryCompleted(mapAddressDevice);
                        }
                    });
                    wrapper.discovery(discovery);
                }
            }
        }
    }

    public void connect(short attemps, AdHocDevice adHocDevice) throws DeviceException {

        switch (adHocDevice.getType()) {
            case Service.WIFI:
                wrappers[Service.WIFI].connect(attemps, adHocDevice);
                break;
            case Service.BLUETOOTH:
                wrappers[Service.BLUETOOTH].connect(attemps, adHocDevice);
                break;
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

    public boolean broadcast(Object object) throws IOException {
        boolean sent = false;
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                Header header = new Header(AbstractWrapper.BROADCAST, wrapper.getMac(),
                        config.getLabel(), wrapper.getAdapterName(), wrapper.getType());
                if (wrapper.broadcast(new MessageAdHoc(header, object))) {
                    sent = true;
                }
            }
        }
        return sent;
    }

    public void broadcastExcept(MessageAdHoc message, String excludedAddress) throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.broadcastExcept(message, excludedAddress);
            }
        }
    }

    public boolean broadcastExcept(Object object, String excludedAddress) throws IOException {

        boolean sent = false;
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                Header header = new Header(AbstractWrapper.BROADCAST, wrapper.getMac(),
                        config.getLabel(), wrapper.getAdapterName(), wrapper.getType());
                if (wrapper.broadcastExcept(new MessageAdHoc(header, object), excludedAddress)) {
                    sent = true;
                }
            }
        }
        return sent;
    }

    public HashMap<String, AdHocDevice> getPaired() {
        if (wrappers[Service.BLUETOOTH].isEnabled()) {
            return wrappers[Service.BLUETOOTH].getPaired();
        }
        return null;
    }

    public void enableAll(Context context, final ListenerAdapter listenerAdapter) throws BluetoothBadDuration {
        for (AbstractWrapper wrapper : wrappers) {
            enable(0, context, wrapper.getType(), listenerAdapter);
        }
    }

    public void enable(int duration, final Context context, final int type,
                       final ListenerAdapter listenerAdapter) throws BluetoothBadDuration {

        if (!wrappers[type].isEnabled()) {
            wrappers[type].enable(context, duration, new ListenerAdapter() {
                @Override
                public void onEnableBluetooth(boolean success) {
                    processListenerAdapter(type, success, context, listenerAdapter);
                }

                @Override
                public void onEnableWifi(boolean success) {
                    processListenerAdapter(type, success, context, listenerAdapter);
                }
            });
        }
    }

    public void disableAll() throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                disable(wrapper.getType());
            }
        }
    }

    public void disable(int type) throws IOException {
        if (wrappers[type].isEnabled()) {
            wrappers[type].stopListening();
            wrappers[type].disable();
        }
    }

    public boolean isEnabled(int type) {
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

    public HashMap<Integer, String> getActifAdapterNames() {
        @SuppressLint("UseSparseArrays") HashMap<Integer, String> adapterNames = new HashMap<>();
        for (AbstractWrapper wrapper : wrappers) {
            String name = getAdapterName(wrapper.getType());
            if (name != null) {
                adapterNames.put(wrapper.getType(), name);
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

    public void updateContext(Context context) {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.updateContext(context);
            }
        }
    }

    public void unpairDevice(BluetoothAdHocDevice adHocDevice)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, DeviceException {
        WrapperBluetooth wrapperBt = (WrapperBluetooth) wrappers[Service.BLUETOOTH];
        if (wrapperBt.isEnabled()) {
            wrapperBt.unpairDevice(adHocDevice);
        } else {
            throw new DeviceException("Bluetooth is not enabled");
        }
    }

    public void setWifiGroupOwnerValue(int valueGroupOwner) throws GroupOwnerBadValue, DeviceException {
        IWrapperWifi wrapperWifi = (IWrapperWifi) wrappers[Service.WIFI];
        if (wrapperWifi.isEnabled()) {
            wrapperWifi.setGroupOwnerValue(valueGroupOwner);
        } else {
            throw new DeviceException("Wifi is not enabled");
        }
    }

    public void removeGroup(ListenerAction listenerAction) throws DeviceException {
        IWrapperWifi wrapperWifi = (IWrapperWifi) wrappers[Service.WIFI];
        if (wrapperWifi.isEnabled()) {
            wrapperWifi.removeGroup(listenerAction);
        } else {
            throw new DeviceException("Wifi is not enabled");
        }
    }

    public boolean isWifiGroupOwner() throws DeviceException {
        IWrapperWifi wrapperWifi = (IWrapperWifi) wrappers[Service.WIFI];
        if (wrapperWifi.isEnabled()) {
            return wrapperWifi.isWifiGroupOwner();
        } else {
            throw new DeviceException("Wifi is not enabled");
        }
    }


    public void cancelConnection(ListenerAction listenerAction) throws DeviceException {
        IWrapperWifi wrapperWifi = (IWrapperWifi) wrappers[Service.WIFI];
        if (wrapperWifi.isEnabled()) {
            wrapperWifi.cancelConnect(listenerAction);
        } else {
            throw new DeviceException("Wifi is not enabled");
        }
    }

    public void updateListener(ListenerApp listenerApp) {
        for (AbstractWrapper wrapper : wrappers) {
            wrapper.updateListener(listenerApp);
        }
    }

    public int checkState() {
        int enabled = 0;
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                enabled++;
            }
        }
        return enabled;
    }

    private void bothDiscovery(final DiscoveryListener discovery) {

        @SuppressLint("HandlerLeak") final Handler mHandler = new Handler(Looper.getMainLooper()) {
            // Used handler to avoid updating views in other threads than the main thread
            public void handleMessage(Message msg) {
                discovery.onDiscoveryCompleted(mapAddressDevice);
            }
        };

        for (AbstractWrapper wrapper : wrappers) {
            wrapper.discovery(discovery);
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

    private void processListenerAdapter(int type, boolean success, Context context,
                                        final ListenerAdapter listenerAdapter) {
        if (success) {
            try {
                wrappers[type].init(config, context);
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

    public interface ListenerBothDiscovery {
        void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice);
    }

}
