package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

/**
 * <p>This class manages the UDP server for the Wi-Fi technology.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
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

    /**
     * @param verbose    a boolean value to set the debug/verbose mode.
     * @param handler    a Handler object which allows to send and process {@link Message}
     *                   and Runnable objects associated with a thread's.
     * @param serverPort an integer value to set the listening port number.
     * @param json       a boolean value to use json or bytes in network transfer.
     * @param label      a String value which represents the label used to identify device.
     */
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

                MessageAdHoc msg;
                if (json) {
                    msg = mapper.readValue(packet.getData(), MessageAdHoc.class);
                } else {
                    msg = Utils.deserialize(packet.getData());
                }

                if (msg != null) {
                    // Check if message if from other node
                    Header header = msg.getHeader();
                    if (header != null && header.getLabel() != null && !header.getLabel().equals(label)) {
                        // Update Handler
                        handler.obtainMessage(Service.MESSAGE_READ, msg).sendToTarget();
                    } else {
                        Log.w(TAG, "Ignored message: " + msg);
                    }
                } else {
                    handler.obtainMessage(Service.MESSAGE_EXCEPTION, new Exception("Null message")).sendToTarget();
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

    /**
     * Method allowing to stop the server.
     */
    public void stopServer() {
        this.running = false;
    }
}