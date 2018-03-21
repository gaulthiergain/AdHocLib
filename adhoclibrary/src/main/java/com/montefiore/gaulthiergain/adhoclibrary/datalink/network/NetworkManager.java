package com.montefiore.gaulthiergain.adhoclibrary.datalink.network;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class NetworkManager {

    private static final String TAG = "[AdHoc][Network]";

    private ISocket isocket;
    private final ObjectMapper mapper;
    private final DataInputStream ois;
    private final DataOutputStream oos;

    public void setSocket(ISocket isocket) {
        this.isocket = isocket;
    }

    public ISocket getISocket() {
        return isocket;
    }


    public NetworkManager(ISocket isocket) throws IOException {
        this.isocket = isocket;
        this.oos = new DataOutputStream(isocket.getOutputStream());
        this.ois = new DataInputStream(isocket.getInputStream());
        this.mapper = new ObjectMapper();
    }

    public void sendMessage(MessageAdHoc msg) throws IOException {

        PrintWriter pw = new PrintWriter(oos);
        pw.println(mapper.writeValueAsString(msg));
        pw.flush();
        Log.d(TAG, "Send message: " + msg);
    }

    public MessageAdHoc receiveMessage() throws IOException, ClassNotFoundException, NullPointerException {

        BufferedReader in = new BufferedReader(new InputStreamReader(ois));
        MessageAdHoc msg = mapper.readValue(in.readLine(), MessageAdHoc.class);
        Log.d(TAG, "Received message: " + msg);

        return msg;
    }

    public void closeConnection() throws IOException {
        oos.close();
        ois.close();
        isocket.close();
    }

}
