package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import android.os.Handler;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class UdpServer extends Thread {

    private final static String TAG = "[AdHoc][UdpServer]";
    private final static int SIZE = 65507;

    private final boolean v;
    private final int serverPort;
    private final Handler handler;
    private final ObjectMapper mapper;

    private DatagramSocket socket;
    private boolean running;

    UdpServer(boolean verbose, Handler handler, int serverPort) {
        this.v = verbose;
        this.handler = handler;
        this.serverPort = serverPort;
        this.mapper = new ObjectMapper();
    }

    void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {

        running = true;
        try {
            if (v) Log.d(TAG, "Starting UDP Server on " + serverPort);
            socket = new DatagramSocket(serverPort);

            while (running) {

                // Receive request
                byte[] buffer = new byte[SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
                socket.receive(packet);

                // Send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                MessageAdHoc msg = deserialize(packet.getData());
                // Update Handler
                handler.obtainMessage(Service.MESSAGE_READ, msg).sendToTarget();
            }

            if (v) Log.d(TAG, "UDP Server ended");

        } catch (IOException e) {
            handler.obtainMessage(Service.MESSAGE_EXCEPTION, e).sendToTarget();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private MessageAdHoc deserialize(byte[] data) throws IOException {
        MessageAdHoc msg = mapper.readValue(data, MessageAdHoc.class);
        Log.d(TAG, "Received message: " + msg);
        return msg;
    }
}