package com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.Utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * <p>This class manages the connection between peers and allows to send and receive network messages.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class SocketManager {

    private final ISocket isocket;
    private final ObjectMapper mapper;
    private final DataInputStream ois;
    private final DataOutputStream oos;
    private final String remoteSocketAddress;
    private final boolean json;

    /**
     * Constructor
     *
     * @param isocket a ISocket object which represents a socket between the server and the client.
     * @param json    a boolean value to use json or bytes in network transfer.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public SocketManager(ISocket isocket, boolean json) throws IOException {
        this.isocket = isocket;
        this.remoteSocketAddress = isocket.getRemoteSocketAddress();
        this.oos = new DataOutputStream(isocket.getOutputStream());
        this.ois = new DataInputStream(isocket.getInputStream());
        this.mapper = new ObjectMapper();
        this.json = json;
    }

    /**
     * Method allowing to send a message from the remote peer.
     *
     * @param msg a MessageAdHoc object which represents a PDU exchanged between nodes.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void sendMessage(MessageAdHoc msg) throws IOException {

        if (json) {
            PrintWriter pw = new PrintWriter(oos);
            pw.println(mapper.writeValueAsString(msg));
            pw.flush();
        } else {
            byte[] byteArray = Utils.serialize(msg);
            if (byteArray != null) {
                oos.writeInt(byteArray.length);
                oos.write(byteArray);
            }
        }
    }

    /**
     * Method allowing to receive a message from the remote peer.
     *
     * @return msg a MessageAdHoc object which represents a PDU exchanged between nodes.
     * @throws IOException            signals that an I/O exception of some sort has occurred.
     * @throws ClassNotFoundException signals that a class was not found.
     */
    public MessageAdHoc receiveMessage() throws IOException, ClassNotFoundException {

        if (json) {
            BufferedReader in = new BufferedReader(new InputStreamReader(ois));
            MessageAdHoc msg;
            try {
                msg = mapper.readValue(in.readLine(), MessageAdHoc.class);
            } catch (NullPointerException e) {
                throw new IOException("Closed remote socket");
            }
            return msg;
        } else {
            int length = ois.readInt();
            if (length > 0) {
                byte[] message = new byte[length];
                ois.readFully(message, 0, message.length);
                return Utils.deserialize(message);
            }
            return null;
        }
    }

    /**
     * Method allowing to close a current connection.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void closeConnection() throws IOException {
        oos.close();
        ois.close();
        isocket.close();
    }

    /**
     * Method allowing to get a Isocket object.
     *
     * @return a ISocket object which represents a socket between the server and the client.
     */
    public ISocket getISocket() {
        return isocket;
    }

    /**
     * Method allowing to get a the address of a remote peer.
     *
     * @return a String value which represents the address of a remote peer.
     */
    public String getRemoteSocketAddress() {
        return remoteSocketAddress;
    }
}
