package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceMessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;

import java.net.InetAddress;

/**
 * <p>This class represents a UDP peers and contains the server and client logic code.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class UdpPeers extends Thread {

    private static final String TAG = "[AdHoc][UDPPeers]";
    private final Handler handler;
    private final boolean v;
    private final boolean json;
    private UdpServer udpServer;

    /**
     * @param verbose                a boolean value to set the debug/verbose mode.
     * @param serverPort             an integer value to set the listening port number.
     * @param json                   a boolean value to use json or bytes in network transfer.
     * @param label                  a String value which represents the label used to identify device.
     * @param serviceMessageListener a serviceMessageListener object which contains callback functions.
     */
    @SuppressLint("HandlerLeak")
    public UdpPeers(boolean verbose, int serverPort, boolean json, String label,
                    final ServiceMessageListener serviceMessageListener) {
        this.v = verbose;
        this.json = json;
        this.handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case Service.MESSAGE_READ:
                        if (v) Log.d(TAG, "MESSAGE_READ");
                        serviceMessageListener.onMessageReceived((MessageAdHoc) msg.obj);
                        break;
                    case Service.MESSAGE_EXCEPTION:
                        if (v) Log.e(TAG, "MESSAGE_EXCEPTION");
                        serviceMessageListener.onMsgException((Exception) msg.obj);
                        break;
                    case Service.NETWORK_UNREACHABLE:
                        if (v) Log.e(TAG, "NETWORK_UNREACHABLE");
                        serviceMessageListener.onConnectionClosed((String) msg.obj);
                        break;
                    case Service.LOG_EXCEPTION:
                        if (v) Log.e(TAG, "LOG_EXCEPTION: " + ((Exception) msg.obj).getMessage());
                        break;
                }
            }
        };

        if (v) Log.d(TAG, "Listen in background");
        udpServer = new UdpServer(v, handler, serverPort, json, label);
        udpServer.start();
    }

    /**
     * Method allowing to stop the UDP server.
     */
    public void stopServer() {
        udpServer.stopServer();
    }

    /**
     * Method allowing to send a message to a remote UDP peer.
     *
     * @param msg        a MessageAdHoc object which represents a PDU exchanged between nodes.
     * @param serverAddr remoteAddress a String value which represents the IP address of the remote peer.
     * @param serverPort an integer value to set the remote listening port number.
     */
    public void sendMessageTo(MessageAdHoc msg, InetAddress serverAddr, int serverPort) {
        UdpClient udpClient = new UdpClient(handler, json, msg, serverAddr, serverPort);
        udpClient.start();
    }
}

