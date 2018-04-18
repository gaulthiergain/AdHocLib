package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import android.os.Handler;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.util.Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

class UdpServer extends Thread {

    private final static String TAG = "[AdHoc][UdpServer]";
    private final static int SIZE = 65507;

    private final boolean v;
    private final boolean json;
    private final String label;
    private final int serverPort;
    private final Handler handler;
    private final ObjectMapper mapper;

    private DatagramSocket socket;
    private boolean running;

    UdpServer(boolean verbose, Handler handler, int serverPort, boolean json, String label) {
        this.v = verbose;
        this.json = json;
        this.handler = handler;
        this.serverPort = serverPort;
        this.label = label;
        this.mapper = new ObjectMapper();
    }

    @Override
    public void run() {

        running = true;
        try {
            if (v) Log.d(TAG, "Starting UDP Server on " + serverPort);
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(serverPort));

            while (running) {

                // Receive request
                byte[] buffer = new byte[SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
                socket.receive(packet);

                MessageAdHoc msg = deserialize(packet.getData());

                // Check if message if from other node
                Header header = msg.getHeader();
                if (header != null && header.getLabel() != null && !header.getLabel().equals(label)) {
                    // Update Handler
                    handler.obtainMessage(Service.MESSAGE_READ, msg).sendToTarget();
                } else {
                    //
                    Log.w(TAG, "IGNORED MESSAGE " + header);
                }
            }

            if (v) Log.d(TAG, "UDP Server ended");

        } catch (IOException e) {
            handler.obtainMessage(Service.MESSAGE_EXCEPTION, e).sendToTarget();
        } catch (ClassNotFoundException e) {
            handler.obtainMessage(Service.MESSAGE_EXCEPTION, e).sendToTarget();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private MessageAdHoc deserialize(byte[] data) throws IOException, ClassNotFoundException {
        if (json) {
            return mapper.readValue(data, MessageAdHoc.class);
        }
        return Utils.deserialize(data);
    }

    public void stopServer() {
        this.running = false;
    }
}