package com.montefiore.gaulthiergain.adhoclibrary.datalink.udpwifi;

import android.os.Handler;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

class UdpServer extends Thread {

    private final boolean v;
    private final int serverPort;
    private final Handler handler;
    private final static String TAG = "[AdHoc]";
    private DatagramSocket socket;
    private boolean running;

    UdpServer(boolean verbose, Handler handler, int serverPort) {
        this.v = verbose;
        this.handler = handler;
        this.serverPort = serverPort;
    }

    void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public void run() {

        running = true;

        try {
            Log.d(TAG, "Starting UDP Server");
            socket = new DatagramSocket(serverPort);


            while (running) {

                Log.d(TAG, "UDP Server is running");

                // receive request
                byte[] buffer = new byte[65507];
                DatagramPacket packet = new DatagramPacket(buffer, 0, buffer.length);
                socket.receive(packet);


                // send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();

                try {
                    MessageAdHoc msg = (MessageAdHoc) deserialize(packet.getData());
                    Log.d(TAG, "Request from: " + address + ":" + port + " - " + msg.toString() + "\n");
                    msg.setPdu(address);

                    handler.obtainMessage(Service.MESSAGE_READ, msg).sendToTarget();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

                /*String dString = new Date().toString() + "\n"
                        + "Your address " + address.toString() + ":" + String.valueOf(port);
                buf = dString.getBytes();
                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);*/
            }

            Log.d(TAG, "UDP Server ended");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
                Log.d(TAG, "socket.close()");
            }
        }
    }

    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }
}