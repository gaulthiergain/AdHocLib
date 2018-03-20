package com.montefiore.gaulthiergain.adhoclibrary.datalink.network;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class NetworkObject {

    private static final String TAG = "[AdHoc][Network]";

    private ISocket isocket;
    private DataInputStream ois;
    private DataOutputStream oos;

    public void setSocket(ISocket isocket) {
        this.isocket = isocket;
    }

    public ISocket getISocket() {
        return isocket;
    }


    public NetworkObject(ISocket isocket) {
        try {
            this.isocket = isocket;
            this.oos = new DataOutputStream(isocket.getOutputStream());
            this.ois = new DataInputStream(isocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace(); //todo check this
        }
    }

    public void sendObjectStream(MessageAdHoc msg) throws IOException {

        String jsonStr = serialize(msg, false);

        PrintWriter pw = new PrintWriter(oos);
        pw.println(jsonStr);
        System.out.println(msg.toString());
        System.out.println(jsonStr);
        pw.flush();

        if (msg.toString().length() > 50) {
            Log.d(TAG, "Message sent: " + msg.toString().substring(0, 30) + "...");
        } else {
            Log.d(TAG, "Object sent: " + msg);
        }
    }

    public MessageAdHoc receiveObjectStream() throws IOException, ClassNotFoundException {

        BufferedReader in = new BufferedReader(new InputStreamReader(ois));

        ObjectMapper mapper = new ObjectMapper();
        String str = in.readLine();
        MessageAdHoc msg = mapper.readValue(str, MessageAdHoc.class);
        System.out.println("Received: " + str);

        if (msg.toString().length() > 50) {
            Log.d(TAG, "Received object: " + msg.toString().substring(0, 30) + "...");
        } else {
            Log.d(TAG, "Received object: " + msg);
        }
        return msg;
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

    private String serialize(Object obj, boolean pretty) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        if (pretty) {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        }

        return mapper.writeValueAsString(obj);
    }

    public OutputStream getOutputStream() {
        return this.oos;
    }
}
