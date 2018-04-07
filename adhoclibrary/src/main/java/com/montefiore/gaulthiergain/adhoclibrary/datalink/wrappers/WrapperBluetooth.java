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
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.ListenerDataLink;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.DeviceAlreadyConnectedException;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.util.SHeader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WrapperBluetooth extends WrapperConnOriented {

    private static final String TAG = "[AdHoc][WrapperBt]";

    private boolean secure;
    private String ownStringUUID;
    private BluetoothManager bluetoothManager;

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
        this.ownStringUUID = BluetoothUtil.UUID + ownMac.replace(":", "").toLowerCase();
        this.listenServer();
    }

    @Override
    public void connect(AdHocDevice device) throws DeviceAlreadyConnectedException {

        BluetoothAdHocDevice btDevice = (BluetoothAdHocDevice) device;
        if (!neighbors.getNeighbors().containsKey(btDevice.getUuid())) {
            _connect(btDevice);
        } else {
            throw new DeviceAlreadyConnectedException(btDevice.getUuid()
                    + " is already connected");
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
            public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapNameDevice) {

                mapMacDevices.clear(); //todo refactor this

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
                bluetoothManager.unregisterDiscovery();
            }

        });
    }

    @Override
    public HashMap<String, AdHocDevice> getPaired() {

        // Clear the discovered device
        mapMacDevices.clear();

        // Add paired devices into the mapUuidDevices
        for (Map.Entry<String, BluetoothAdHocDevice> entry : bluetoothManager.getPairedDevices().entrySet()) {

            if (!mapMacDevices.containsKey(entry.getValue().getUuid())) {
                if (v)
                    Log.d(TAG, "Add paired " + entry.getValue().getUuid() + " into mapUuidDevices");
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

        });

        bluetoothServiceClient.setListenerAutoConnect(new BluetoothServiceClient.ListenerAutoConnect() {
            @Override
            public void connected(UUID uuid, SocketManager network) throws IOException, NoConnectionException {

                String remoteUUIDString = uuid.toString();

                // Add network to temporary hashmap
                mapAddrNetwork.put(remoteUUIDString, network);

                // Send CONNECT message to establish the pairing
                bluetoothServiceClient.send(new MessageAdHoc(
                        new SHeader(CONNECT_SERVER, ownStringUUID, ownMac, label, ownName)));

            }
        });

        // Start the bluetoothServiceClient thread
        new Thread(bluetoothServiceClient).start();
    }

    private void processMsgReceived(MessageAdHoc message) throws IOException {
        if (v) Log.d(TAG, "Message rcvd " + message.toString());

        switch (message.getHeader().getType()) {
            case CONNECT_SERVER: {

                // Get Messsage Header
                SHeader header = (SHeader) message.getHeader();

                // Get socket manager to send message
                SocketManager socketManager = serviceServer.getActiveConnections().get(header.getMac());
                if (socketManager != null) {

                    // Send new message
                    socketManager.sendMessage(new MessageAdHoc(
                            new SHeader(CONNECT_CLIENT, ownStringUUID, ownMac, label, ownName)));

                    receivedPeerMsg(header, socketManager);
                }
                break;
            }
            case CONNECT_CLIENT: {

                // Get Messsage Header
                SHeader header = (SHeader) message.getHeader();

                SocketManager socketManager = mapAddrNetwork.get(header.getAddress());
                if (socketManager != null) {
                    receivedPeerMsg(header, socketManager);
                }
                break;
            }
            case BROADCAST: {

                // Get Messsage Header
                SHeader header = (SHeader) message.getHeader();

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
