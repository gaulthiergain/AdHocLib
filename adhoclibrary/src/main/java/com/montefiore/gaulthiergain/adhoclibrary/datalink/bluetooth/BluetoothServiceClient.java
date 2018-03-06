package com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.AdHocSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.remotedevice.RemoteBtDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceClient;

import java.io.IOException;
import java.util.Random;
import java.util.UUID;

/**
 * <p>This class defines the client's logic for bluetooth implementation. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class BluetoothServiceClient extends ServiceClient implements Runnable {

    //todo refactor this? (backoff algorithm)
    private static final int LOW = 500;
    private static final int HIGH = 2500;

    private final boolean secure;
    private final int attempts;
    private final BluetoothAdHocDevice bluetoothAdHocDevice;

    private ListenerAutoConnect listenerAutoConnect;

    /**
     * Constructor
     *
     * @param verbose              a boolean value to set the debug/verbose mode.
     * @param context              a Context object which gives global information about an application
     *                             environment.
     * @param messageListener      a messageListener object which serves as callback functions.
     * @param background           a boolean value which defines if the service must listen messages
     *                             to background.
     * @param secure               a boolean value which represents the state of the connection.
     * @param attempts             an integer value which represents the number of attempts.
     * @param bluetoothAdHocDevice a BluetoothAdHocDevice object which represents a remote Bluetooth
     *                             device.
     */
    public BluetoothServiceClient(boolean verbose, Context context, MessageListener messageListener,
                                  boolean background, boolean secure, int attempts,
                                  BluetoothAdHocDevice bluetoothAdHocDevice) {
        super(verbose, context, messageListener, background);
        this.secure = secure;
        this.attempts = attempts;
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
                network = new NetworkObject(new AdHocSocketBluetooth(bluetoothSocket));
                if (listenerAutoConnect != null) {
                    listenerAutoConnect.connected(uuid, network);
                }

                // Notify handler
                handler.obtainMessage(Service.CONNECTION_PERFORMED,
                        new RemoteBtDevice(bluetoothSocket.getRemoteDevice().getAddress(),
                                bluetoothSocket.getRemoteDevice().getName(),
                                bluetoothAdHocDevice.getUuid())).sendToTarget();

                // Update state
                setState(STATE_CONNECTED);

                // Listen in Background
                if (background) {
                    listenInBackground();
                }
            } catch (IOException e) {
                setState(STATE_NONE);
                throw new NoConnectionException("No remote connection");
            }
        }
    }

    @Override
    public void run() {
        int i = 0;
        do {
            try {
                connect();
                i = attempts;
            } catch (NoConnectionException e) {
                i++;
                try {
                    long result = (long) new Random().nextInt(HIGH - LOW) + LOW;
                    Thread.sleep((result));
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                Log.e(TAG, "Attempts: " + i + " failed in thread " + Thread.currentThread().getName());
            }
        } while (i < this.attempts);
    }

    public NetworkObject getNetwork() {
        return network;
    }

    public void setListenerAutoConnect(ListenerAutoConnect listenerAutoConnect) {
        this.listenerAutoConnect = listenerAutoConnect;
    }

    public interface ListenerAutoConnect {
        void connected(UUID uuid, NetworkObject network) throws IOException, NoConnectionException;
    }
}
