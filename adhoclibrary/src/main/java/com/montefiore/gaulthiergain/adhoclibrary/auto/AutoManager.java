package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoManager {

    private final static int DURATION = 10;
    private final static int NB_THREAD = 8;
    private final static int ATTEMPTS = 3;

    private final boolean v;
    private final Context context;
    private final String TAG = "[AdHoc][AutoManager]";
    private ListenerGUI listenerGUI;

    private ListenerAutoConnect listenerAutoConnect;

    private final AutoConnectionActives autoConnectionActives;

    private BluetoothManager bluetoothManager;
    private HashMap<String, BluetoothAdHocDevice> hashMapDevices;

    private BluetoothServiceServer bluetoothServiceServer;


    public AutoManager(boolean v, Context context, UUID ownUUID) {
        this.v = v;
        this.context = context;
        try {
            this.bluetoothManager = new BluetoothManager(true, context);
        } catch (DeviceException e) {
            e.printStackTrace();
        }
        this.autoConnectionActives = new AutoConnectionActives();
        this.listenServer(ownUUID);
    }

    public void discovery() {
        try {
            bluetoothManager = new BluetoothManager(true, context);
        } catch (DeviceException e) {
            e.printStackTrace();
        }

        if (!bluetoothManager.isEnabled()) {

            // Enable bluetooth and enable the discovery
            try {
                bluetoothManager.enable();
                bluetoothManager.enableDiscovery(DURATION);
            } catch (BluetoothBadDuration bluetoothBadDuration) {
                bluetoothBadDuration.printStackTrace();
            }
        }

        hashMapDevices = bluetoothManager.getPairedDevices();
        // Start Discovery
        bluetoothManager.discovery(new DiscoveryListener() {
            @Override
            public void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapBluetoothDevice.entrySet()) {
                    if (entry.getValue().getDevice().getName().contains(Code.ID_APP)) {
                        hashMapDevices.put(entry.getValue().getUuid(), entry.getValue());
                        if (v) Log.d(TAG, "Add " + entry.getValue().getUuid() + " into Hashmap");
                    }
                }
                bluetoothManager.unregisterDiscovery();
                if (listenerGUI != null) {
                    // Listener on GUI
                    listenerGUI.onDiscoveryCompleted(hashMapBluetoothDevice);
                }
            }

            @Override
            public void onDiscoveryStarted() {

            }

            @Override
            public void onDeviceFound(BluetoothDevice device) {
                Toast.makeText(context, "New devices found: " + device.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onScanModeChange(int currentMode, int oldMode) {

            }
        });
    }

    public void connect() {
        for (Map.Entry<String, BluetoothAdHocDevice> entry : hashMapDevices.entrySet()) {

            if (!autoConnectionActives.getActivesConnections().containsKey(entry.getValue().getUuid())) {
                _connect(entry.getValue());
            } else {
                if (v) Log.d(TAG, entry.getValue().getUuid() + " is already connected");
            }
        }
    }

    private void _connect(BluetoothAdHocDevice bluetoothAdHocDevice) {
        BluetoothServiceClient bluetoothServiceClient = new BluetoothServiceClient(true, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                processMsgReceived(message);
            }

            @Override
            public void onMessageSent(MessageAdHoc message) {
                Log.d(TAG, "Message sent: " + message.getPdu().toString());
            }

            @Override
            public void onForward(MessageAdHoc message) {
                Log.d(TAG, "OnForward: " + message.getPdu().toString());
            }

            @Override
            public void onConnectionClosed(String deviceName, String deviceAddress) {

                Log.d(TAG, "Link broken with " + deviceAddress);

                //remove remote connections
                /*
                if (autoConnectionActives.getActivesConnections().containsKey(remoteUuid)) {
                    Log.d(TAG, "Remove active connection with " + remoteUuid.substring(LOW, END));
                    NetworkObject networkObject = autoConnectionActives.getActivesConnections().get(remoteUuid);
                    autoConnectionActives.getActivesConnections().remove(remoteUuid);
                    networkObject.closeConnection();
                    networkObject = null;
                }

                if (aodv.getRoutingTable().containsDest(remoteUuid)) {
                    Log.traceAodv(TAG, "Remove " + remoteUuid.substring(LOW, END) + " entry from RIB");
                    aodv.getRoutingTable().removeEntry(remoteUuid);
                }

                if (aodv.getRoutingTable().getRoutingTable().size() > 0) {
                    Log.traceAodv(TAG, "Send RRER ");
                    sendRRER(remoteUuid);
                }*/
            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                Log.d(TAG, "Connected to server: " + deviceAddress + " - " + deviceName);
            }
        }, true, true, ATTEMPTS, bluetoothAdHocDevice);

        bluetoothServiceClient.setListenerAutoConnect(new ListenerAutoConnect() {
            @Override
            public void connected(UUID uuid, NetworkObject network) {
                autoConnectionActives.addConnection(uuid.toString(), network);
            }
        });
        new Thread(bluetoothServiceClient).start();
        try {
            Thread.sleep(3000); //TODO remove
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
            /*bluetoothServiceClient.send(new MessageAdHoc(
                    new Header("CONNECT", "/" + selfDevice.getAddr() + ":" + localPort, ownUUID.toString())
                    , ""));*/
        // add network client



    }


    public void stopListening() throws IOException {
        bluetoothServiceServer.stopListening();
    }

    private void listenServer(UUID ownUUID) {
        bluetoothServiceServer = new BluetoothServiceServer(true, context, new MessageListener() {
            @Override
            public void onMessageReceived(MessageAdHoc message) {
                processMsgReceived(message);
            }

            @Override
            public void onMessageSent(MessageAdHoc message) {
                Log.d(TAG, "Message sent: " + message.getPdu().toString());
            }

            @Override
            public void onForward(MessageAdHoc message) {
                Log.d(TAG, "OnForward: " + message.getPdu().toString());
            }

            @Override
            public void onConnectionClosed(String deviceName, String deviceAddress) {
                Log.d(TAG, "onConnectionClosed");
                String remoteUuid = "";
                NetworkObject networkObject = bluetoothServiceServer.getActiveConnexion().get(deviceAddress);
                if (networkObject != null) {
                    for (Map.Entry<String, NetworkObject> entry : autoConnectionActives.getActivesConnections().entrySet()) {
                        if (entry.getValue().equals(networkObject)) {
                            remoteUuid = entry.getKey();
                            Log.d(TAG, "Link broken with " + remoteUuid);
                        }
                    }
                    //remove remote connections
                    if (autoConnectionActives.getActivesConnections().containsKey(remoteUuid)) {
                        Log.d(TAG, "Remote active connection with " + remoteUuid);
                        autoConnectionActives.getActivesConnections().remove(remoteUuid);
                    }

                    /*if (aodv.getRoutingTable().containsDest(remoteUuid)) {
                        Log.traceAodv(TAG, "Remote " + remoteUuid.substring(LOW, END) + " from RIB");
                        aodv.getRoutingTable().removeEntry(remoteUuid);
                    }

                    if (aodv.getRoutingTable().getRoutingTable().size() > 0) {
                        Log.traceAodv(TAG, "Send RRER ");
                        sendRRER(remoteUuid);
                    }*/

                    networkObject.closeConnection();
                    networkObject = null;
                } else {
                    Log.e(TAG, "onConnectionClosed >>> Not Found");
                }

            }

            @Override
            public void onConnection(String deviceName, String deviceAddress, String localAddress) {
                Log.d(TAG, "Connected to client: " + deviceAddress);
            }
        });
        try {
            bluetoothServiceServer.listen(NB_THREAD, true, "secure",
                    BluetoothAdapter.getDefaultAdapter(), ownUUID);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processMsgReceived(MessageAdHoc message) {
    }

    public void setListenerGUI(ListenerGUI listenerGUI) {
        this.listenerGUI = listenerGUI;
    }
}