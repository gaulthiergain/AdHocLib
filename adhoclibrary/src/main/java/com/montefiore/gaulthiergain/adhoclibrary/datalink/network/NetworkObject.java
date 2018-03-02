package com.montefiore.gaulthiergain.adhoclibrary.datalink.network;

import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class NetworkObject {

    private static final String TAG = "[AdHoc][Network]";

    private ISocket isocket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public void setSocket(ISocket isocket) {
        this.isocket = isocket;
    }

    public ISocket getISocket() {
        return isocket;
    }


    public NetworkObject(ISocket isocket) {
        try {
            this.isocket = isocket;
            this.oos = new ObjectOutputStream(isocket.getOutputStream());
            this.ois = new ObjectInputStream(isocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendObjectStream(Object obj) throws IOException {
        /*
        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = mapper.writeValueAsString(obj);
         */
        oos.writeObject(obj);
        oos.flush();
        if (obj.toString().length() > 50) {
            Log.d(TAG, "Object sent: " + obj.toString().substring(0, 30) + "...");
        } else {
            Log.d(TAG, "Object sent: " + obj);
        }
    }

    public Object receiveObjectStream() throws IOException, ClassNotFoundException {

        /*
        ObjectMapper mapper = new ObjectMapper();
        Object obj = mapper.readValue((String) ois.readObject(), MessageAdHoc.class);
         */
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
            isocket.close();
        } catch (IOException ex) {
            Log.d(TAG, "Error I/O: " + ex.getMessage());
        }
    }

    public OutputStream getOutputStream() {
        return this.oos;
    }
}
