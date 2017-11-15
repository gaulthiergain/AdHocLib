package com.montefiore.gaulthiergain.adhoclib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclib.network.BluetoothNetwork;
import com.montefiore.gaulthiergain.adhoclib.threadPool.ListSocketDevice;
import com.montefiore.gaulthiergain.adhoclib.threadPool.ThreadServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaulthiergain on 28/10/17.
 */


public class BluetoothServiceServer extends BluetoothService {

    private UUID uuid;
    private ThreadServer threadListen;
    private HashMap<String, BluetoothAdHocDevice> arrayConnectedDevice;

    public BluetoothServiceServer(Context context, boolean verbose) {
        super(context, verbose);
        arrayConnectedDevice = new HashMap<>();
    }

    public void listen(int nbThreads, boolean secure, String name, BluetoothAdapter bluetoothAdapter,
                       UUID uuid) throws IOException {

        if (v) Log.d(TAG, "Listening on device: " + uuid.toString());

        setState(STATE_LISTENING);

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

    }

    public void sendto(BluetoothAdHocDevice device){

    }

    public void sendtoAll(){
        ConcurrentHashMap<String, BluetoothNetwork> hashMap = threadListen.getActiveConnexion();
        // Iterating over values only
        for (String value : hashMap.keySet()) {
            System.out.println("Value = " + value);
        }

        for (BluetoothNetwork network : hashMap.values()) {
            try {
                network.send("----> ---> test" + network.getSocket().getRemoteDevice().toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
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
