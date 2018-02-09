package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.aodv.Aodv;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.AodvManager;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.Data;
import com.montefiore.gaulthiergain.adhoclibrary.aodv.TypeAodv;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.DiscoveryListener;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class AutoManager {

    //Helper
    private final static int LOW = 24;
    private final static int END = 36;

    private final static int NB_THREAD = 8;
    private final static int ATTEMPTS = 3;

    private final boolean v;
    private final Context context;
    private final String TAG = "[AdHoc][AutoManager]";
    private ListenerGUI listenerGUI;

    private final String ownStringUUID;
    private final String ownName;
    private final String ownMac;


    private final AodvManager aodvManager;

    private BluetoothManager bluetoothManager;
    private HashMap<String, BluetoothAdHocDevice> hashMapDevices;

    private BluetoothServiceServer bluetoothServiceServer;


    public AutoManager(boolean v, Context context, UUID ownUUID) throws IOException, DeviceException {

        this.bluetoothManager = new BluetoothManager(true, context);
        this.v = v;
        this.context = context;
        this.hashMapDevices = new HashMap<>();
        this.ownStringUUID = ownUUID.toString().substring(LOW, END); // Take only the last part (24-36) to optimize the process
        this.updateName();
        this.ownName = BluetoothUtil.getCurrentName();
        this.ownMac = BluetoothUtil.getCurrentMac(context);
        this.listenServer(ownUUID); // Listen on server threads
        this.aodvManager = new AodvManager(v, ownStringUUID, ownName);
    }

    private void updateName() {
        //TODO update this
        //bluetoothManager.updateDeviceName(Code.ID_APP );
    }

    public void discovery(int duration) throws DeviceException, BluetoothBadDuration {

        // Create instance of Bluetooth Manager
        bluetoothManager = new BluetoothManager(true, context);

        // Check if Bluetooth is enabled
        if (!bluetoothManager.isEnabled()) {

            // If not, enable bluetooth and enable the discovery
            bluetoothManager.enable();
            bluetoothManager.enableDiscovery(duration);
        }

        // Add paired devices into the hashMapDevices
        for (Map.Entry<String, BluetoothAdHocDevice> entry : bluetoothManager.getPairedDevices().entrySet()) {
            if (entry.getValue().getDevice().getName().contains(Code.ID_APP)) {
                hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                if (v) Log.d(TAG, "Add paired " + entry.getValue().getShortUuid()
                        + " into hashMapDevices");
            }
        }

        // Start the discovery process
        bluetoothManager.discovery(new DiscoveryListener() {
            @Override
            public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                // Add no paired devices into the hashMapDevices
                for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
                    if (entry.getValue().getDevice().getName() != null &&
                            entry.getValue().getDevice().getName().contains(Code.ID_APP)) {
                        hashMapDevices.put(entry.getValue().getShortUuid(), entry.getValue());
                        if (v) Log.d(TAG, "Add no paired " + entry.getValue().getShortUuid()
                                + " into hashMapDevices");
                    }
                }
                // Stop and unregister to the discovery process
                bluetoothManager.unregisterDiscovery();

                // Execute onDiscoveryCompleted in the GUI
                if (listenerGUI != null) {
                    listenerGUI.onDiscoveryCompleted(hashMapBluetoothDevice);
                }
            }

            @Override
            public void onDiscoveryStarted() {
                // Execute onDiscoveryStarted in the GUI
                if (listenerGUI != null) {
                    listenerGUI.onDiscoveryStarted();
                }
            }

            @Override
            public void onDeviceFound(BluetoothDevice device) {
                // Execute onDeviceFound in the GUI
                if (listenerGUI != null) {
                    listenerGUI.onDeviceFound(device);
                }
            }

            @Override
            public void onScanModeChange(int currentMode, int oldMode) {
                // Execute onScanModeChange in the GUI
                if (listenerGUI != null) {
                    listenerGUI.onScanModeChange(currentMode, oldMode);
                }
            }
        });
    }

    public void connect() {
        for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapDevices.entrySet()) {
            if (!aodvManager.getConnections().containsKey(entry.getValue().getShortUuid())) {
                //TODO remove
                if (ownName.equals("#eO91#SamsungGT3") && entry.getValue().getDevice().getName().equals("#e091#Samsung_gt")) {

                } else {
                    _connect(entry.getValue());
                }
            } else {
                if (v) Log.d(TAG, entry.getValue().getShortUuid() + " is already connected");
            }
        }
    }

    private void _connect(final BluetoothAdHocDevice bluetoothAdHocDevice) {
        final BluetoothServiceClient bluetoothServiceClient = new BluetoothServiceClient(true, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoConnectionException e) {
                    e.printStackTrace();
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

                if (v) Log.d(TAG, "Link broken with " + deviceAddress);

                // Get the remote UUID
                String remoteUuid = deviceAddress.replace(":", "").toLowerCase();

                aodvManager.getRemoteConnections(remoteUuid);
            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                if (v) Log.d(TAG, "Connected to server: " + deviceAddress + " - " + deviceName);

                // Execute onConnection in the GUI
                if (listenerGUI != null) {
                    listenerGUI.onConnection(deviceName, deviceAddress, localAddress);
                }
            }
        }, true, true, ATTEMPTS, bluetoothAdHocDevice);

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

    public void stopListening() throws IOException {
        bluetoothServiceServer.stopListening();
    }

    private void listenServer(UUID ownUUID) throws IOException {
        bluetoothServiceServer = new BluetoothServiceServer(true, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                try {
                    processMsgReceived(message);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoConnectionException e) {
                    e.printStackTrace();
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
                if (v) Log.d(TAG, "onConnectionClosed");

                String remoteUuid = "";
                NetworkObject networkObject = bluetoothServiceServer.getActiveConnexion().get(deviceAddress);
                if (networkObject != null) {
                    for (Map.Entry<String, NetworkObject> entry : aodvManager.getConnections().entrySet()) {
                        if (entry.getValue().equals(networkObject)) {
                            remoteUuid = entry.getKey();
                            if (v) Log.d(TAG, "Link broken with " + remoteUuid);
                            break;
                        }
                    }

                    aodvManager.getRemoteConnections(remoteUuid);
                } else {
                    if (v) Log.e(TAG, "onConnectionClosed >>> Not Found");
                }
            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                if (v) Log.d(TAG, "Connected to client: " + deviceAddress);

                // Execute onConnection in the GUI
                if (listenerGUI != null) {
                    listenerGUI.onConnection(deviceName, deviceAddress, localAddress);
                }

            }
        });

        // Start the bluetoothServiceServer listening process
        bluetoothServiceServer.listen(NB_THREAD, true, "secure",
                BluetoothAdapter.getDefaultAdapter(), ownUUID);

    }

    private void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException {
        Log.d(TAG, "Message received: " + message.getPdu().toString());
        switch (message.getHeader().getType()) {
            case "CONNECT":
                NetworkObject networkObject = bluetoothServiceServer.getActiveConnexion().get(message.getHeader().getSenderAddr());
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

    public void sendMessage(String address, Serializable serializable) throws IOException, NoConnectionException {

        // Create MessageAdHoc object
        Header header = new Header(TypeAodv.DATA.getCode(), ownStringUUID, ownName);
        MessageAdHoc msg = new MessageAdHoc(header, new Data(address, serializable));

        // Send message to remote device
        aodvManager.send(msg, address);
    }

    public void setListenerGUI(ListenerGUI listenerGUI) {
        this.listenerGUI = listenerGUI;
    }
}
