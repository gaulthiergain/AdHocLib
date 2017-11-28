package com.montefiore.gaulthiergain.adhoclibrary.network;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WifiNetwork {

    private static final String TAG = "[AdHoc]";

    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }


    public WifiNetwork(Socket socket, boolean object) {
        try {
            this.socket = socket;
            this.oos = new ObjectOutputStream(socket.getOutputStream());
            this.ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(WifiNetwork.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendObjectStream(Object obj) throws IOException {
        oos.writeObject(obj);
        oos.flush();
        if (obj.toString().length() > 50) {
            Log.d(TAG, "Object sent: " + obj.toString().substring(0, 30) + "...");
        } else {
            Log.d(TAG, "Object sent: " + obj);
        }
    }

    public Object receiveObjectStream() throws IOException, ClassNotFoundException {
        Object obj = ois.readObject();

        if (obj.toString().length() > 50) {
            Log.d(TAG, "Received object: " + obj.toString().substring(0, 30) + "...");
        } else {
            Log.d(TAG, "Received object: " + obj);
        }
        return obj;
    }

    public void closeConnection() {
        try {
            oos.close();
            ois.close();
            socket.close();
        } catch (IOException ex) {
            Log.d(TAG, "Error I/O: " + ex.getMessage());
        }
    }
}
