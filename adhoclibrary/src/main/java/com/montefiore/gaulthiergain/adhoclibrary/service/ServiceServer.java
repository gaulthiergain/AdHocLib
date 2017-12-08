package com.montefiore.gaulthiergain.adhoclibrary.service;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.threadPool.ThreadServer;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaulthiergain on 8/12/17.
 */

public abstract class ServiceServer extends Service {

    protected ThreadServer threadListen;

    public ServiceServer(boolean verbose, Context context, MessageListener messageListener) {
        super(verbose, context, messageListener);
    }

    public void stopListening() throws IOException {

        if (threadListen != null) {
            if (v) Log.d(TAG, "Stop listening");
            threadListen.cancel();
            setState(STATE_NONE);
        }
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
            handler.obtainMessage(Service.MESSAGE_WRITE, msg).sendToTarget();
        }
    }

    public void broadcasttoAllExcept(MessageAdHoc msg, String senderAddr) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "broadcasttoAllExcept()");

        if (!_sendtoAllExcept(msg, senderAddr)) {
            throw new NoConnectionException("No remote connection");
        }

        // Notify handler
        handler.obtainMessage(Service.BROADCAST_WRITE, msg).sendToTarget();
    }

    public void broadcast(MessageAdHoc msg) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "broadcast()");

        if (!_sendtoAll(msg)) {
            throw new NoConnectionException("No remote connection");
        }

        // Notify handler
        handler.obtainMessage(Service.BROADCAST_WRITE, msg).sendToTarget();
    }

    public void sendtoAllExcept(MessageAdHoc msg, String senderAddr) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "sendtoAllExcept()");

        if (!_sendtoAllExcept(msg, senderAddr)) {
            throw new NoConnectionException("No remote connection");
        }

        // Notify handler
        handler.obtainMessage(Service.MESSAGE_WRITE, msg).sendToTarget();
    }

    public void sendtoAll(MessageAdHoc msg) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "sendtoAll()");

        if (!_sendtoAll(msg)) {
            throw new NoConnectionException("No remote connection");
        }

        // Notify handler
        handler.obtainMessage(Service.MESSAGE_WRITE, msg).sendToTarget();
    }
}
