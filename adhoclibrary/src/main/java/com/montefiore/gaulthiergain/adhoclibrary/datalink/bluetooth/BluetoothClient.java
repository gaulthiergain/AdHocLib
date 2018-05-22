package com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceMessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.AdHocSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * <p>This class defines the client's logic for bluetooth implementation. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class BluetoothClient extends ServiceClient implements Runnable {

    private final boolean secure;
    private final BluetoothAdHocDevice bluetoothAdHocDevice;
    private ListenerAutoConnect listenerAutoConnect;
    private BluetoothSocket bluetoothSocket;

    /**
     * Constructor
     *
     * @param verbose                a boolean value to set the debug/verbose mode.
     * @param json                   a boolean value to use json or bytes in network transfer.
     * @param timeOut                an integer value which represents the time-out of a connection.
     * @param secure                 a boolean value which represents the state of the connection.
     * @param attempts               a short value which represents the number of attempts.
     * @param bluetoothAdHocDevice   a BluetoothAdHocDevice object which represents a remote Bluetooth
     *                               device.
     * @param serviceMessageListener a serviceMessageListener object which contains callback functions.
     */
    public BluetoothClient(boolean verbose, boolean json, int timeOut,
                           boolean secure, short attempts,
                           BluetoothAdHocDevice bluetoothAdHocDevice, ServiceMessageListener serviceMessageListener) {
        super(verbose, timeOut, attempts, json, serviceMessageListener);
        this.secure = secure;
        this.bluetoothAdHocDevice = bluetoothAdHocDevice;
    }

    /**
     * Method allowing to connect to a remote bluetooth device.
     *
     * @throws NoConnectionException signals that a No Connection Exception exception has occurred.
     */
    private void connect() throws NoConnectionException {

        if (state == STATE_NONE || state == STATE_CONNECTING) {

            if (v) Log.d(TAG, "connect to: " + bluetoothAdHocDevice.getDeviceName()
                    + " (" + bluetoothAdHocDevice.getUuid() + ")");

            // Get the UUID
            UUID uuid = UUID.fromString(bluetoothAdHocDevice.getUuid());
            // Change the state
            setState(STATE_CONNECTING);

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                if (secure) {
                    bluetoothSocket = bluetoothAdHocDevice.getDevice().createRfcommSocketToServiceRecord(uuid);
                } else {
                    bluetoothSocket = bluetoothAdHocDevice.getDevice().createInsecureRfcommSocketToServiceRecord(uuid);
                }

                // Connect to the remote host
                timeout();
                bluetoothSocket.connect();
                network = new SocketManager(new AdHocSocketBluetooth(bluetoothSocket), json);
                if (listenerAutoConnect != null) {
                    listenerAutoConnect.connected(uuid, network);
                }

                // Notify handler
                handler.obtainMessage(Service.CONNECTION_PERFORMED,
                        bluetoothSocket.getRemoteDevice().getAddress()).sendToTarget();

                // Listen in Background
                listenInBackground();

                // Update state
                setState(STATE_CONNECTED);

            } catch (IOException e) {
                setState(STATE_NONE);
                throw new NoConnectionException("Unable to connect to "
                        + bluetoothAdHocDevice.getUuid());
            }
        }
    }

    /**
     * Method allowing to launch a time-out if a connection fails.
     */
    private void timeout() {

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!bluetoothSocket.isConnected()) {
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, timeOut);
    }

    @Override
    public void run() {

        int i = 0;
        do {
            try {
                i++;
                connect();
            } catch (NoConnectionException e) {

                if (v) Log.e(TAG, "Attempts: " + i + " failed");
                if (attempts == i) {
                    handler.obtainMessage(Service.CONNECTION_FAILED, e).sendToTarget();
                    break;
                }

                try {
                    // Back-off algorithm before connecting
                    Thread.sleep((getBackOffTime()));
                } catch (InterruptedException e1) {
                    handler.obtainMessage(Service.CONNECTION_FAILED, e1).sendToTarget();
                }
            }
        } while (i < this.attempts);
    }

    /**
     * Method allowing to get a SocketManager object.
     *
     * @return a SocketManager object which a socket connected between two devices.
     */
    public SocketManager getNetwork() {
        return network;
    }

    /**
     * Method allowing to set a ListenerAutoConnect object.
     *
     * @param listenerAutoConnect a ListenerAutoConnect object which contains callback functions.
     */
    public void setListenerAutoConnect(ListenerAutoConnect listenerAutoConnect) {
        this.listenerAutoConnect = listenerAutoConnect;
    }

    public interface ListenerAutoConnect {
        void connected(UUID uuid, SocketManager network) throws IOException, NoConnectionException;
    }
}
