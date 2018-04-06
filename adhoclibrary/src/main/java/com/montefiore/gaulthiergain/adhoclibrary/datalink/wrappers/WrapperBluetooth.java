package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothManager;
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
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvAbstractException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.DeviceAlreadyConnectedException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WrapperBluetooth extends WrapperConnOriented {

    private static final String TAG = "[AdHoc][WrapperBt]";

    private UUID ownUUID;
    private boolean secure;
    private BluetoothManager bluetoothManager;

    private HashMap<String, BluetoothAdHocDevice> mapUuidDevices;

    public WrapperBluetooth(boolean verbose, Context context, Config config,
                            HashMap<String, AdHocDevice> mapAddressDevice,
                            ListenerApp listenerAodv, ListenerDataLink listenerDataLink) throws IOException {

        super(verbose, context, config, config.getNbThreadBt(), mapAddressDevice, listenerAodv, listenerDataLink);

        try {
            this.type = Service.BLUETOOTH;
            this.bluetoothManager = new BluetoothManager(v, context);
            if (bluetoothManager.isEnabled()) {
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
        this.ownMac = BluetoothUtil.getCurrentMac(context);
        this.ownUUID = UUID.fromString(macToUUID(ownMac));
        this.mapUuidDevices = new HashMap<>();
        this.listenServer();
    }

    @Override
    public void connect(AdHocDevice device) throws DeviceAlreadyConnectedException {

        String uuid = macToUUID(device.getDeviceAddress());
        BluetoothAdHocDevice btDevice = mapUuidDevices.get(uuid);
        if (btDevice != null) {
            if (!neighbors.getNeighbors().containsKey(btDevice.getUuid())) {
                _connect(btDevice);
            } else {
                throw new DeviceAlreadyConnectedException(btDevice.getUuid()
                        + " is already connected");
            }
        }
    }

    @Override
    public void stopListening() throws IOException {
        serviceServer.stopListening();
    }

    @Override
    public void discovery() {
        bluetoothManager.discovery(new DiscoveryListener() {
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
                listenerApp.onDeviceDiscovered(device);
            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, AdHocDevice> hashMapBluetoothDevice) {

                mapMacDevice.clear();
                mapUuidDevices.clear();

                // Add no paired devices into the mapUuidDevices
                for (Map.Entry<String, AdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
                    BluetoothAdHocDevice btDevice = (BluetoothAdHocDevice) entry.getValue();
                    if (!mapUuidDevices.containsKey(btDevice.getUuid())) {
                        mapUuidDevices.put(btDevice.getUuid(), btDevice);
                        if (v)
                            Log.d(TAG, "Add no paired " + btDevice.getUuid() + " into mapUuidDevices");
                        mapMacDevice.put(btDevice.getDeviceAddress(), btDevice);
                    }
                }

                if (discoveryListener != null) {
                    listenerApp.onDiscoveryCompleted(mapMacDevice);
                }

                discoveryCompleted = true;

                // Stop and unregister to the discovery process
                bluetoothManager.unregisterDiscovery();
            }

        });
    }

    @Override
    public HashMap<String, AdHocDevice> getPaired() {

        // Clear the discovered device
        mapMacDevice.clear();
        mapUuidDevices.clear();

        // Add paired devices into the mapUuidDevices
        for (Map.Entry<String, BluetoothAdHocDevice> entry : bluetoothManager.getPairedDevices().entrySet()) {

            if (!mapUuidDevices.containsKey(entry.getValue().getUuid())) {
                mapUuidDevices.put(entry.getValue().getUuid(), entry.getValue());
                if (v)
                    Log.d(TAG, "Add paired " + entry.getValue().getUuid() + " into mapUuidDevices");
                mapMacDevice.put(entry.getValue().getDeviceAddress(), entry.getValue());
            }
        }

        return mapMacDevice;
    }

    @Override
    public void unregisterConnection() {
        // Not used in bluetooth context
    }

    @Override
    public void enable(int duration, ListenerAdapter listenerAdapter) throws BluetoothBadDuration {
        bluetoothManager.enableDiscovery(duration);
        bluetoothManager.onEnableBluetooth(listenerAdapter);
        enabled = true;
    }

    @Override
    public void disable() {
        bluetoothManager.disable();
        enabled = false;
    }


    @Override
    public void unregisterAdapter() {
        bluetoothManager.unregisterEnableAdapter();
    }

    @Override
    public void resetDeviceName() {
        bluetoothManager.resetDeviceName();
    }

    @Override
    public boolean updateDeviceName(String name) {
        return bluetoothManager.updateDeviceName(name);
    }

    @Override
    public String getAdapterName() {
        return bluetoothManager.getAdapterName();
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

        });

        // Start the serviceServer listening process
        serviceServer.listen(new ServiceConfig(nbThreads, secure, BluetoothAdapter.getDefaultAdapter(), ownUUID));
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

        });

        bluetoothServiceClient.setListenerAutoConnect(new BluetoothServiceClient.ListenerAutoConnect() {
            @Override
            public void connected(UUID uuid, SocketManager network) throws IOException, NoConnectionException {

                String remoteUUIDString = uuid.toString();

                // Add network to temporary hashmap
                mapAddrNetwork.put(remoteUUIDString, network);

                // Send CONNECT message to establish the pairing
                bluetoothServiceClient.send(new MessageAdHoc(
                        new Header(CONNECT_SERVER, label, ownName), ownMac));

            }
        });

        // Start the bluetoothServiceClient thread
        new Thread(bluetoothServiceClient).start();
    }

    private void processMsgReceived(MessageAdHoc message) throws IOException {
        if (v) Log.d(TAG, "Message rcvd " + message.toString());

        switch (message.getHeader().getType()) {
            case CONNECT_SERVER: {

                String remoteMac = (String) message.getPdu();
                String remoteUUID = macToUUID(remoteMac);
                String remoteLabel = message.getHeader().getSenderAddr();

                SocketManager socketManager = serviceServer.getActiveConnections().get(remoteMac);
                if (socketManager != null) {

                    socketManager.sendMessage(new MessageAdHoc(
                            new Header(CONNECT_CLIENT, label, ownName), ownUUID.toString()));

                    if (v) Log.d(TAG, "Add mapping: " + remoteUUID + " " + remoteLabel);

                    // Add mapping MAC - label
                    mapAddrLabel.put(remoteUUID, remoteLabel);

                    // Add mapping label - remoteConnection
                    mapLabelRemoteName.put(remoteLabel, message.getHeader().getSenderName());

                    if (!neighbors.getNeighbors().containsKey(remoteLabel)) {
                        // Callback connection
                        listenerApp.onConnection(remoteLabel, message.getHeader().getSenderName(), 0);
                    }

                    // Add the neighbor into the neighbors object
                    neighbors.addNeighbors(remoteLabel, socketManager);
                }
                break;
            }
            case CONNECT_CLIENT: {

                String remoteUUID = (String) message.getPdu();
                String remoteLabel = message.getHeader().getSenderAddr();

                SocketManager socketManager = mapAddrNetwork.get(remoteUUID);
                if (socketManager != null) {

                    if (v) Log.d(TAG, "Add mapping: " + remoteUUID + " " + remoteLabel);

                    // Add mapping UUID - label
                    mapAddrLabel.put(remoteUUID, remoteLabel);

                    // Add mapping label - remoteConnection
                    mapLabelRemoteName.put(remoteLabel, message.getHeader().getSenderName());

                    if (!neighbors.getNeighbors().containsKey(remoteLabel)) {
                        // Callback connection
                        listenerApp.onConnection(remoteLabel, message.getHeader().getSenderName(), 0);
                    }

                    // Add the active connection into the neighbors object
                    neighbors.addNeighbors(remoteLabel, socketManager);
                }
                break;
            }
            case BROADCAST: {
                listenerApp.onReceivedData(message.getHeader().getSenderName(),
                        message.getHeader().getSenderAddr(), message.getPdu());
                break;
            }
            default:
                // Handle messages in protocol scope
                listenerDataLink.processMsgReceived(message);
        }
    }

    private String macToUUID(String mac) {
        return BluetoothUtil.UUID + mac.replace(":", "").toLowerCase();
    }

}
