package com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.AdHocSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.RemoteConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceClient;

import java.io.IOException;
import java.util.UUID;

/**
 * <p>This class defines the client's logic for bluetooth implementation. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class BluetoothServiceClient extends ServiceClient implements Runnable {

    private final boolean secure;
    private final BluetoothAdHocDevice bluetoothAdHocDevice;

    private ListenerAutoConnect listenerAutoConnect;

    /**
     * Constructor
     *
     * @param verbose              a boolean value to set the debug/verbose mode.
     * @param context              a Context object which gives global information about an application
     *                             environment.
     * @param json                 a boolean value to use json or bytes in network transfer.
     * @param background           a boolean value which defines if the service must listen messages
     *                             to background.
     * @param secure               a boolean value which represents the state of the connection.
     * @param attempts             a short value which represents the number of attempts.
     * @param bluetoothAdHocDevice a BluetoothAdHocDevice object which represents a remote Bluetooth
     *                             device.
     * @param messageListener      a messageListener object which serves as callback functions.
     */
    public BluetoothServiceClient(boolean verbose, Context context, boolean json, boolean background,
                                  boolean secure, short attempts,
                                  BluetoothAdHocDevice bluetoothAdHocDevice, MessageListener messageListener) {
        super(verbose, context, attempts, json, background, messageListener);
        this.secure = secure;
        this.bluetoothAdHocDevice = bluetoothAdHocDevice;
    }

    /**
     * Method allowing to connect to a remote bluetooth device.
     *
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     */
    private void connect() throws NoConnectionException {
        if (v) Log.d(TAG, "connect to: " + bluetoothAdHocDevice.getDevice().getName()
                + " (" + bluetoothAdHocDevice.getUuid() + ")");

        if (state == STATE_NONE || state == STATE_CONNECTING) {

            // Get the UUID
            UUID uuid = UUID.fromString(bluetoothAdHocDevice.getUuid());
            // Change the state
            setState(STATE_CONNECTING);

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                BluetoothSocket bluetoothSocket;
                if (secure) {
                    bluetoothSocket = bluetoothAdHocDevice.getDevice().createRfcommSocketToServiceRecord(uuid);
                } else {
                    bluetoothSocket = bluetoothAdHocDevice.getDevice().createInsecureRfcommSocketToServiceRecord(uuid);
                }

                // Connect to the remote host
                bluetoothSocket.connect();
                network = new SocketManager(new AdHocSocketBluetooth(bluetoothSocket), json);
                if (listenerAutoConnect != null) {
                    listenerAutoConnect.connected(uuid, network);
                }

                // Notify handler
                handler.obtainMessage(Service.CONNECTION_PERFORMED,
                        new RemoteConnection(bluetoothSocket.getRemoteDevice().getAddress(),
                                bluetoothSocket.getRemoteDevice().getName())).sendToTarget();

                // Update state
                setState(STATE_CONNECTED);

                // Listen in Background
                if (background) {
                    listenInBackground();
                }
            } catch (IOException e) {
                setState(STATE_NONE);
                throw new NoConnectionException("No remote connection to "
                        + bluetoothAdHocDevice.getUuid());
            }
        }
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
                    handler.obtainMessage(Service.CONNECTION_FAILED, new RemoteConnection(
                            bluetoothAdHocDevice.getDevice().getAddress(),
                            bluetoothAdHocDevice.getDevice().getName())).sendToTarget();
                    break;
                }

                try {
                    Thread.sleep((getBackOffTime()));
                } catch (InterruptedException e1) {
                    handler.obtainMessage(Service.CATH_EXCEPTION, e1).sendToTarget();
                }
            }
        } while (i < this.attempts);
    }

    public SocketManager getNetwork() {
        return network;
    }

    public void setListenerAutoConnect(ListenerAutoConnect listenerAutoConnect) {
        this.listenerAutoConnect = listenerAutoConnect;
    }

    public interface ListenerAutoConnect {
        void connected(UUID uuid, SocketManager network) throws IOException, NoConnectionException;
    }
}
