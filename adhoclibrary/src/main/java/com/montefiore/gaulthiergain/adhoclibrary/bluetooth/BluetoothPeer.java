package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.auto.SmartBluetoothDevice;
import com.montefiore.gaulthiergain.adhoclibrary.auto.SmartDevice;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class BluetoothPeer extends BluetoothServiceClient {


    public BluetoothPeer(boolean verbose, Context context, MessageListener messageListener) {
        super(verbose, context, messageListener, false);
    }

    private void connectPeers(boolean secure, BluetoothDevice bluetoothDevice, UUID uuid) throws IOException {

        if (v) Log.d(TAG, "connect to: " + uuid);

        // Get a BluetoothSocket to connect with the given BluetoothDevice.
        BluetoothSocket bluetoothSocket;
        if (secure) {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
        } else {
            bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
        }

        // Connect to the remote host
        bluetoothSocket.connect();
        network = new NetworkObject(new AdHocSocketBluetooth(bluetoothSocket));
    }

    public void connect(final boolean secure, final BluetoothDevice bluetoothDevice,
                        final UUID uuid) throws NoConnectionException {


        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (v) Log.d(TAG, "CONNECT PEERS...");
                    connectPeers(secure, bluetoothDevice, uuid);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    public void listen(final int nbPeers, final boolean secure, final String name, final BluetoothAdapter mAdapter,
                       final UUID uuid) throws NoConnectionException, IOException {
        for (int i = 0; i < nbPeers; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (v) Log.d(TAG, "WAITING PEERS...");
                        listenPeers(secure, name, mAdapter, uuid);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void listenPeers(boolean secure, String name, BluetoothAdapter mAdapter, UUID uuid) throws IOException {
        BluetoothServerSocket serverSocket;
        if (secure) {
            serverSocket = mAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
        } else {
            serverSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord(name, uuid);
        }

        BluetoothSocket socket = serverSocket.accept();
        NetworkObject networkObject = new NetworkObject(new AdHocSocketBluetooth(socket));
        if (v) Log.d(TAG, "Accept peers: " + socket.getRemoteDevice());

        while (true) {
            try {
                MessageAdHoc messageAdHoc = (MessageAdHoc) networkObject.receiveObjectStream();
            } catch (IOException e) {
                Log.e(TAG, "disconnected", e);
                networkObject.closeConnection();
                break;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                Log.e(TAG, "disconnected", e);
                networkObject.closeConnection();
                break;
            }
        }
    }
}
