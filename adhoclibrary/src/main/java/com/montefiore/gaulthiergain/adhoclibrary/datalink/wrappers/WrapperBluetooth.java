package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceConfig;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.DeviceAlreadyConnectedException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WrapperBluetooth extends WrapperConnOriented {

    private static final String TAG = "[AdHoc][WrapperBt]";

    private boolean secure;
    private String ownStringUUID;
    private BluetoothAdHocManager bluetoothAdHocManager;

    public WrapperBluetooth(boolean verbose, Context context, Config config,
                            HashMap<String, AdHocDevice> mapAddressDevice,
                            ListenerApp listenerApp, ListenerDataLink listenerDataLink) throws IOException {

        super(verbose, context, config, config.getNbThreadBt(), mapAddressDevice, listenerApp, listenerDataLink);

        try {
            this.type = Service.BLUETOOTH;
            this.bluetoothAdHocManager = new BluetoothAdHocManager(v, context);
            if (bluetoothAdHocManager.isEnabled()) {
                init(config);
            } else {
                enabled = false;
            }
        } catch (DeviceException e) {
            enabled = false;
        }
    }

    /*-------------------------------------Override methods---------------------------------------*/

    @Override
    public void init(Config config) throws IOException {
        this.secure = config.isSecure();
        this.ownName = BluetoothUtil.getCurrentName();
        this.ownMac = BluetoothUtil.getCurrentMac(context);
        this.listenerDataLink.initInfos(ownMac, ownName);
        this.ownStringUUID = BluetoothUtil.UUID + ownMac.replace(":", "").toLowerCase();
        this.listenServer();
    }

    @Override
    public void connect(AdHocDevice device) throws DeviceAlreadyConnectedException {

        BluetoothAdHocDevice btDevice = (BluetoothAdHocDevice) mapMacDevices.get(device.getMacAddress());
        if (btDevice != null) {
            if (!neighbors.getNeighbors().containsKey(btDevice.getUuid())) {
                _connect(btDevice);
            } else {
                throw new DeviceAlreadyConnectedException(btDevice.getUuid()
                        + " is already connected");
            }
        }//todo exceptsion if null

    }

    @Override
    public void stopListening() throws IOException {
        serviceServer.stopListening();
    }

    @Override
    public void discovery() {
        bluetoothAdHocManager.discovery(new DiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {
                listenerApp.onDiscoveryStarted();
            }

            @Override
            public void onDiscoveryFailed(Exception e) {
                listenerApp.onDiscoveryFailed(e);
            }

            @Override
            public void onDeviceDiscovered(AdHocDevice device) {
                if (!mapMacDevices.containsKey(device.getMacAddress())) {
                    if (v)
                        Log.d(TAG, "Add " + device.getMacAddress() + " into mapMacDevices");
                    mapMacDevices.put(device.getMacAddress(), device);
                }

                listenerApp.onDeviceDiscovered(device);
            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapNameDevice) {

                //todo refactor this

                // Add device into mapMacDevices
                for (AdHocDevice device : mapNameDevice.values()) {
                    if (!mapMacDevices.containsKey(device.getMacAddress())) {
                        if (v)
                            Log.d(TAG, "Add " + device.getMacAddress() + " into mapMacDevices");
                        mapMacDevices.put(device.getMacAddress(), device);
                    }
                }

                if (discoveryListener != null) {
                    listenerApp.onDiscoveryCompleted(mapMacDevices);
                }

                discoveryCompleted = true;

                // Stop and unregister to the discovery process
                bluetoothAdHocManager.unregisterDiscovery();
            }

        });
    }

    @Override
    public HashMap<String, AdHocDevice> getPaired() {

        if (!bluetoothAdHocManager.isEnabled()) {
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
    public void unregisterConnection() {
        // Not used in bluetooth context
    }

    @Override
    public void enable(int duration, ListenerAdapter listenerAdapter) throws BluetoothBadDuration {
        bluetoothAdHocManager.enableDiscovery(duration);
        bluetoothAdHocManager.onEnableBluetooth(listenerAdapter);
        enabled = true;
    }

    @Override
    public void disable() {
        bluetoothAdHocManager.disable();
        enabled = false;
    }


    @Override
    public void unregisterAdapter() {
        bluetoothAdHocManager.unregisterEnableAdapter();
    }

    @Override
    public void resetDeviceName() {
        bluetoothAdHocManager.resetDeviceName();
    }

    @Override
    public boolean updateDeviceName(String name) {
        return bluetoothAdHocManager.updateDeviceName(name);
    }

    @Override
    public String getAdapterName() {
        return bluetoothAdHocManager.getAdapterName();
    }

    /*--------------------------------------Private methods---------------------------------------*/

    private void listenServer() throws IOException {
        serviceServer = new BluetoothServiceServer(v, context, json, new MessageListener() {
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

    private void _connect(final BluetoothAdHocDevice bluetoothAdHocDevice) {
        final BluetoothServiceClient bluetoothServiceClient = new BluetoothServiceClient(v, context,
                json, background, secure, attemps, bluetoothAdHocDevice, new MessageListener() {
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

        bluetoothServiceClient.setListenerAutoConnect(new BluetoothServiceClient.ListenerAutoConnect() {
            @Override
            public void connected(UUID uuid, SocketManager network) throws IOException, NoConnectionException {

                String remoteUUIDString = uuid.toString();

                // Add network to temporary hashmap
                mapAddrNetwork.put(remoteUUIDString, network);

                // Send CONNECT message to establish the pairing
                bluetoothServiceClient.send(new MessageAdHoc(
                        new Header(CONNECT_SERVER, ownStringUUID, ownMac, label, ownName)));

            }
        });

        // Start the bluetoothServiceClient thread
        new Thread(bluetoothServiceClient).start();
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
                if (checkFloodEvent(message)) {

                    // Get Messsage Header
                    Header header = message.getHeader();

                    // Remote connection happens in other node
                    listenerApp.onConnection(new AdHocDevice(header.getLabel(), header.getMac(),
                            header.getName(), type, false));
                }

                break;
            }
            case DISCONNECT_BROADCAST: {
                if (checkFloodEvent(message)) {

                    // Get Messsage Header
                    Header header = message.getHeader();

                    // Remote connection is closed in other node
                    listenerApp.onConnectionClosed(new AdHocDevice(header.getLabel(), header.getMac(),
                            header.getName(), type, false));
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
