package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import android.os.Handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

class UdpClient extends Thread {

    private final int serverPort;
    private final boolean json;
    private final Handler handler;
    private final MessageAdHoc msg;
    private final InetAddress serverAddr;

    UdpClient(Handler handler, boolean json, MessageAdHoc msg, InetAddress serverAddr, int serverPort) {
        this.json = json;
        this.handler = handler;
        this.msg = msg;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {

        try {
            byte[] msgBytes;
            if (json) {
                ObjectMapper mapper = new ObjectMapper();
                msgBytes = mapper.writeValueAsString(msg).getBytes();
            } else {
                msgBytes = Utils.serialize(msg);
            }

            DatagramSocket datagramSocket = null;
            try {
                datagramSocket = new DatagramSocket();
                DatagramPacket datagramPacket = new DatagramPacket(msgBytes, msgBytes.length, serverAddr, serverPort);
                datagramSocket.send(datagramPacket);
            } catch (SocketException e) {
                handler.obtainMessage(Service.NETWORK_UNREACHABLE, serverAddr.getHostAddress()).sendToTarget();
            } catch (IOException e) {
                handler.obtainMessage(Service.MESSAGE_EXCEPTION, e).sendToTarget();
            } finally {
                if (datagramSocket != null) {
                    datagramSocket.close();
                }
            }
        } catch (IOException e) {
            handler.obtainMessage(Service.MESSAGE_EXCEPTION, e).sendToTarget();
        }
    }
}


