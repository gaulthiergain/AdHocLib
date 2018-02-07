package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.UUID;


public class BluetoothPeer extends BluetoothServiceClient {


    private BluetoothServerSocket serverSocket;

    public BluetoothPeer(boolean verbose, Context context, MessageListener messageListener) {
        super(verbose, context, messageListener, false, false, 3, null);
        //TODO UPDATE
    }

    private void connectPeers(boolean secure, BluetoothDevice bluetoothDevice, UUID uuid) throws IOException {


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
        if (v) Log.d(TAG, "connect to: " + uuid);
    }

    public void connect(final boolean secure, final BluetoothDevice bluetoothDevice,
                        final UUID uuid) throws NoConnectionException {


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connectPeers(secure, bluetoothDevice, uuid);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        if (v) Log.d(TAG, "CONNECTION TO PEERS on thread: " + t.getName());
        t.start();

    }

    public void listen(final int nbPeers, final boolean secure, final String name, final BluetoothAdapter mAdapter,
                       final UUID uuid) throws NoConnectionException, IOException {


        for (int i = 0; i < nbPeers; i++) {
            if (secure) {
                serverSocket = mAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
            } else {
                serverSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord(name, uuid);
            }

            if (v) Log.d(TAG, "WAITING PEERS on thread: " + Thread.currentThread().getName());
            final BluetoothSocket socket = serverSocket.accept();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        listenPeers(socket, secure, name + Thread.currentThread().getName(), mAdapter, uuid);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void listenPeers(BluetoothSocket socket, boolean secure, String name, BluetoothAdapter mAdapter, UUID uuid) throws IOException {


        if (v) Log.d(TAG, "Accept peers: " + socket.getRemoteDevice());
        NetworkObject networkObject = new NetworkObject(new AdHocSocketBluetooth(socket));

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
