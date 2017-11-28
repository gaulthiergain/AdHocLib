package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothListenThread;
import com.montefiore.gaulthiergain.adhoclibrary.bluetoothListener.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.BluetoothNetwork;
import com.montefiore.gaulthiergain.adhoclibrary.network.WifiNetwork;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.wifiListener.WifiMessageListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;

/**
 * Created by gaulthiergain on 28/10/17.
 */

public class WifiServiceClient extends WifiService implements Runnable {

    protected WifiNetwork wifiNetwork;
    protected WifiListenThread threadListening;
    private final int port;
    private final String remoteAddr;

    public WifiServiceClient(Context context, boolean verbose, WifiMessageListener messageListener,
                             String remoteAddr, int port) {
        super(context, verbose, messageListener);
        this.remoteAddr = remoteAddr;
        this.port = port;
    }


    public void listenInBackground() throws NoConnectionException, IOException {
        if (v) Log.d(TAG, "listenInBackground()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        } else {
            // Cancel any thread currently running a connection
            if (threadListening != null) {
                threadListening.cancel();
                threadListening = null;
            }

            setState(STATE_LISTENING_CONNECTED);

            // Start the thread to connect with the given device
            threadListening = new WifiListenThread(wifiNetwork, handler);
            threadListening.start();
        }
    }

    protected void stopListeningInBackground() {
        if (v) Log.d(TAG, "stopListeningInBackground()");

        if (state == STATE_LISTENING_CONNECTED) {
            // Cancel any thread currently running a connection
            if (threadListening != null) {
                threadListening.cancel();
                threadListening = null;
            }
            // Update the state of the connection
            setState(STATE_CONNECTED);
        }
    }

    public void send(MessageAdHoc msg) throws IOException, NoConnectionException {
        if (v) Log.d(TAG, "send()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        } else {
            // Send message to remote device
            wifiNetwork.sendObjectStream(msg);

            // Notify handler
            handler.obtainMessage(WifiService.MESSAGE_WRITE, msg).sendToTarget();
        }
    }

    public void disconnect() throws NoConnectionException {
        if (v) Log.d(TAG, "disconnect()");

        if (state == STATE_NONE) {
            throw new NoConnectionException("No remote connection");
        } else {
            if (state == STATE_CONNECTED) {
                wifiNetwork.closeConnection();
            } else if (state == STATE_LISTENING_CONNECTED) {
                stopListeningInBackground();
            }

            // Update the state of the connection
            setState(STATE_NONE);
        }
    }

    @Override
    public void run() {
        if (v) Log.d(TAG, "connect to: " + remoteAddr + ":" + port);

        if (state == STATE_NONE) {

            // Change the state
            setState(STATE_CONNECTING);

            try {
                // Connect to the remote host
                Socket socket = new Socket();

                socket.bind(null);
                socket.connect((new InetSocketAddress(remoteAddr, port)), 5000);

                wifiNetwork = new WifiNetwork(socket, true);

                // Notify handler
                String messageHandle = socket.getRemoteSocketAddress().toString();
                handler.obtainMessage(WifiService.CONNECTION_PERFORMED, messageHandle).sendToTarget();

                // Update state
                setState(STATE_CONNECTED);
            } catch (IOException e) {
                setState(STATE_NONE);
                e.printStackTrace();
            }
        }
    }
}
