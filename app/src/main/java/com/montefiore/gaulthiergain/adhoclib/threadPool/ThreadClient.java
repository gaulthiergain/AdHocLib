package com.montefiore.gaulthiergain.adhoclib.threadPool;

import android.util.Log;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by gaulthiergain on 10/11/17.
 *
 */
public class ThreadClient extends Thread {

    private static final String TAG = "[AdHoc]";

    private ListSocketDevice listSocketDevice;
    private String name;
    private Network network = null;

    ThreadClient(ListSocketDevice listSocketDevice, String name) {
        this.listSocketDevice = listSocketDevice;
        this.name = name;
    }

    public void run() {
        while (!isInterrupted()) {
            try {
                Socket socketDevice = listSocketDevice.getSocketDevice();
                network = new Network(socketDevice, false);
                while (true) {
                    processRequest(network.receive());
                }
            } catch (InterruptedException e) {
                Log.d(TAG, "Error InterruptedException: " + e.getMessage());
            } catch (EOFException e) {
                Log.d(TAG, "Error EOFException: " + e.getMessage());
                e.printStackTrace();
            } catch (IOException e) {
                network.closeConnection();
                Log.d(TAG, "Error IOException: " + e.getMessage());
                e.printStackTrace();
            } finally {
                network.closeConnection();
            }
        }
    }

    private void processRequest(String request) throws IOException {
        Log.d(TAG, "Processing request " + request);
        network.send("CLOSING connection");
    }

}
