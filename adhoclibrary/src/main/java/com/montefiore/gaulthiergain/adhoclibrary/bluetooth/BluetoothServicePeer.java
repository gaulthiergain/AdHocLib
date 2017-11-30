package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetoothListener.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;

import java.io.IOException;
import java.util.UUID;

public class BluetoothServicePeer extends BluetoothServiceClient {

    private BluetoothServerSocket serverSocket;
    private BluetoothAdapter bluetoothAdapter;
    private final boolean secure;
    private final UUID device_uuid;

    public BluetoothServicePeer(Context context, boolean verbose, MessageListener messageListener,
                                boolean secure, BluetoothAdapter bluetoothAdapter, UUID device_uuid, boolean background) throws IOException {
        super(context, verbose, background, messageListener);
        this.secure = secure;
        this.bluetoothAdapter = bluetoothAdapter;
        this.device_uuid = device_uuid;
    }

    private void waitPeers() throws IOException {
        String name = secure ? "Secure" : "Insecure";

        if (secure) {
            this.serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(name, device_uuid);
        } else {
            this.serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(name, device_uuid);
        }

        BluetoothSocket bluetoothSocket = serverSocket.accept();
        if (bluetoothSocket != null) {
            Log.d(TAG, bluetoothSocket.getRemoteDevice().getAddress() + " accepted");
            bluetoothNetwork = new NetworkObject(new AdHocSocketBluetooth(bluetoothSocket));
            setState(STATE_LISTENING);
        } else {
            Log.d(TAG, "Error while accepting client");
        }
    }

    private void listenInBackground() throws NoConnectionException, IOException {
        if (v) Log.d(TAG, "listenInBackground()");

        this.waitPeers();

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        }

        // Cancel any thread currently running a connection
        if (threadListening != null) {
            threadListening.cancel();
            threadListening = null;
        }

        setState(STATE_LISTENING_CONNECTED);

        // Start the thread to connect with the given device
        threadListening = new BluetoothListenThread(bluetoothNetwork, handler);
        threadListening.start();
    }

    public void disconnect() throws NoConnectionException {
        if (v) Log.d(TAG, "disconnect()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");

        }else{
            bluetoothNetwork.closeConnection();
            stopListeningInBackground();
            setState(STATE_NONE);
        }
    }

}
