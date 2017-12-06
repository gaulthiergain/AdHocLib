package com.montefiore.gaulthiergain.adhoclibrary.network;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

public class NetworkDataStream {
    private static final String TAG = "[AdHoc]";

    private DataInputStream dis;
    private DataOutputStream dos;

    public NetworkDataStream(DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
    }

    public void sendDataStream(String msg) throws IOException {
        dos.writeInt(msg.length());
        dos.flush();
        dos.writeBytes(msg);
        dos.flush();
        Log.d(TAG, "MessageAdHoc sent: " + msg);
    }

    public void sendDataStream(String msg, String endchar) throws IOException {
        dos.writeBytes(msg + endchar);
        dos.flush();
        Log.d(TAG, "MessageAdHoc sent: " + msg + endchar);
    }

    public void sendUTFDataStream(String msg, String endchar) throws IOException {
        dos.writeUTF(msg + endchar);
        dos.flush();
        Log.d(TAG, "MessageAdHoc sent: " + msg);
    }

    public String receiveDataStream() throws IOException {
        int size = dis.readInt();
        byte[] bytes = new byte[size];
        dis.readFully(bytes);
        return new String(bytes);
    }

    public String receiveDataStream(char endchar) throws IOException {
        StringBuilder trame = new StringBuilder();
        char c;
        while (true) {
            try {
                c = (char) dis.readByte();
                if (c == endchar) {
                    break;
                } else {
                    trame.append(c);
                }
            } catch (EOFException ex) {
                Log.d(TAG, "Error EOF: " + ex.getMessage());
            }
        }
        Log.d(TAG, "Received msg: " + trame.toString());
        return trame.toString();
    }

    public void closeConnectionDataStream() {
        try {
            dos.close();
            dis.close();
            //socket.close(); //TODO
        } catch (IOException ex) {
            Log.d(TAG, "Error I/O: " + ex.getMessage());
        }
    }
}
