package com.montefiore.gaulthiergain.adhoclibrary.datalink.threadmanager;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.ISocket;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice.RemoteBtDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice.RemoteWifiDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.EOFException;
import java.io.IOException;

class ThreadClient extends Thread {

    private static final String TAG = "[AdHoc][ThreadClient]";

    private final ListSocketDevice listSocketDevice;
    private final String name;
    private final Handler handler;
    private NetworkObject network = null;

    ThreadClient(ListSocketDevice listSocketDevice, String name, Handler handler) {
        this.listSocketDevice = listSocketDevice;
        this.name = name;
        this.handler = handler;
    }

    public void run() {
        ISocket socketDevice = null;
        while (!isInterrupted()) {
            try {
                socketDevice = listSocketDevice.getSocketDevice();
                network = listSocketDevice.getActiveConnection().get(socketDevice.getRemoteSocketAddress());
                while (true) {
                    processRequest((MessageAdHoc) network.receiveObjectStream());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                //handler.obtainMessage(Service.CATH_EXCEPTION, e).sendToTarget();
            } catch (EOFException e) {
                handler.obtainMessage(Service.CATH_EXCEPTION, e).sendToTarget();
            } catch (IOException e) {
                handler.obtainMessage(Service.CATH_EXCEPTION, e).sendToTarget();
            } catch (ClassNotFoundException e) {
                handler.obtainMessage(Service.CATH_EXCEPTION, e).sendToTarget();
            } finally {
                if (network != null) {
                    if (socketDevice instanceof AdHocSocketWifi) {
                        // Notify handler
                        handler.obtainMessage(Service.CONNECTION_ABORTED,
                                new RemoteWifiDevice(network.getISocket().getRemoteSocketAddress()))
                                .sendToTarget();
                    } else {
                        // Get Socket
                        BluetoothSocket socket = (BluetoothSocket) network.getISocket().getSocket();
                        // Notify handler
                        handler.obtainMessage(Service.CONNECTION_ABORTED,
                                new RemoteBtDevice(socket.getRemoteDevice().getAddress(),
                                        socket.getRemoteDevice().getName())).sendToTarget();
                    }

                    // Remove client from hashmap
                    listSocketDevice.removeActiveConnexion(socketDevice);

                    // Close network
                    network.closeConnection();
                }

            }
        }
    }

    private void processRequest(MessageAdHoc request) throws IOException {
        handler.obtainMessage(Service.MESSAGE_READ, request).sendToTarget();
    }

    String getNameThread() {
        return name;
    }
}
