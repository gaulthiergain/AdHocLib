package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.aodv.AodvManager;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.Data;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.TypeAodv;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoManager {

    //Helper
    public static final String ID_APP = "#e091#";
    private final static int LOW = 24;
    private final static int END = 36;
    private final static int NB_THREAD = 8;
    private final static int ATTEMPTS = 3;

    private final boolean v;
    private final boolean secure;
    private final Context context;
    private final String TAG = "[AdHoc][AutoManager]";

    private ListenerAodv listenerAodv;
    private ListenerDiscoveryGUI listenerDiscoveryGUI;

    private final String ownStringUUID;
    private final String ownName;
    private final String ownMac;

    private final AodvManager aodvManager;

    private BluetoothManager bluetoothManager;
    private HashMap<String, BluetoothAdHocDevice> hashMapDevices;

    private BluetoothServiceServer bluetoothServiceServer;


    /**
     * Constructor
     *
     * @param verbose      a boolean value to set the debug/verbose mode.
     * @param context      a Context object which gives global information about an application
     *                     environment.
     * @param ownUUID      a UUID object which represents the UUID of the current device.
     * @param listenerAodv a ListenerAodv object which serves as callback functions.
     * @param secure       a boolean value to determine if the connection is secure.
     * @throws IOException     Signals that an I/O exception of some sort has occurred.
     * @throws DeviceException Signals that a Bluetooth Device Exception exception has occurred.
     */
    public AutoManager(boolean verbose, Context context, UUID ownUUID, ListenerAodv listenerAodv,
                       boolean secure) throws IOException, DeviceException {

        this.bluetoothManager = new BluetoothManager(verbose, context);
        this.v = verbose;
        this.context = context;
        this.hashMapDevices = new HashMap<>();
        this.listenerAodv = listenerAodv;
        this.ownStringUUID = ownUUID.toString().substring(LOW, END); // Take only the last part (24-36) to optimize the process
        this.updateName();
        this.ownName = BluetoothUtil.getCurrentName();
        this.ownMac = BluetoothUtil.getCurrentMac(context);
        this.listenServer(ownUUID); // Listen on server threads
        this.secure = secure;
        this.aodvManager = new AodvManager(v, ownStringUUID, ownName, listenerAodv);
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
     *
     * @param duration an integer value between 0 and 3600 which represents the time of the
     *                 discovery mode.
     * @return a HashMap<String, BluetoothAdHocDevice> that maps the device's name with
     * BluetoothAdHocDevice object.
     * @throws BluetoothBadDuration Signals that a Bluetooth Bad Duration exception has occurred.
     */
    public HashMap<String, BluetoothAdHocDevice> getPaired(int duration) throws BluetoothBadDuration {
        // Check if Bluetooth is enabled
        if (!bluetoothManager.isEnabled()) {

            // If not, enable bluetooth and enable the discovery
            bluetoothManager.enable();
            bluetoothManager.enableDiscovery(duration);
        }

        // Add paired devices into the hashMapDevices
        for (Map.Entry<String, BluetoothAdHocDevice> entry : bluetoothManager.getPairedDevices().entrySet()) {
            if (entry.getValue().getDevice().getName().contains(AutoManager.ID_APP)) {
                hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                if (v) Log.d(TAG, "Add paired " + entry.getValue().getShortUuid()
                        + " into hashMapDevices");
            }
        }
        return bluetoothManager.getPairedDevices();
    }

    /**
     * Method allowing to discover other bluetooth devices.
     *
     * @param duration an integer value between 0 and 3600 which represents the time of
     *                 the discovery mode.
     * @throws BluetoothBadDuration Signals that a Bluetooth Bad Duration exception has occurred.
     */
    public void discovery(int duration) throws BluetoothBadDuration {

        // Check if Bluetooth is enabled
        if (!bluetoothManager.isEnabled()) {

            // If not, enable bluetooth and enable the discovery
            bluetoothManager.enable();
            bluetoothManager.enableDiscovery(duration);
        }

        // Start the discovery process
        bluetoothManager.discovery(new DiscoveryListener() {
            @Override
            public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                // Add no paired devices into the hashMapDevices
                for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
                    if (entry.getValue().getDevice().getName() != null &&
                            entry.getValue().getDevice().getName().contains(AutoManager.ID_APP)) {
                        hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                        if (v) Log.d(TAG, "Add no paired " + entry.getValue().getShortUuid()
                                + " into hashMapDevices");
                    }
                }
                // Stop and unregister to the discovery process
                bluetoothManager.unregisterDiscovery();

                // Execute onDiscoveryCompleted in the GUI
                if (listenerDiscoveryGUI != null) {
                    listenerDiscoveryGUI.onDiscoveryCompleted(hashMapBluetoothDevice);
                }
            }

            @Override
            public void onDiscoveryStarted() {
                // Execute onDiscoveryStarted in the GUI
                if (listenerDiscoveryGUI != null) {
                    listenerDiscoveryGUI.onDiscoveryStarted();
                }
            }

            @Override
            public void onDeviceFound(BluetoothDevice device) {
                // Execute onDeviceFound in the GUI
                if (listenerDiscoveryGUI != null) {
                    listenerDiscoveryGUI.onDeviceFound(device);
                }
            }

            @Override
            public void onScanModeChange(int currentMode, int oldMode) {
                // Execute onScanModeChange in the GUI
                if (listenerDiscoveryGUI != null) {
                    listenerDiscoveryGUI.onScanModeChange(currentMode, oldMode);
                }
            }
        });
    }


    /**
     * Method allowing to connect to remote bluetooth devices.
     */
    public void connect() {
        for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapDevices.entrySet()) {
            if (!aodvManager.getConnections().containsKey(entry.getValue().getShortUuid())) {
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
                            listenerAodv.clientIOException(e);
                        } catch (NoConnectionException e) {
                            listenerAodv.clientNoConnectionException(e);
                        } catch (AodvUnknownTypeException e) {
                            listenerAodv.clientAodvUnknownTypeException(e);
                        } catch (AodvUnknownDestException e) {
                            listenerAodv.clientAodvUnknownDestException(e);
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
                            aodvManager.removeRemoteConnection(remoteUuid);
                        } catch (IOException e) {
                            listenerAodv.clientIOException(e);
                        }
                    }

                    @Override
                    public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                        if (v)
                            Log.d(TAG, "Connected to server: " + deviceAddress + " - " + deviceName);

                        // Execute onConnection in the GUI
                        if (listenerDiscoveryGUI != null) {
                            listenerDiscoveryGUI.onConnection(deviceName, deviceAddress, localAddress);
                        }
                    }
                }, true, secure, ATTEMPTS, bluetoothAdHocDevice);

        bluetoothServiceClient.setListenerAutoConnect(new ListenerAutoConnect() {
            @Override
            public void connected(UUID uuid, NetworkObject network) throws IOException, NoConnectionException {

                // Add the active connection into the autoConnectionActives object
                aodvManager.addConnection(uuid.toString().substring(LOW, END).toLowerCase(), network);

                // Send CONNECT message to establish the pairing
                bluetoothServiceClient.send(new MessageAdHoc(
                        new Header("CONNECT", ownMac, ownName), ownStringUUID));

            }
        });

        // Start the bluetoothServiceClient thread
        new Thread(bluetoothServiceClient).start();
    }

    /**
     * Method allowing to stop the server listening threads.
     *
     * @throws IOException Signals that an I/O exception of some sort has occurred.
     */
    public void stopListening() throws IOException {
        bluetoothServiceServer.stopListening();
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
                    listenerAodv.serverIOException(e);
                } catch (NoConnectionException e) {
                    listenerAodv.serverNoConnectionException(e);
                } catch (AodvUnknownTypeException e) {
                    listenerAodv.serverAodvUnknownTypeException(e);
                } catch (AodvUnknownDestException e) {
                    listenerAodv.serverAodvUnknownDestException(e);
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
                    aodvManager.removeRemoteConnection(remoteUuid);
                } catch (IOException e) {
                    listenerAodv.serverIOException(e);
                }
            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                if (v) Log.d(TAG, "Connected to client: " + deviceAddress);

                // Execute onConnection in the GUI
                if (listenerDiscoveryGUI != null) {
                    listenerDiscoveryGUI.onConnection(deviceName, deviceAddress, localAddress);
                }

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
                    aodvManager.addConnection((String) message.getPdu(), networkObject);
                }
                break;
            default:
                // Handle messages in protocol scope
                aodvManager.processMsgReceived(message);
        }
    }

    /**
     * Method allowing to send a message to a remote address.
     *
     * @param address a String value which represents the destination address.
     * @param pdu     a Serializable value which represents the PDU of the message.
     * @throws IOException           Signals that an I/O exception of some sort has occurred.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     */
    public void sendMessage(String address, Serializable pdu) throws IOException, NoConnectionException {

        // Create MessageAdHoc object
        Header header = new Header(TypeAodv.DATA.getCode(), ownStringUUID, ownName);
        MessageAdHoc msg = new MessageAdHoc(header, new Data(address, pdu));

        // Send message to remote device
        aodvManager.send(msg, address);
    }

    /**
     * Method allowing to set the listenerDiscoveryGUI callback.
     *
     * @param listenerDiscoveryGUI a listenerDiscoveryGUI object that serves as callback.
     */
    public void setListenerDiscoveryGUI(ListenerDiscoveryGUI listenerDiscoveryGUI) {
        this.listenerDiscoveryGUI = listenerDiscoveryGUI;
    }
}
