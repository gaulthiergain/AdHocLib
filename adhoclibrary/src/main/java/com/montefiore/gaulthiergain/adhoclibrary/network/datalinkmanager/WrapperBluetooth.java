package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceConfig;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceMessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

class WrapperBluetooth extends WrapperConnOriented {

    private static final String TAG = "[AdHoc][WrapperBt]";

    private boolean secure;
    private String ownStringUUID;
    private BluetoothAdHocManager bluetoothAdHocManager;

    WrapperBluetooth(boolean verbose, Context context, Config config,
                     HashMap<String, AdHocDevice> mapAddressDevice,
                     ListenerApp listenerApp, ListenerDataLink listenerDataLink) throws IOException {

        super(verbose, config, config.getNbThreadBt(), mapAddressDevice, listenerApp, listenerDataLink);

        this.type = Service.BLUETOOTH;
        if (BluetoothUtil.isEnabled()) {
            this.bluetoothAdHocManager = new BluetoothAdHocManager(v, context);
            this.init(config, context);
        } else {
            this.enabled = false;
        }
    }

    /*-------------------------------------Override methods---------------------------------------*/

    @Override
    void init(Config config, Context context) throws IOException {
        this.secure = config.isSecure();
        this.ownName = BluetoothUtil.getCurrentName();
        this.ownMac = BluetoothUtil.getCurrentMac(context);
        this.listenerDataLink.initInfos(ownMac, ownName);
        this.ownStringUUID = BluetoothUtil.UUID + ownMac.replace(":", "").toLowerCase();
        this.listenServer();
    }

    @Override
    void connect(short attemps, AdHocDevice device) throws DeviceException {

        BluetoothAdHocDevice btDevice = (BluetoothAdHocDevice) mapMacDevices.get(device.getMacAddress());
        if (btDevice != null) {
            if (serviceServer.getActiveConnections()!= null && !serviceServer.getActiveConnections().containsKey(btDevice.getMacAddress())) {
                _connect(attemps, btDevice);
            } else {
                throw new DeviceException(device.getDeviceName()
                        + "(" + device.getMacAddress() + ") is already connected");
            }
        }
    }

    @Override
    void stopListening() throws IOException {
        serviceServer.stopListening();
    }

    @Override
    void discovery(final DiscoveryListener discoveryListener) {
        bluetoothAdHocManager.discovery(new DiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {
                discoveryListener.onDiscoveryStarted();
            }

            @Override
            public void onDiscoveryFailed(Exception e) {
                discoveryListener.onDiscoveryFailed(e);
            }

            @Override
            public void onDeviceDiscovered(AdHocDevice device) {
                if (!mapMacDevices.containsKey(device.getMacAddress())) {
                    if (v)
                        Log.d(TAG, "Add " + device.getMacAddress() + " into mapMacDevices");
                    mapMacDevices.put(device.getMacAddress(), device);
                }

                discoveryListener.onDeviceDiscovered(device);
            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapNameDevice) {

                if (bluetoothAdHocManager == null) {
                    discoveryListener.onDiscoveryFailed(
                            new DeviceException("Unable to complete the discovery due to bluetooth connectivity"));
                } else {
                    // Add device into mapMacDevices
                    for (AdHocDevice device : mapNameDevice.values()) {
                        if (!mapMacDevices.containsKey(device.getMacAddress())) {
                            if (v)
                                Log.d(TAG, "Add " + device.getMacAddress() + " into mapMacDevices");
                            mapMacDevices.put(device.getMacAddress(), device);
                        }
                    }

                    if (listenerBothDiscovery != null) {
                        listenerBothDiscovery.onDiscoveryCompleted(mapMacDevices);
                    }

                    discoveryCompleted = true;

                    // Stop and unregister to the discovery process
                    bluetoothAdHocManager.unregisterDiscovery();
                }
            }
        });
    }

    @Override
    HashMap<String, AdHocDevice> getPaired() {

        if (!BluetoothUtil.isEnabled()) {
            return null;
        }

        // Add paired devices into the mapUuidDevices
        for (Map.Entry<String, BluetoothAdHocDevice> entry : bluetoothAdHocManager.getPairedDevices().entrySet()) {

            if (!mapMacDevices.containsKey(entry.getValue().getMacAddress())) {
                if (v)
                    Log.d(TAG, "Add paired " + entry.getValue().getMacAddress() + " into mapUuidDevices");
                mapMacDevices.put(entry.getValue().getMacAddress(), entry.getValue());
            }
        }

        return mapMacDevices;
    }

    @Override
    void unregisterConnection() {
        // Not used in bluetooth context
    }

    @Override
    void enable(Context context, int duration, ListenerAdapter listenerAdapter) throws BluetoothBadDuration {
        bluetoothAdHocManager = new BluetoothAdHocManager(v, context);
        bluetoothAdHocManager.enableDiscovery(context, duration);
        bluetoothAdHocManager.onEnableBluetooth(listenerAdapter);
        enabled = true;
    }

    @Override
    void disable() {
        // Clear data structure if adapter is disabled
        mapAddrNetwork.clear();
        neighbors.getNeighbors().clear();

        bluetoothAdHocManager.disable();
        bluetoothAdHocManager = null;
        enabled = false;
    }

    @Override
    void updateContext(Context context) {
        bluetoothAdHocManager.updateContext(context);
    }

    @Override
    void unregisterAdapter() {
        bluetoothAdHocManager.unregisterAdapter();
    }

    @Override
    void resetDeviceName() {
        bluetoothAdHocManager.resetDeviceName();
    }

    @Override
    boolean updateDeviceName(String name) {
        return bluetoothAdHocManager.updateDeviceName(name);
    }

    @Override
    String getAdapterName() {
        return bluetoothAdHocManager.getAdapterName();
    }

    /*--------------------------------------Public  methods---------------------------------------*/

    public void unpairDevice(BluetoothAdHocDevice device)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        bluetoothAdHocManager.unpairDevice(device);
    }

    /*--------------------------------------Private methods---------------------------------------*/

    private void listenServer() throws IOException {
        serviceServer = new BluetoothServer(v, json, new ServiceMessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException e) {
                    listenerApp.processMsgException(e);
                }
            }

            @Override
            public void onConnectionClosed(String remoteAddress) {
                try {
                    connectionClosed(remoteAddress);
                } catch (IOException | NoConnectionException e) {
                    listenerApp.onConnectionClosedFailed(e);
                }
            }

            @Override
            public void onConnection(String remoteAddress) {
            }

            @Override
            public void onConnectionFailed(Exception e) {
                listenerApp.onConnectionFailed(e);
            }

            @Override
            public void onMsgException(Exception e) {
                listenerApp.processMsgException(e);
            }

        });

        // Start the serviceServer listening process
        serviceServer.listen(new ServiceConfig(nbThreads, secure, BluetoothAdapter.getDefaultAdapter(),
                UUID.fromString(ownStringUUID)));
    }

    private void _connect(short attemps, final BluetoothAdHocDevice bluetoothAdHocDevice) {
        final BluetoothClient bluetoothClient = new BluetoothClient(v,
                json, timeout, secure, attemps, bluetoothAdHocDevice, new ServiceMessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException e) {
                    listenerApp.processMsgException(e);
                }
            }

            @Override
            public void onConnectionClosed(String remoteAddress) {
                try {
                    connectionClosed(remoteAddress);
                } catch (IOException | NoConnectionException e) {
                    listenerApp.onConnectionClosedFailed(e);
                }
            }

            @Override
            public void onConnection(String remoteAddress) {

            }

            @Override
            public void onConnectionFailed(Exception e) {
                listenerApp.onConnectionFailed(e);
            }

            @Override
            public void onMsgException(Exception e) {
                listenerApp.processMsgException(e);
            }

        });

        bluetoothClient.setListenerAutoConnect(new BluetoothClient.ListenerAutoConnect() {
            @Override
            public void connected(UUID uuid, SocketManager network) throws IOException, NoConnectionException {

                String remoteUUIDString = uuid.toString();

                // Add network to temporary hashmap
                mapAddrNetwork.put(remoteUUIDString, network);

                // Send CONNECT message to establish the pairing
                bluetoothClient.send(new MessageAdHoc(
                        new Header(CONNECT_SERVER, ownStringUUID, ownMac, label, ownName)));

            }
        });

        // Start the bluetoothClient thread
        new Thread(bluetoothClient).start();
    }

    private void processMsgReceived(MessageAdHoc message) throws IOException {
        switch (message.getHeader().getType()) {
            case CONNECT_SERVER: {

                // Get socket manager to send message
                SocketManager socketManager = serviceServer.getActiveConnections().get(
                        message.getHeader().getMac());
                if (socketManager != null) {

                    // Send new message
                    socketManager.sendMessage(new MessageAdHoc(
                            new Header(CONNECT_CLIENT, ownStringUUID, ownMac, label, ownName)));

                    receivedPeerMsg(message.getHeader(), socketManager);
                }

                break;
            }
            case CONNECT_CLIENT: {

                SocketManager socketManager = mapAddrNetwork.get(message.getHeader().getAddress());
                if (socketManager != null) {
                    receivedPeerMsg(message.getHeader(), socketManager);
                }

                break;
            }
            case CONNECT_BROADCAST: {
                if (checkFloodEvent(((FloodMsg) message.getPdu()).getId())) {

                    // Re-broadcast message
                    broadcastExcept(message, message.getHeader().getLabel());

                    // Get Message info
                    HashSet<AdHocDevice> list = ((FloodMsg) message.getPdu()).getAdHocDevices();

                    // Remote connection(s) happen(s) in other node(s)
                    for (AdHocDevice adHocDevice : list) {
                        if (!adHocDevice.getLabel().equals(label) && !setRemoteDevices.contains(adHocDevice)
                                && !isDirectNeighbors(adHocDevice.getLabel())) {

                            adHocDevice.setDirectedConnected(false);

                            listenerApp.onConnection(adHocDevice);

                            // Update set
                            setRemoteDevices.add(adHocDevice);
                        }
                    }
                }

                break;
            }
            case DISCONNECT_BROADCAST: {
                if (checkFloodEvent((String) message.getPdu())) {

                    // Re-broadcast message
                    broadcastExcept(message, message.getHeader().getLabel());

                    // Get Message Header
                    Header header = message.getHeader();

                    AdHocDevice adHocDevice = new AdHocDevice(header.getLabel(), header.getMac(),
                            header.getName(), type, false);

                    // Remote connection is closed in other node
                    listenerApp.onConnectionClosed(adHocDevice);

                    if (setRemoteDevices.contains(adHocDevice)) {
                        setRemoteDevices.remove(adHocDevice);
                    }
                }
                break;
            }
            case BROADCAST: {

                // Get Messsage Header
                Header header = message.getHeader();

                listenerApp.onReceivedData(new AdHocDevice(header.getLabel(), header.getMac(),
                        header.getName(), type), message.getPdu());
                break;
            }
            default:
                // Handle messages in protocol scope
                listenerDataLink.processMsgReceived(message);
        }
    }
}
