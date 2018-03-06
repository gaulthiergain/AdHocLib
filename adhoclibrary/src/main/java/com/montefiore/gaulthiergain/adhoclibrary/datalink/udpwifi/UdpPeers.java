package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageMainListener;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.net.InetAddress;

import static com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service.CATH_EXCEPTION;
import static com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service.MESSAGE_READ;
import static com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service.MESSAGE_WRITE;

public class UdpPeers extends Thread {

    private static final String TAG = "[AdHoc][UDPClient]";
    private final Handler handler;
    private final boolean v;
    private UdpServer udpServer;

    @SuppressLint("HandlerLeak")
    public UdpPeers(boolean verbose, int serverPort, boolean background, final MessageMainListener messageListener) {
        this.v = verbose;
        this.handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_READ:
                        if (v) Log.d(TAG, "MESSAGE_READ");
                        messageListener.onMessageReceived((MessageAdHoc) msg.obj);
                        break;
                    case MESSAGE_WRITE:
                        if (v) Log.d(TAG, "MESSAGE_WRITE");
                        messageListener.onMessageSent((MessageAdHoc) msg.obj);
                        break;
                    case CATH_EXCEPTION:
                        if (v) Log.d(TAG, "MESSAGE_WRITE");
                        messageListener.catchException((Exception) msg.obj);
                        break;
                }
            }
        };
        if (background) {
            Log.d(TAG, "Listen in background");
            udpServer = new UdpServer(v, handler, serverPort);
            udpServer.start();
        }
    }

    public void setBackgroundRunning(boolean backgroundRunning) {
        udpServer.setRunning(backgroundRunning);
    }

    public void sendMessageTo(MessageAdHoc message, InetAddress serverAddr, int serverPort) {
        UdpClient udpClient = new UdpClient(v, handler, message, serverAddr, serverPort);
        udpClient.start();
    }
}

