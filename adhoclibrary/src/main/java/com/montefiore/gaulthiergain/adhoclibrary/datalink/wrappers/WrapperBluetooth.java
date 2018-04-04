package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.RemoteConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.Neighbors;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.NetworkObject;
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

public class WrapperBluetooth extends AbstractWrapper {

    private static final String TAG = "[AdHoc][WrapperBt]";

    private UUID ownUUID;
    private boolean secure;
    private BluetoothManager bluetoothManager;
    private BluetoothServiceServer bluetoothServiceServer;

    private HashMap<String, SocketManager> mapUuidNetwork;
    private HashMap<String, BluetoothAdHocDevice> mapUuidDevices;

    public WrapperBluetooth(boolean verbose, Context context, Config config, Neighbors neighbors,
                            HashMap<String, AdHocDevice> mapAddressDevice,
                            ListenerApp listenerAodv, ListenerDataLink listenerDataLink) throws IOException {

        super(verbose, context, config.isJson(), config.getNbThreadBt(), config.isBackground(), config.getLabel(),
                mapAddressDevice, neighbors, listenerAodv, listenerDataLink);

        try {
            this.bluetoothManager = new BluetoothManager(v, context);
            if (bluetoothManager.isEnabled()) {

                this.secure = config.isSecure();
                this.type = DataLinkManager.BLUETOOTH;
                this.ownMac = BluetoothUtil.getCurrentMac(context);
                this.ownUUID = UUID.fromString(BluetoothUtil.UUID + ownMac.replace(":", "").toLowerCase());
                this.ownName = BluetoothUtil.getCurrentName();

                this.mapUuidDevices = new HashMap<>();
                this.mapUuidNetwork = new HashMap<>();

                this.listenServer();
            } else {
                enabled = false;
            }
        } catch (DeviceException e) {
            enabled = false;
        }
    }

    /*-------------------------------------Override methods---------------------------------------*/

    @Override
    public void connect(AdHocDevice device) {

        String shortUuid = device.getMac().replace(":", "").toLowerCase();
        BluetoothAdHocDevice btDevice = mapUuidDevices.get(shortUuid);
        if (btDevice != null) {
            if (!neighbors.getNeighbors().containsKey(btDevice.getShortUuid())) {
                _connect(btDevice);
            } else {
                listenerApp.traceException(new DeviceAlreadyConnectedException(btDevice.getShortUuid()
                        + " is already connected"));
            }
        }
    }

    @Override
    public void stopListening() throws IOException {
        bluetoothServiceServer.stopListening();
    }

    @Override
    public void discovery() {
        bluetoothManager.discovery(new com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.DiscoveryListener() {
            @Override
            public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {

                mapAddressDevice.clear();

                // Add no paired devices into the mapUuidDevices
                for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
                    if (!mapUuidDevices.containsKey(entry.getValue().getShortUuid())) {
                        mapUuidDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                        if (v) Log.d(TAG, "Add no paired " + entry.getValue().getShortUuid()
                                + " into mapUuidDevices");
                        mapAddressDevice.put(entry.getValue().getDevice().getAddress(),
                                new AdHocDevice(entry.getValue().getDevice().getAddress(),
                                        entry.getValue().getDevice().getName(), type));
                    }
                }

                if (discoveryListener != null) {
                    listenerApp.onDiscoveryCompleted(mapAddressDevice);
                }

                discoveryCompleted = true;

                // Stop and unregister to the discovery process
                bluetoothManager.unregisterDiscovery();
            }

            @Override
            public void onDiscoveryStarted() {

            }

            @Override
            public void onDeviceFound(BluetoothDevice device) {

            }

            @Override
            public void onScanModeChange(int currentMode, int oldMode) {

            }
        });
    }

    @Override
    public HashMap<String, AdHocDevice> getPaired() {

        mapAddressDevice.clear();

        // Add paired devices into the mapUuidDevices
        for (Map.Entry<String, BluetoothAdHocDevice> entry : bluetoothManager.getPairedDevices().entrySet()) {
            if (!mapUuidDevices.containsKey(entry.getValue().getShortUuid())) {
                mapUuidDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                if (v) Log.d(TAG, "Add paired " + entry.getValue().getShortUuid()
                        + " into mapUuidDevices");

                mapAddressDevice.put(entry.getValue().getDevice().getAddress(),
                        new AdHocDevice(entry.getValue().getDevice().getAddress(),
                                entry.getValue().getDevice().getName(), type));
            }
        }

        return mapAddressDevice;
    }

    @Override
    public void unregisterConnection() {
        // Not used in bluetooth context
    }

    @Override
    public void enable(int duration, ListenerAdapter listenerAdapter) {
        try {
            bluetoothManager.enableDiscovery(duration);
            bluetoothManager.onEnableBluetooth(listenerAdapter);
            enabled = true;
        } catch (BluetoothBadDuration e) {
            listenerApp.traceException(e);
        }
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
    public void updateName(String name) {
        bluetoothManager.updateDeviceName(name);
    }

    @Override
    public void listenServer() throws IOException {

        bluetoothServiceServer = new BluetoothServiceServer(v, context, json, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException | NoConnectionException | AodvAbstractException e) {
                    listenerApp.traceException(e);
                }
            }

            @Override
            public void onMessageSent(MessageAdHoc message) {
                if (v) Log.d(TAG, "Message sent: " + message.getPdu().toString());
            }

            @Override
            public void onForward(MessageAdHoc message) {
                if (v) Log.d(TAG, "OnForward: " + message.getPdu().toString());
            }

            @Override
            public void catchException(Exception e) {
                listenerApp.traceException(e);
            }

            @Override
            public void onConnectionClosed(RemoteConnection remoteDevice) {

                //Get label from ip
                String remoteLabel = mapLabelAddr.get(remoteDevice.getDeviceAddress());

                if (v) Log.d(TAG, "Link broken with " + remoteLabel);

                try {
                    listenerDataLink.brokenLink(remoteLabel);
                    neighbors.getNeighbors().remove(remoteLabel);
                } catch (IOException e) {
                    listenerApp.traceException(e);
                } catch (NoConnectionException e) {
                    listenerApp.traceException(e);
                }

                listenerApp.onConnectionClosed(remoteLabel, remoteDevice.getDeviceName());
            }

            @Override
            public void onConnection(RemoteConnection remoteDevice) {

            }

            @Override
            public void onConnectionFailed(RemoteConnection remoteDevice) {
                listenerApp.onConnectionFailed(remoteDevice.getDeviceName());
            }
        });

        // Start the bluetoothServiceServer listening process
        bluetoothServiceServer.listen(nbThreads, secure, "secure",
                BluetoothAdapter.getDefaultAdapter(), ownUUID);
    }

    /*--------------------------------------Private methods---------------------------------------*/
    private void _connect(final BluetoothAdHocDevice bluetoothAdHocDevice) {
        final BluetoothServiceClient bluetoothServiceClient = new BluetoothServiceClient(v, context,
                json, background, secure, ATTEMPTS, bluetoothAdHocDevice, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException | NoConnectionException | AodvAbstractException e) {
                    listenerApp.traceException(e);
                }
            }

            @Override
            public void onMessageSent(MessageAdHoc message) {
                if (v) Log.d(TAG, "Message sent: " + message.getPdu().toString());
            }

            @Override
            public void onForward(MessageAdHoc message) {
                if (v) Log.d(TAG, "OnForward: " + message.getPdu().toString());
            }

            @Override
            public void catchException(Exception e) {
                listenerApp.traceException(e);
            }

            @Override
            public void onConnectionClosed(RemoteConnection remoteDevice) {

                //Get label from ip
                String remoteLabel = mapLabelAddr.get(remoteDevice.getDeviceAddress());

                if (v) Log.d(TAG, "Link broken with " + remoteLabel);

                try {
                    listenerDataLink.brokenLink(remoteLabel);
                    neighbors.getNeighbors().remove(remoteLabel);
                } catch (IOException e) {
                    listenerApp.traceException(e);
                } catch (NoConnectionException e) {
                    listenerApp.traceException(e);
                }

                listenerApp.onConnectionClosed(remoteLabel, remoteDevice.getDeviceName());
            }

            @Override
            public void onConnection(RemoteConnection remoteDevice) {

            }

            @Override
            public void onConnectionFailed(RemoteConnection remoteDevice) {
                listenerApp.onConnectionFailed(remoteDevice.getDeviceName());
            }

        });

        bluetoothServiceClient.setListenerAutoConnect(new BluetoothServiceClient.ListenerAutoConnect() {
            @Override
            public void connected(UUID uuid, SocketManager network) throws IOException, NoConnectionException {

                // Add network to temporary hashmap
                mapUuidNetwork.put(uuid.toString(), network);

                // Add bluetooth client to hashmap
                mapUuidClients.put(uuid.toString(), bluetoothServiceClient);

                // Send CONNECT message to establish the pairing
                bluetoothServiceClient.send(new MessageAdHoc(
                        new Header(CONNECT_SERVER, label, ownName), ownMac));

            }
        });

        // Start the bluetoothServiceClient thread
        new Thread(bluetoothServiceClient).start();
    }

    private void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {
        if (v) Log.d(TAG, "Message rcvd " + message.toString());
        switch (message.getHeader().getType()) {
            case CONNECT_SERVER: {
                SocketManager socketManager = bluetoothServiceServer.getActiveConnections().get(message.getPdu().toString());

                if (socketManager != null) {

                    socketManager.sendMessage(new MessageAdHoc(
                            new Header(CONNECT_CLIENT, label, ownName), ownUUID.toString()));

                    if (v) Log.d(TAG, "Add mapping: " + message.getPdu().toString()
                            + " " + message.getHeader().getSenderAddr());

                    // Add mapping MAC - label
                    mapLabelAddr.put(message.getPdu().toString(),
                            message.getHeader().getSenderAddr());

                    if (!neighbors.getNeighbors().containsKey(message.getHeader().getSenderAddr())) {
                        // Callback connection
                        listenerApp.onConnection(message.getHeader().getSenderAddr(),
                                message.getHeader().getSenderName());
                    }

                    // Add the neighbor into the neighbors object
                    neighbors.addNeighbors(message.getHeader().getSenderAddr(),
                            new NetworkObject(type, socketManager));
                }
                break;
            }
            case CONNECT_CLIENT: {
                SocketManager socketManager = mapUuidNetwork.get(message.getPdu().toString());
                if (socketManager != null) {

                    if (v) Log.d(TAG, "Add mapping: " + message.getPdu().toString()
                            + " " + message.getHeader().getSenderAddr());

                    // Add mapping MAC - label
                    mapLabelAddr.put(message.getPdu().toString(),
                            message.getHeader().getSenderAddr());

                    if (!neighbors.getNeighbors().containsKey(message.getHeader().getSenderAddr())) {
                        // Callback connection
                        listenerApp.onConnection(message.getHeader().getSenderAddr(),
                                message.getHeader().getSenderName());
                    }

                    // Add the active connection into the neighbors object
                    neighbors.addNeighbors(message.getHeader().getSenderAddr(),
                            new NetworkObject(type, socketManager));
                }
                break;
            }
            default:
                // Handle messages in protocol scope
                listenerDataLink.processMsgReceived(message);
        }
    }
}
