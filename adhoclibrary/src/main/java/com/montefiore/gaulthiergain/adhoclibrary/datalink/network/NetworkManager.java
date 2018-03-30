package com.montefiore.gaulthiergain.adhoclibrary.datalink.network;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

public class NetworkManager {

    private ISocket isocket;
    private final ObjectMapper mapper;
    private final DataInputStream ois;
    private final DataOutputStream oos;
    private final boolean json;

    public void setSocket(ISocket isocket) {
        this.isocket = isocket;
    }

    public ISocket getISocket() {
        return isocket;
    }

    public NetworkManager(ISocket isocket, boolean json) throws IOException {
        this.isocket = isocket;
        this.oos = new DataOutputStream(isocket.getOutputStream());
        this.ois = new DataInputStream(isocket.getInputStream());
        this.mapper = new ObjectMapper();
        this.json = json;
    }

    public void sendMessage(MessageAdHoc msg) throws IOException {

        if (json) {
            Log.d("[AdHoc]", "JSON send");
            PrintWriter pw = new PrintWriter(oos);
            pw.println(mapper.writeValueAsString(msg));
            pw.flush();
        } else {
            Log.d("[AdHoc]", "Byte send");
            byte[] byteArray = serialize(msg);
            if (byteArray != null) {
                oos.writeInt(byteArray.length);
                oos.write(byteArray);
            }
        }
    }

    public MessageAdHoc receiveMessage() throws IOException, ClassNotFoundException {

        if (json) {
            Log.d("[AdHoc]", "JSON rcv");
            BufferedReader in = new BufferedReader(new InputStreamReader(ois));
            MessageAdHoc msg;
            try {
                msg = mapper.readValue(in.readLine(), MessageAdHoc.class);
            } catch (NullPointerException e) {
                throw new IOException("Closed remote socket");
            }
            return msg;
        } else {
            Log.d("[AdHoc]", "bytes rcv");
            int length = ois.readInt();
            if (length > 0) {
                byte[] message = new byte[length];
                ois.readFully(message, 0, message.length);
                return deserialize(message);
            }
            return null;
        }
    }

    public void closeConnection() throws IOException {
        oos.close();
        ois.close();
        isocket.close();
    }

    private MessageAdHoc deserialize(byte[] byteArray) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        ObjectInput in;

        in = new ObjectInputStream(bis);
        MessageAdHoc messageAdHoc = (MessageAdHoc) in.readObject();
        in.close();
        return messageAdHoc;
    }

    private byte[] serialize(MessageAdHoc messageAdHoc) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;

        out = new ObjectOutputStream(bos);
        out.writeObject(messageAdHoc);
        out.flush();
        byte[] byteArray = bos.toByteArray();
        bos.close();
        return byteArray;
    }
}
