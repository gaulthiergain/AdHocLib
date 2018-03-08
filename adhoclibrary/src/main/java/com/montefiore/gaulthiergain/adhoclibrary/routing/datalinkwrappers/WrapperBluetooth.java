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
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DataLinkBtManager;
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

    private UUID ownUUID;
    boolean secure;
    String ownAddress;
    String ownMac;
    private BluetoothManager bluetoothManager;
    BluetoothServiceServer bluetoothServiceServer;
    private HashMap<String, BluetoothAdHocDevice> hashMapDevices;

    public WrapperBluetooth(boolean v, Context context, boolean secure, short nbThreads, short duration, ActiveConnections activeConnections, ListenerAodv listenerAodv,
                            ListenerDataLinkAodv listenerDataLinkAodv) throws DeviceException, BluetoothDisabledException, BluetoothBadDuration, IOException {


        super(v, context, activeConnections, listenerAodv, listenerDataLinkAodv);
        this.secure = secure;
        this.bluetoothManager = new BluetoothManager(v, context);
        this.hashMapDevices = new HashMap<>();
        this.ownMac = BluetoothUtil.getCurrentMac(context);
        this.ownAddress = ownMac.replace(":", "").toLowerCase();
        this.ownUUID = UUID.fromString(BluetoothUtil.UUID + ownAddress);

        this.updateName(); //must return new name
        this.ownName = BluetoothUtil.getCurrentName(); // todo update with updateName

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
        super(v, context, activeConnections, listenerAodv, listenerDataLinkAodv);
        this.secure = secure;
        this.bluetoothManager = new BluetoothManager(v, context);
        this.hashMapDevices = new HashMap<>();
        this.ownMac = BluetoothUtil.getCurrentMac(context);
        this.ownAddress = ownAddress;
        this.ownUUID = UUID.fromString(BluetoothUtil.UUID + ownAddress);

        // Check if the bluetooth adapter is enabled
        if (!bluetoothManager.isEnabled()) {
            if (!bluetoothManager.enable()) {
                throw new BluetoothDisabledException("Unable to enable Bluetooth adapter");
            }
            bluetoothManager.enableDiscovery(duration);
        }

        this.listenServer(nbThreads);
    }

    /**
     * Method allowing to update the name of the device.
     */
    private void updateName() {
        //TODO update this
        //bluetoothManager.updateDeviceName(Code.ID_APP );
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
                // Add no paired devices into the hashMapDevices
                for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
                    if (entry.getValue().getDevice().getName() != null &&
                            entry.getValue().getDevice().getName().contains(AbstractWrapper.ID_APP)) {
                        hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                        if (v) Log.d(TAG, "Add no paired " + entry.getValue().getShortUuid()
                                + " into hashMapDevices");
                    }
                }
                listenerAodv.onDiscoveryCompleted();
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
            if (entry.getValue().getDevice().getName().contains(AbstractWrapper.ID_APP)) {
                hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                if (v) Log.d(TAG, "Add paired " + entry.getValue().getShortUuid()
                        + " into hashMapDevices");
            }
        }

        listenerAodv.onPairedCompleted();
    }
}
