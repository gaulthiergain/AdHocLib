package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothDisabledException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.MaxThreadReachedException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice.AbstractRemoteDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice.RemoteBtDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ActiveConnections;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DiscoveredDevice;
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

public class WrapperBluetooth extends AbstractWrapper {

    private static final String TAG = "[AdHoc][WrapperBt]";

    // Constants for taking only the last part of the UUID
    final static int LOW = 24;
    final static int END = 36;

    UUID ownUUID;
    String ownMac;
    boolean secure;
    String ownAddress;
    BluetoothManager bluetoothManager;
    BluetoothServiceServer bluetoothServiceServer;
    HashMap<String, BluetoothAdHocDevice> hashMapDevices;

    private WrapperBluetooth(boolean v, Context context, boolean secure, ActiveConnections activeConnections, ListenerAodv listenerAodv,
                             ListenerDataLinkAodv listenerDataLinkAodv) throws DeviceException {
        super(v, context, activeConnections, listenerAodv, listenerDataLinkAodv);
        this.secure = secure;
        this.bluetoothManager = new BluetoothManager(v, context);
        this.hashMapDevices = new HashMap<>();
        this.ownMac = BluetoothUtil.getCurrentMac(context);
    }

    public WrapperBluetooth(boolean v, Context context, boolean secure, short nbThreads, short duration, ActiveConnections activeConnections, ListenerAodv listenerAodv,
                            ListenerDataLinkAodv listenerDataLinkAodv) throws DeviceException, BluetoothDisabledException, BluetoothBadDuration, IOException {

        this(v, context, secure, activeConnections, listenerAodv, listenerDataLinkAodv);

        this.ownAddress = ownMac.replace(":", "").toLowerCase();
        this.ownUUID = UUID.fromString(BluetoothUtil.UUID + ownAddress);
        this.ownName = BluetoothUtil.getCurrentName();

        // callback
        listenerDataLinkAodv.getDeviceAddress(ownAddress);
        listenerDataLinkAodv.getDeviceName(ownName);

        // Check if the bluetooth adapter is enabled
        if (!bluetoothManager.isEnabled()) {
            if (!bluetoothManager.enable()) {
                throw new BluetoothDisabledException("Unable to enable Bluetooth adapter");
            }
            bluetoothManager.enableDiscovery(duration);
        }

        this.listenServer(nbThreads);
    }

    public WrapperBluetooth(boolean v, Context context, boolean secure, short nbThreads, short duration,
                            String ownAddress, ActiveConnections activeConnections, ListenerAodv listenerAodv,
                            ListenerDataLinkAodv listenerDataLinkAodv) throws DeviceException, BluetoothDisabledException, BluetoothBadDuration, IOException {

        this(v, context, secure, activeConnections, listenerAodv, listenerDataLinkAodv);

        this.ownAddress = ownAddress;
        this.ownUUID = UUID.fromString(BluetoothUtil.UUID + ownMac.replace(":", "").toLowerCase());

        // Check if the bluetooth adapter is enabled
        if (!bluetoothManager.isEnabled()) {
            if (!bluetoothManager.enable()) {
                throw new BluetoothDisabledException("Unable to enable Bluetooth adapter");
            }
            bluetoothManager.enableDiscovery(duration);
        }

        this.listenServer(nbThreads);
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
            public void onConnectionClosed(AbstractRemoteDevice remoteDevice) {

                RemoteBtDevice remoteBtDevice = (RemoteBtDevice) remoteDevice;

                // Get the remote UUID
                String remoteUuid = remoteBtDevice.getDeviceAddress()
                        .replace(":", "").toLowerCase();

                if (v) Log.d(TAG, "Link broken with " + remoteUuid);

                try {
                    listenerDataLinkAodv.brokenLink(remoteUuid);
                } catch (IOException e) {
                    listenerAodv.catchException(e);
                } catch (NoConnectionException e) {
                    listenerAodv.catchException(e);
                }

                listenerAodv.onConnectionClosed(remoteDevice);
            }

            @Override
            public void onConnection(AbstractRemoteDevice remoteDevice) {
                listenerAodv.onConnection(remoteDevice);
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

    /**
     * Method allowing to process received messages.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException              Signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException    Signals that a No Connection Exception exception has occurred.
     * @throws AodvUnknownTypeException Signals that a Unknown AODV type has been caught.
     */
    void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {
        Log.d(TAG, "Message rcvd " + message.toString());
        switch (message.getHeader().getType()) {
            case "CONNECT":
                NetworkObject networkObjectBt = bluetoothServiceServer.getActiveConnections().get(message.getPdu().toString());

                if (networkObjectBt != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection(message.getHeader().getSenderAddr(), networkObjectBt);
                }
                break;
            default:
                // Handle messages in protocol scope
                listenerDataLinkAodv.processMsgReceived(message);
        }
    }

    public void connect(DiscoveredDevice device) {

        String shortUuid = device.getAddress().replace(":", "").toLowerCase();
        BluetoothAdHocDevice btDevice = hashMapDevices.get(shortUuid);
        if (btDevice != null) {
            if (!activeConnections.getActivesConnections().containsKey(btDevice.getShortUuid())) {
                _connect(btDevice);
            } else {
                if (v) Log.d(TAG, btDevice.getShortUuid() + " is already connected");
            }
        } else {
            Log.d(TAG, "ERROR " + shortUuid);
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

                // Add the active connection into the autoConnectionActives object
                activeConnections.addConnection(uuid.toString().substring(LOW, END).toLowerCase(), network);

                // Send CONNECT message to establish the pairing
                bluetoothServiceClient.send(new MessageAdHoc(
                        new Header("CONNECT", ownAddress, ownName), ownMac));

            }
        });

        // Start the bluetoothServiceClient thread
        new Thread(bluetoothServiceClient).start();
    }

    @Override
    public void stopListening() throws IOException {
        bluetoothServiceServer.stopListening();
    }

    public void discovery() {
        bluetoothManager.discovery(new com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.DiscoveryListener() {
            @Override
            public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                HashMap<String, DiscoveredDevice> mapAddressDevice = new HashMap<>();

                // Add no paired devices into the hashMapDevices
                for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {

                    hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                    if (v) Log.d(TAG, "Add no paired " + entry.getValue().getShortUuid()
                            + " into hashMapDevices");
                    mapAddressDevice.put(entry.getValue().getDevice().getAddress(),
                            new DiscoveredDevice(entry.getValue().getDevice().getAddress(),
                                    entry.getValue().getDevice().getName(), DiscoveredDevice.BLUETOOTH));

                }
                listenerAodv.onDiscoveryCompleted(mapAddressDevice);
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
}
