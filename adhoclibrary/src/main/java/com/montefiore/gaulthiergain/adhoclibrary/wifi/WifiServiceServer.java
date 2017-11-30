package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.threadPool.ListSocketDevice;
import com.montefiore.gaulthiergain.adhoclibrary.threadPool.ThreadServer;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.wifiListener.WifiMessageListener;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaulthiergain on 28/10/17.
 */

public class WifiServiceServer extends WifiService {

    private int port;
    private ThreadServer threadListen;

    public WifiServiceServer(Context context, boolean verbose, WifiMessageListener messageListener) {
        super(context, verbose, messageListener);
    }

    public void listen(int nbThreads, int port) throws IOException {
        if (v) Log.d(TAG, "Listening()");

        // Set the port
        this.port = port;

        // Cancel any thread currently running a connection
        if (threadListen != null) {
            threadListen.cancel();
            threadListen = null;
        }

        // Start thread Listening
        threadListen = new ThreadServer(handler, nbThreads, port, new ListSocketDevice());
        threadListen.start();

        // Update state
        setState(STATE_LISTENING);

        if (v) Log.d(TAG, "Listening on port: " + port);

    }

    public void sendto(MessageAdHoc msg, String address) throws NoConnectionException, IOException {
        if (v) Log.d(TAG, "sendto()");

        // Get remote connection
        ConcurrentHashMap<String, NetworkObject> hashMap = threadListen.getActiveConnexion();

        // Get associated socket
        NetworkObject network = hashMap.get(address);
        if (network == null) {
            throw new NoConnectionException("No remote connexion with " + address);
        } else {
            // Send message to connected device
            network.sendObjectStream(msg);
            if (v)
                Log.d(TAG, "Send " + msg + " to " + address);

            // Notify handler
            handler.obtainMessage(WifiService.MESSAGE_WRITE, msg).sendToTarget();
        }
    }

    public void broadcasttoAllExcept(MessageAdHoc msg, String senderAddr) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "broadcasttoAllExcept()");

        if (!_sendtoAllExcept(msg, senderAddr)) {
            throw new NoConnectionException("No remote connection");
        }

        // Notify handler
        handler.obtainMessage(WifiService.BROADCAST_WRITE, msg).sendToTarget();
    }

    public void broadcast(MessageAdHoc msg) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "broadcast()");

        if (!_sendtoAll(msg)) {
            throw new NoConnectionException("No remote connection");
        }

        // Notify handler
        handler.obtainMessage(WifiService.BROADCAST_WRITE, msg).sendToTarget();
    }

    public void sendtoAllExcept(MessageAdHoc msg, String senderAddr) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "sendtoAllExcept()");

        if (!_sendtoAllExcept(msg, senderAddr)) {
            throw new NoConnectionException("No remote connection");
        }

        // Notify handler
        handler.obtainMessage(WifiService.MESSAGE_WRITE, msg).sendToTarget();
    }

    public void sendtoAll(MessageAdHoc msg) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "sendtoAll()");

        if (!_sendtoAll(msg)) {
            throw new NoConnectionException("No remote connection");
        }

        // Notify handler
        handler.obtainMessage(WifiService.MESSAGE_WRITE, msg).sendToTarget();
    }

    private boolean _sendtoAll(MessageAdHoc msg) throws IOException, NoConnectionException {

        // Get remote connection
        ConcurrentHashMap<String, NetworkObject> hashMap = threadListen.getActiveConnexion();
        if (hashMap.size() == 0) {
            // If no remote device, return false
            return false;
        }

        // Send message to all connected devices
        for (Map.Entry<String, NetworkObject> pairs : hashMap.entrySet()) {
            pairs.getValue().sendObjectStream(msg);
            if (v) Log.d(TAG, "Send " + msg + " to " + pairs.getKey());
        }

        return true;
    }

    private boolean _sendtoAllExcept(MessageAdHoc msg, String senderAddr) throws NoConnectionException, IOException {

        // Get remote connection
        ConcurrentHashMap<String, NetworkObject> hashMap = threadListen.getActiveConnexion();

        if (hashMap.size() == 0) {
            // If no remote device, return false
            return false;
        }

        // Send message to all connected devices
        for (Map.Entry<String, NetworkObject> pairs : hashMap.entrySet()) {
            if (!pairs.getKey().equals(senderAddr)) {
                pairs.getValue().sendObjectStream(msg);
                if (v) Log.d(TAG, "Send " + msg + " to " + pairs.getKey());
            }
        }

        return true;
    }

    public void stopListening() throws IOException {

        if (threadListen != null) {
            if (v) Log.d(TAG, "Stop listening on device on port " + port);
            threadListen.cancel();
            setState(STATE_NONE);
        }
    }


}
