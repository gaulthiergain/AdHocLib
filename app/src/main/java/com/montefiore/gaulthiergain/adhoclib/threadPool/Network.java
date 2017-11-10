package com.montefiore.gaulthiergain.adhoclib.threadPool;


import android.util.Log;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by gaulthiergain on 10/11/17.
 */
public class Network {

    private static final String TAG = "[AdHoc]";

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }

    public Network(Socket socket, boolean object) {
        try {
            this.socket = socket;
            if (!object) {
                this.dis = new DataInputStream(socket.getInputStream());
                this.dos = new DataOutputStream(socket.getOutputStream());
            } else {
                this.oos = new ObjectOutputStream(socket.getOutputStream());
                this.ois = new ObjectInputStream(socket.getInputStream());
            }
        } catch (IOException ex) {
            Logger.getLogger(Network.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendObject(Object obj) throws IOException {
        oos.writeObject(obj);
        oos.flush();
        if (obj.toString().length() > 50) {
            Log.d(TAG, "Object sent: " + obj.toString().substring(0, 30) + "...");
        } else {
            Log.d(TAG, "Object sent: " + obj);
        }
    }


    public void send(String msg) throws IOException {
        dos.writeInt(msg.length());
        dos.flush();
        dos.writeBytes(msg);
        dos.flush();
        Log.d(TAG, "Message sent: " + msg);
    }

    public void send(String msg, String endchar) throws IOException {
        dos.writeBytes(msg + endchar);
        dos.flush();
        Log.d(TAG, "Message sent: " + msg + endchar);
    }

    public void sendUTF(String msg, String endchar) throws IOException {
        dos.writeUTF(msg + endchar);
        dos.flush();
        Log.d(TAG, "Message sent: " + msg);
    }

    public Object receiveObject() throws IOException, ClassNotFoundException {
        Object obj = ois.readObject();

        if (obj.toString().length() > 50) {
            Log.d(TAG, "Received object: " + obj.toString().substring(0, 30) + "...");
        } else {
            Log.d(TAG, "Received object: " + obj);
        }
        return obj;
    }

    public String receive() throws IOException {
        int size = dis.readInt();
        byte[] bytes = new byte[size];
        dis.readFully(bytes);
        return new String(bytes);
    }

    public String receive(char endchar) throws IOException {
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

    public void closeConnection() {
        try {
            dos.close();
            dis.close();
            socket.close();
        } catch (IOException ex) {
            Log.d(TAG, "Error I/O: " + ex.getMessage());
        }
    }

    public void closeConnectionObject() {
        try {
            oos.close();
            ois.close();
            socket.close();
        } catch (IOException ex) {
            Log.d(TAG, "Error I/O: " + ex.getMessage());
        }
    }
}
