package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceMessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.net.InetAddress;

public class UdpPeers extends Thread {

    private static final String TAG = "[AdHoc][UDPClient]";
    private final Handler handler;
    private final boolean v;
    private final boolean json;
    private UdpServer udpServer;


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

    public void stopServer() {
        udpServer.stopServer();
    }

    public void sendMessageTo(MessageAdHoc message, InetAddress serverAddr, int serverPort) {
        UdpClient udpClient = new UdpClient(handler, json, message, serverAddr, serverPort);
        udpClient.start();
    }
}

