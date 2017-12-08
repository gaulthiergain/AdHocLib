package com.montefiore.gaulthiergain.adhoclibrary.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocSocketBluetooth;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.service.ServiceClient;

import java.io.IOException;
import java.util.UUID;

/**
 * <p>This class defines the client's logic for bluetooth implementation. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class BluetoothServiceClient extends ServiceClient {

    /**
     * Constructor
     *
     * @param verbose         a boolean value to set the debug/verbose mode.
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param messageListener a messageListener object which serves as callback functions.
     * @param background      a boolean value which defines if the service must listen messages
     *                        to background.
     */
    public BluetoothServiceClient(boolean verbose, Context context, MessageListener messageListener, boolean background) {
        super(verbose, context, messageListener, background);
    }

    /**
     * Method allowing to connect to a remote bluetooth device.
     *
     * @param secure               a boolean value which represents the state of the connection.
     * @param bluetoothAdHocDevice a BluetoothAdHocDevice object which represents a remote Bluetooth
     *                             device.
     * @throws NoConnectionException Signals that a No Connection Exception exception has occurred.
     */
    public void connect(boolean secure, BluetoothAdHocDevice bluetoothAdHocDevice) throws NoConnectionException {
        if (v) Log.d(TAG, "connect to: " + bluetoothAdHocDevice.getDevice().getName());

        if (state == STATE_NONE) {

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

                // Notify handler
                String messageHandle[] = new String[3];
                messageHandle[0] = bluetoothSocket.getRemoteDevice().getName();
                messageHandle[1] = bluetoothSocket.getRemoteDevice().getAddress();
                messageHandle[2] = bluetoothAdHocDevice.getUuid();
                handler.obtainMessage(Service.CONNECTION_PERFORMED, messageHandle).sendToTarget();

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

}
