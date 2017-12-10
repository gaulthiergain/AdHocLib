package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.bluetooth.BluetoothAdapter;
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


    public BluetoothPeer(boolean verbose, Context context, MessageListener messageListener) {
        super(verbose, context, messageListener, false);
    }

    public void listen(final int nbPeers, final boolean secure, final String name, final BluetoothAdapter mAdapter,
                          final UUID uuid) throws NoConnectionException, IOException {
        for (int i = 0; i < nbPeers; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(v) Log.d(TAG, "WAITING PEERS...");
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
