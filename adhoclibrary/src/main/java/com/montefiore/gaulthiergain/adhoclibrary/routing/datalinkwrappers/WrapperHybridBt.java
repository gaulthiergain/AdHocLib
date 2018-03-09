package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothDisabledException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice.AbstractRemoteDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice.RemoteBtDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ActiveConnections;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ListenerAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvAbstractException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class WrapperHybridBt extends WrapperBluetooth {

    private static final String TAG = "[AdHoc][WrapperWifiHy]";
    private HashMap<String, String> mapAddressLabel;
    private HashMap<String, NetworkObject> hashmapUuidNetwork;

    public WrapperHybridBt(boolean v, Context context, boolean secure, short nbThreads, short duration,
                           String ownAddress, ActiveConnections activeConnections, HashMap<String, String> mapAddressLabel,
                           ListenerAodv listenerAodv, ListenerDataLinkAodv listenerDataLinkAodv)
            throws DeviceException, BluetoothDisabledException, BluetoothBadDuration, IOException {
        super(v, context, secure, nbThreads, duration, ownAddress, activeConnections, listenerAodv, listenerDataLinkAodv);
        this.mapAddressLabel = mapAddressLabel;
        this.hashmapUuidNetwork = new HashMap<>();
    }

    public void connect() {
        for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapDevices.entrySet()) {
            if (!activeConnections.getActivesConnections().containsKey(entry.getValue().getShortUuid())) {
                //TODO remove
                /*if (ownName.equals("#eO91#SamsungGT3") && entry.getValue().getDevice().getName().equals("#e091#Samsung_gt")) {

                } else if (ownName.equals("#eO91#Samsung_gt") && entry.getValue().getDevice().getName().equals("#e091#SamsungGT3")) {

                } else {

                }*/
                _connect(entry.getValue());

            } else {
                if (v) Log.d(TAG, entry.getValue().getShortUuid() + " is already connected");
            }
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
                    public void onConnectionClosed(AbstractRemoteDevice remoteDevice) {

                        RemoteBtDevice remoteBtDevice = (RemoteBtDevice) remoteDevice;

                        // Get the remote UUID
                        String remoteUuid = remoteBtDevice.getDeviceAddress().
                                replace(":", "").toLowerCase();

                        if (v) Log.d(TAG, "Link broken with " + remoteUuid);

                        try {
                            listenerDataLinkAodv.brokenLink(remoteUuid);
                        } catch (IOException | NoConnectionException e) {
                            listenerAodv.catchException(e);
                        }

                        listenerAodv.onConnectionClosed(remoteDevice);
                    }

                    @Override
                    public void onConnection(AbstractRemoteDevice remoteDevice) {
                        listenerAodv.onConnection(remoteDevice);
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
                }
                break;
            case "CONNECT_CLIENT":
                NetworkObject networkObjectServer = hashmapUuidNetwork.get(message.getPdu().toString());
                if (networkObjectServer != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkObjectServer);
                    Log.d(TAG, "ADDED !!!");
                }
                break;
            default:
                // Handle messages in protocol scope
                listenerDataLinkAodv.processMsgReceived(message);
        }
    }

    /**
     * Method allowing to update the name of the device.
     */
    public void updateName(String name) {
        bluetoothManager.updateDeviceName(name);
    }
}
