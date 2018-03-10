package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.AbstractRemoteConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.RemoteBtConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.MaxThreadReachedException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
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

public class WrapperHybridBt extends WrapperBluetooth {

    private static final String TAG = "[AdHoc][WrapperBtHy]";
    private HashMap<String, String> mapLabelUuid;
    private HashMap<String, DiscoveredDevice> mapAddressDevice;
    private HashMap<String, NetworkObject> hashmapUuidNetwork;
    private boolean finishDiscovery = false;

    public WrapperHybridBt(boolean v, Context context, boolean secure, short nbThreads,
                           String ownAddress, ActiveConnections activeConnections,
                           HashMap<String, DiscoveredDevice> mapAddressDevice,
                           ListenerAodv listenerAodv, ListenerDataLinkAodv listenerDataLinkAodv)
            throws DeviceException, IOException {

        super(v, context, secure, activeConnections, listenerAodv, listenerDataLinkAodv);

        this.ownAddress = ownAddress;
        this.ownUUID = UUID.fromString(BluetoothUtil.UUID + ownMac.replace(":", "").toLowerCase());
        this.ownName = BluetoothUtil.getCurrentName(); // todo update with label if necessary

        this.mapLabelUuid = new HashMap<>();
        this.mapAddressDevice = mapAddressDevice;
        this.hashmapUuidNetwork = new HashMap<>();

        // Check if the bluetooth adapter is enabled
        listenServer(nbThreads);
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
        try {
            bluetoothServiceServer.listen(nbThreads, secure, "secure",
                    BluetoothAdapter.getDefaultAdapter(), ownUUID);
        } catch (MaxThreadReachedException e) {
            listenerAodv.catchException(e);
        }

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
            public void connected(UUID uuid, NetworkObject network) throws IOException, NoConnectionException {

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
                NetworkObject networkObjectBt = bluetoothServiceServer.getActiveConnections().get(message.getPdu().toString());

                if (networkObjectBt != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkObjectBt);

                    networkObjectBt.sendObjectStream(new MessageAdHoc(
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
                NetworkObject networkObjectServer = hashmapUuidNetwork.get(message.getPdu().toString());
                if (networkObjectServer != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkObjectServer);

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

                finishDiscovery = true;

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

    /**
     * Method allowing to update the name of the device.
     */
    public void updateName(String name) {
        bluetoothManager.updateDeviceName(name);
    }

    public boolean isFinishDiscovery() {
        return finishDiscovery;
    }

    public void setFinishDiscovery(boolean finishDiscovery) {
        this.finishDiscovery = finishDiscovery;
    }
}
