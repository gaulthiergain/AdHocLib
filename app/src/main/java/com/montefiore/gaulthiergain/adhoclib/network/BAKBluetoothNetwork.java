package com.montefiore.gaulthiergain.adhoclib.network;

/**
 * Created by gaulthiergain on 28/10/17.
 */


import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class provides methods to manage network communications. It allows to
 * open/close connections and to send/receive messages.
 *
 * @author Gain Gaulthier
 */
public class BAKBluetoothNetwork {

    private BluetoothSocket socket;
    private InputStream input;
    private OutputStream output;
    private String TAG = "[AdHoc]";


    /**
     * Constructor.
     *
     * @param socket a stream socket connected to the specified client.
     */
    public BAKBluetoothNetwork(BluetoothSocket socket) throws IOException {
        this.socket = socket;
        this.input = socket.getInputStream();
        this.output = socket.getOutputStream();
    }

    public void sendString(String message) throws IOException {
        output.write(message.getBytes());
        output.flush();
    }

    public String receiveString() throws IOException {
        byte[] buffer = new byte[256];
        int length;
        length = input.read(buffer);
        return new String(buffer, 0, length);
    }

    public void closeSocket() throws IOException {
        socket.close();
    }


    public void connect() throws IOException {
        socket.connect();
    }


    public BluetoothSocket getSocket() {
        return socket;
    }

    public InputStream getInput() {
        return input;
    }

    public OutputStream getOutput() {
        return output;
    }
}
