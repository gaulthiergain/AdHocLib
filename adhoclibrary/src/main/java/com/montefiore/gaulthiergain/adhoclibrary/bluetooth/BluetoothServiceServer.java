package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetoothListener.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.BluetoothNetwork;
import com.montefiore.gaulthiergain.adhoclibrary.threadPool.ListSocketDevice;
import com.montefiore.gaulthiergain.adhoclibrary.threadPool.ThreadServer;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaulthiergain on 28/10/17.
 */

public class BluetoothServiceServer extends BluetoothService {

    private UUID uuid;
    private ThreadServer threadListen;

    public BluetoothServiceServer(Context context, boolean verbose, MessageListener messageListener) {
        super(context, verbose, messageListener);
    }

    public void listen(int nbThreads, boolean secure, String name, BluetoothAdapter bluetoothAdapter,
                       UUID uuid) throws IOException {
        if (v) Log.d(TAG, "Listening()");

        // Cancel any thread currently running a connection
        if (threadListen != null) {
            threadListen.cancel();
            threadListen = null;
        }

        // Save the uuid
        this.uuid = uuid;

        // Start thread Listening
        threadListen = new ThreadServer(handler, nbThreads, secure, name, bluetoothAdapter, uuid,
                new ListSocketDevice());
        threadListen.start();

        // Update state
        setState(STATE_LISTENING);

        if (v) Log.d(TAG, "Listening on device: " + uuid.toString());

    }

    public void sendto(String msg, BluetoothAdHocDevice bluetoothAdHocDevice) throws NoConnectionException, IOException {
        if (v) Log.d(TAG, "sendto()");

        // Get remote connection
        ConcurrentHashMap<String, BluetoothNetwork> hashMap = threadListen.getActiveConnexion();

        // Get associated socket
        BluetoothNetwork network = hashMap.get(bluetoothAdHocDevice.getDevice().getAddress());
        if (network == null) {
            throw new NoConnectionException("No remote connexion with " + bluetoothAdHocDevice.toString());
        } else {
            // Send message to connected device
            network.send(msg);
            if (v) Log.d(TAG, "Send " + msg + " to " + bluetoothAdHocDevice.getDevice().getAddress());

            // Notify handler
            handler.obtainMessage(BluetoothService.MESSAGE_WRITE, msg).sendToTarget();
        }
    }

    public void sendtoAll(String msg) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "sendtoAll()");

        // Get remote connection
        ConcurrentHashMap<String, BluetoothNetwork> hashMap = threadListen.getActiveConnexion();

        if (hashMap.size() == 0) {
            throw new NoConnectionException("No remote connection");
        } else {

            // Send message to all connected devices
            for (Map.Entry<String, BluetoothNetwork> pairs : hashMap.entrySet()) {
                pairs.getValue().send(msg);
                if (v) Log.d(TAG, "Send " + msg + " to " + pairs.getKey());
            }

            // Notify handler
            handler.obtainMessage(BluetoothService.MESSAGE_WRITE, msg).sendToTarget();
        }
    }

    public void stopListening() throws IOException {

        if (threadListen != null) {
            if (v) Log.d(TAG, "Stop listening on device: " + uuid.toString());
            threadListen.cancel();
            setState(STATE_NONE);
        }
    }
}
