package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothDisabledException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.AodvManager;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.Data;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.TypeAodv;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataLinkBtManager implements IDataLink {

    //Helper
    public static final String ID_APP = "#e091#";
    private final static int LOW = 24;
    private final static int END = 36;
    private final static int NB_THREAD = 8;
    private final static int ATTEMPTS = 3;

    private final boolean v;
    private final boolean secure;
    private final Context context;
    private final String TAG = "[AdHoc][DataLinkBt]";

    private ListenerAodv listenerAodv;
    private ListenerDataLinkAodv listenerDataLinkAodv;

    private final String ownStringUUID;
    private final String ownName;
    private final String ownMac;

    private final ActiveConnections activeConnections;
    private final BluetoothManager bluetoothManager;
    private HashMap<String, BluetoothAdHocDevice> hashMapDevices;

    private BluetoothServiceServer bluetoothServiceServer;


    /**
     * Constructor
     *
     * @param verbose              a boolean value to set the debug/verbose mode.
     * @param context              a Context object which gives global information about an
     *                             application environment.
     * @param ownUUID              a UUID object which represents the UUID of the current device.
     * @param ownName              a String value which represents the name of the current device.
     * @param secure               a boolean value to determine if the connection is secure.
     * @param listenerAodv         a ListenerAodv object which serves as callback functions.
     * @param listenerDataLinkAodv a listenerDataLinkAodv object which serves as callback functions.
     * @throws IOException     Signals that an I/O exception of some sort has occurred.
     * @throws DeviceException Signals that a Bluetooth Device Exception exception has occurred.
     */
    public DataLinkBtManager(boolean verbose, Context context, UUID ownUUID, String ownName, boolean secure,
                             ListenerAodv listenerAodv, ListenerDataLinkAodv listenerDataLinkAodv) throws IOException, DeviceException {


        this.v = verbose;
        this.context = context;
        this.bluetoothManager = new BluetoothManager(verbose, context);
        this.listenerAodv = listenerAodv;
        this.listenerDataLinkAodv = listenerDataLinkAodv;
        this.ownStringUUID = ownUUID.toString().substring(LOW, END); // Take only the last part
        // (24-36) to optimize the process
        this.ownName = ownName;
        this.ownMac = BluetoothUtil.getCurrentMac(context);

        this.hashMapDevices = new HashMap<>();
        this.activeConnections = new ActiveConnections();

        this.updateName();
        this.listenServer(ownUUID); // Listen on server threads
        this.secure = secure;

        //TODO check if bluetooth is active and set duration here
    }

    /**
     * Method allowing to update the name of the device.
     */
    private void updateName() {
        //TODO update this
        //bluetoothManager.updateDeviceName(Code.ID_APP );
    }

    /**
     * Method allowing to get all the paired Bluetooth devices.
     */
    public void getPaired() {

        // Add paired devices into the hashMapDevices
        for (Map.Entry<String, BluetoothAdHocDevice> entry : bluetoothManager.getPairedDevices().entrySet()) {
            if (entry.getValue().getDevice().getName().contains(DataLinkBtManager.ID_APP)) {
                hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                if (v) Log.d(TAG, "Add paired " + entry.getValue().getShortUuid()
                        + " into hashMapDevices");
            }
        }
    }

    /**
     * Method allowing to discover other bluetooth devices.
     */
    public void discovery() {

        // Start the discovery process
        bluetoothManager.discovery(new DiscoveryListener() {
            @Override
            public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                // Add no paired devices into the hashMapDevices
                for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
                    if (entry.getValue().getDevice().getName() != null &&
                            entry.getValue().getDevice().getName().contains(DataLinkBtManager.ID_APP)) {
                        hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                        if (v) Log.d(TAG, "Add no paired " + entry.getValue().getShortUuid()
                                + " into hashMapDevices");
                    }
                }
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
     * Method allowing to connect to remote bluetooth devices.
     */
    public void connect() {
        for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapDevices.entrySet()) {
            if (!activeConnections.getActivesConnections().containsKey(entry.getValue().getShortUuid())) {
                //TODO remove
                if (ownName.equals("#eO91#SamsungGT3") && entry.getValue().getDevice().getName().equals("#e091#Samsung_gt")) {

                } else if (ownName.equals("#eO91#Samsung_gt") && entry.getValue().getDevice().getName().equals("#e091#SamsungGT3")) {

                } else {
                    _connect(entry.getValue());
                }
            } else {
                if (v) Log.d(TAG, entry.getValue().getShortUuid() + " is already connected");
            }
        }
    }

    /**
     * Method allowing to connect to a remote bluetooth device.
     *
     * @param bluetoothAdHocDevice a BluetoothAdHocDevice object which represents a remote Bluetooth
     *                             device.
     */
    private void _connect(final BluetoothAdHocDevice bluetoothAdHocDevice) {
        final BluetoothServiceClient bluetoothServiceClient = new BluetoothServiceClient(v, context,
                new MessageListener() {
                    @Override
                    public void onMessageReceived(MessageAdHoc message) {
                        try {
                            processMsgReceived(message);
                        } catch (IOException e) {
                            listenerAodv.IOException(e);
                        } catch (NoConnectionException e) {
                            listenerAodv.NoConnectionException(e);
                        } catch (AodvUnknownTypeException e) {
                            listenerAodv.AodvUnknownTypeException(e);
                        } catch (AodvUnknownDestException e) {
                            listenerAodv.AodvUnknownDestException(e);
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
                    public void onConnectionClosed(String deviceName, String deviceAddress) {

                        // Get the remote UUID
                        String remoteUuid = deviceAddress.replace(":", "").toLowerCase();

                        if (v) Log.d(TAG, "Link broken with " + remoteUuid);

                        try {
                            listenerDataLinkAodv.brokenLink(remoteUuid);
                        } catch (IOException e) {
                            listenerAodv.IOException(e);
                        } catch (NoConnectionException e) {
                            listenerAodv.NoConnectionException(e);
                        }

                    }

                    @Override
                    public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                        if (v)
                            Log.d(TAG, "Connected to server: " + deviceAddress + " - " + deviceName);
                    }
                }, true, secure, ATTEMPTS, bluetoothAdHocDevice);

        bluetoothServiceClient.setListenerAutoConnect(new ListenerAutoConnect() {
            @Override
            public void connected(UUID uuid, NetworkObject network) throws IOException, NoConnectionException {

                // Add the active connection into the autoConnectionActives object
                activeConnections.addConnection(uuid.toString().substring(LOW, END).toLowerCase(), network);

                // Send CONNECT message to establish the pairing
                bluetoothServiceClient.send(new MessageAdHoc(
                        new Header("CONNECT", ownMac, ownName), ownStringUUID));

            }
        });

        // Start the bluetoothServiceClient thread
        new Thread(bluetoothServiceClient).start();
    }

    /**
     * Method allowing to listen for incoming bluetooth connections.
     *
     * @param ownUUID an UUID object which identify the physical device.
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    private void listenServer(UUID ownUUID) throws IOException {
        bluetoothServiceServer = new BluetoothServiceServer(v, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException e) {
                    listenerAodv.IOException(e);
                } catch (NoConnectionException e) {
                    listenerAodv.NoConnectionException(e);
                } catch (AodvUnknownTypeException e) {
                    listenerAodv.AodvUnknownTypeException(e);
                } catch (AodvUnknownDestException e) {
                    listenerAodv.AodvUnknownDestException(e);
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
            public void onConnectionClosed(String deviceName, String deviceAddress) {

                // Get the remote UUID
                String remoteUuid = deviceAddress.replace(":", "").toLowerCase();

                if (v) Log.d(TAG, "Link broken with " + remoteUuid);

                try {
                    listenerDataLinkAodv.brokenLink(remoteUuid);
                } catch (IOException e) {
                    listenerAodv.IOException(e);
                } catch (NoConnectionException e) {
                    listenerAodv.NoConnectionException(e);
                }
            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                if (v) Log.d(TAG, "Connected to client: " + deviceAddress);
            }
        });

        // Start the bluetoothServiceServer listening process
        bluetoothServiceServer.listen(NB_THREAD, secure, "secure",
                BluetoothAdapter.getDefaultAdapter(), ownUUID);

    }

    /**
     * Method allowing to process received messages.
     *
     * @param message a MessageAdHoc object which represents a message exchanged between nodes.
     * @throws IOException              Signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException    Signals that a No Connection Exception exception has occurred.
     * @throws AodvUnknownTypeException Signals that a Unknown AODV type has been caught.
     */
    private void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException {
        Log.d(TAG, "Message received: " + message.getPdu().toString());
        switch (message.getHeader().getType()) {
            case "CONNECT":
                NetworkObject networkObject = bluetoothServiceServer.getActiveConnections().get(message.getHeader().getSenderAddr());
                if (networkObject != null) {
                    // Add the active connection into the autoConnectionActives object
                    activeConnections.addConnection((String) message.getPdu(), networkObject);
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
    public void sendMessage(MessageAdHoc message, String address) throws IOException {

        NetworkObject networkObject = activeConnections.getActivesConnections().get(address);
        networkObject.sendObjectStream(message);
        if (v) Log.d(TAG, "Send directly to " + address);
    }

    @Override
    public boolean isDirectNeighbors(String address) {
        return activeConnections.getActivesConnections().containsKey(address);
    }

    @Override
    public void broadcastExcept(String originateAddr, MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : activeConnections.getActivesConnections().entrySet()) {
            if (!entry.getKey().equals(originateAddr)) {
                entry.getValue().sendObjectStream(message);
                if (v)
                    Log.d(TAG, "Broadcast Message to " + entry.getKey());
            }
        }
    }

    @Override
    public void broadcast(MessageAdHoc message) throws IOException {
        for (Map.Entry<String, NetworkObject> entry : activeConnections.getActivesConnections().entrySet()) {
            entry.getValue().sendObjectStream(message);
            if (v)
                Log.d(TAG, "Broadcast Message to " + entry.getKey());
        }
    }
}
