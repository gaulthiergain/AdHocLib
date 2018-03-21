package com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.AbstractRemoteConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.RemoteBtConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ActiveConnections;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DiscoveredDevice;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ListenerAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvAbstractException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.DeviceAlreadyConnectedException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WrapperHybridBt extends AbstractWrapper {

    private boolean bluetoothEnabled;

    private UUID ownUUID;
    private String ownMac;
    private boolean secure;
    private String ownAddress;
    private BluetoothManager bluetoothManager;
    private BluetoothServiceServer bluetoothServiceServer;
    private HashMap<String, BluetoothAdHocDevice> hashMapDevices;

    private static final String TAG = "[AdHoc][WrapperBt]";
    private HashMap<String, String> mapLabelUuid;
    private HashMap<String, DiscoveredDevice> mapAddressDevice;
    private HashMap<String, NetworkManager> hashmapUuidNetwork;

    public WrapperHybridBt(boolean v, Context context, boolean secure, short nbThreads,
                           String ownAddress, ActiveConnections activeConnections,
                           HashMap<String, DiscoveredDevice> mapAddressDevice,
                           ListenerAodv listenerAodv, ListenerDataLinkAodv listenerDataLinkAodv) throws IOException {

        super(v, context, activeConnections, listenerAodv, listenerDataLinkAodv);

        try {
            this.bluetoothManager = new BluetoothManager(v, context);
        } catch (DeviceException e) {
            bluetoothEnabled = false;
        }
        if (bluetoothManager.isEnabled()) {

            this.bluetoothEnabled = true;

            this.secure = secure;
            this.hashMapDevices = new HashMap<>();
            this.ownMac = BluetoothUtil.getCurrentMac(context);

            this.ownAddress = ownAddress;
            this.ownUUID = UUID.fromString(BluetoothUtil.UUID + ownMac.replace(":", "").toLowerCase());
            this.ownName = BluetoothUtil.getCurrentName();

            this.mapLabelUuid = new HashMap<>();
            this.mapAddressDevice = mapAddressDevice;
            this.hashmapUuidNetwork = new HashMap<>();

            this.listenServer(nbThreads);
        } else {
            bluetoothEnabled = false;
        }
    }

    public boolean isEnabled() {
        return bluetoothEnabled;
    }

    @Override
    public void unregisterConnection() {
        // not used
    }

    public void connect(DiscoveredDevice device) {

        String shortUuid = device.getAddress().replace(":", "").toLowerCase();
        BluetoothAdHocDevice btDevice = hashMapDevices.get(shortUuid);
        if (btDevice != null) {
            //todo remove statement below and add checking hybrid list via name
            if (!activeConnections.getActivesConnections().containsKey(btDevice.getShortUuid())) {
                _connect(btDevice);
            } else {
                listenerAodv.catchException(new DeviceAlreadyConnectedException(btDevice.getShortUuid()
                        + " is already connected"));
            }
        }
    }

    public void listenServer(short nbThreads) throws IOException {

        bluetoothServiceServer = new BluetoothServiceServer(v, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException | NoConnectionException | AodvAbstractException e) {
                    listenerAodv.catchException(e);
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
                listenerAodv.catchException(e);
            }

            @Override
            public void onConnectionClosed(AbstractRemoteConnection remoteDevice) {

                //Get label from ip
                String remoteLabel = mapLabelUuid.get(remoteDevice.getDeviceAddress());

                if (v) Log.d(TAG, "Link broken with " + remoteLabel);

                try {
                    listenerDataLinkAodv.brokenLink(remoteLabel);
                } catch (IOException e) {
                    listenerAodv.catchException(e);
                } catch (NoConnectionException e) {
                    listenerAodv.catchException(e);
                }

                remoteDevice.setDeviceAddress(remoteLabel);

                listenerAodv.onConnectionClosed(remoteDevice);
            }

            @Override
            public void onConnection(AbstractRemoteConnection remoteDevice) {

            }
        });

        // Start the bluetoothServiceServer listening process
        bluetoothServiceServer.listen(nbThreads, secure, "secure",
                BluetoothAdapter.getDefaultAdapter(), ownUUID);
    }

    private void _connect(final BluetoothAdHocDevice bluetoothAdHocDevice) {
        final BluetoothServiceClient bluetoothServiceClient = new BluetoothServiceClient(v, context,
                new MessageListener() {
                    @Override
                    public void onMessageReceived(MessageAdHoc message) {
                        try {
                            processMsgReceived(message);
                        } catch (IOException | NoConnectionException | AodvAbstractException e) {
                            listenerAodv.catchException(e);
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
                        listenerAodv.catchException(e);
                    }

                    @Override
                    public void onConnectionClosed(AbstractRemoteConnection remoteDevice) {

                        //Get label from ip
                        String remoteLabel = mapLabelUuid.get(remoteDevice.getDeviceAddress());

                        if (v) Log.d(TAG, "Link broken with " + remoteLabel);

                        try {
                            listenerDataLinkAodv.brokenLink(remoteLabel);
                        } catch (IOException e) {
                            listenerAodv.catchException(e);
                        } catch (NoConnectionException e) {
                            listenerAodv.catchException(e);
                        }

                        remoteDevice.setDeviceAddress(remoteLabel);

                        listenerAodv.onConnectionClosed(remoteDevice);
                    }

                    @Override
                    public void onConnection(AbstractRemoteConnection remoteDevice) {

                    }

                }, true, secure, ATTEMPTS, bluetoothAdHocDevice);

        bluetoothServiceClient.setListenerAutoConnect(new BluetoothServiceClient.ListenerAutoConnect() {
            @Override
            public void connected(UUID uuid, NetworkManager network) throws IOException, NoConnectionException {

                // Add network to temporary hashmap
                hashmapUuidNetwork.put(uuid.toString(), network);

                // Send CONNECT message to establish the pairing
                bluetoothServiceClient.send(new MessageAdHoc(
                        new Header("CONNECT_SERVER", ownAddress, ownName), ownMac));

            }
        });

        // Start the bluetoothServiceClient thread
        new Thread(bluetoothServiceClient).start();
    }

    public void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {
        Log.d(TAG, "Message rcvd " + message.toString());
        switch (message.getHeader().getType()) {
            case "CONNECT_SERVER":
                NetworkManager networkManagerBt = bluetoothServiceServer.getActiveConnections().get(message.getPdu().toString());

                if (networkManagerBt != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkManagerBt);

                    networkManagerBt.sendMessage(new MessageAdHoc(
                            new Header("CONNECT_CLIENT", ownAddress, ownName), ownUUID.toString()));

                    Log.d(TAG, "Add couple: " + message.getPdu().toString()
                            + " " + message.getHeader().getSenderAddr());

                    // Add mapping MAC - label
                    mapLabelUuid.put(message.getPdu().toString(),
                            message.getHeader().getSenderAddr());

                    // callback connection
                    listenerAodv.onConnection(new RemoteBtConnection(message.getHeader().getSenderAddr(),
                            message.getHeader().getSenderName()));
                }
                break;
            case "CONNECT_CLIENT":
                NetworkManager networkManagerServer = hashmapUuidNetwork.get(message.getPdu().toString());
                if (networkManagerServer != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkManagerServer);

                    Log.d(TAG, "Add couple: " + message.getPdu().toString()
                            + " " + message.getHeader().getSenderAddr());

                    // Add mapping MAC - label
                    mapLabelUuid.put(message.getPdu().toString(),
                            message.getHeader().getSenderAddr());

                    // callback connection
                    listenerAodv.onConnection(new RemoteBtConnection(message.getHeader().getSenderAddr(),
                            message.getHeader().getSenderName()));
                }
                break;
            default:
                // Handle messages in protocol scope
                listenerDataLinkAodv.processMsgReceived(message);
        }
    }

    @Override
    public void stopListening() throws IOException {
        bluetoothServiceServer.stopListening();
    }

    @Override
    public void getPaired() {
        // Add paired devices into the hashMapDevices
        for (Map.Entry<String, BluetoothAdHocDevice> entry : bluetoothManager.getPairedDevices().entrySet()) {
            if (!hashMapDevices.containsKey(entry.getValue().getShortUuid())) {
                hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                if (v) Log.d(TAG, "Add paired " + entry.getValue().getShortUuid()
                        + " into hashMapDevices");
            }
        }

        listenerAodv.onPairedCompleted();
    }

    public void discovery() {
        bluetoothManager.discovery(new com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.DiscoveryListener() {
            @Override
            public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                // Add no paired devices into the hashMapDevices
                for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
                    if (!hashMapDevices.containsKey(entry.getValue().getShortUuid())) {
                        hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                        if (v) Log.d(TAG, "Add no paired " + entry.getValue().getShortUuid()
                                + " into hashMapDevices");
                        mapAddressDevice.put(entry.getValue().getDevice().getAddress(),
                                new DiscoveredDevice(entry.getValue().getDevice().getAddress(),
                                        entry.getValue().getDevice().getName(), DiscoveredDevice.BLUETOOTH));
                    }
                }

                if (discoveryListener != null) {
                    listenerAodv.onDiscoveryCompleted(mapAddressDevice);
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
    public void disconnect() {
        //todo array list of bluetooth client and iterate over to disconnect
    }

    /**
     * Method allowing to update the name of the device.
     */
    public void updateName(String name) {
        bluetoothManager.updateDeviceName(name);
    }
}
