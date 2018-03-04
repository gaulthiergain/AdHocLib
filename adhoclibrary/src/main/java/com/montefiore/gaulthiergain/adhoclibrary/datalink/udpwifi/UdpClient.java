package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import android.os.Handler;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpClient extends Thread {

    private final boolean v;
    private final Handler handler;
    private MessageAdHoc message;
    private InetAddress serverAddr;
    private int serverPort;
    private static final String TAG = "[AdHoc][UDPClient]";

    UdpClient(boolean verbose, Handler handler, MessageAdHoc message, InetAddress serverAddr, int serverPort) {
        this.v = verbose;
        this.handler = handler;
        this.message = message;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {

        try {
            byte[] msg = serializeMessage(message);

            DatagramSocket datagramSocket = null;

            try {
                datagramSocket = new DatagramSocket();
                DatagramPacket datagramPacket;
                datagramPacket = new DatagramPacket(msg, msg.length, serverAddr, serverPort);
                datagramSocket.send(datagramPacket);
                handler.obtainMessage(Service.MESSAGE_WRITE, message).sendToTarget();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (datagramSocket != null) {
                    datagramSocket.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private byte[] serializeMessage(MessageAdHoc message) throws IOException {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(bStream);
        oo.writeObject(message);
        oo.close();

        return bStream.toByteArray();
    }
}


