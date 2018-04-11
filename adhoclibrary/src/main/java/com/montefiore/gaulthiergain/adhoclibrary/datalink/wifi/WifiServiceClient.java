package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceMessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.sockets.SocketManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * <p>This class defines the client's logic for bluetooth implementation. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class WifiServiceClient extends ServiceClient implements Runnable {

    private final int port;
    private final int timeOut;
    private final String remoteAddress;

    private ListenerAutoConnect listenerAutoConnect;

    /**
     * Constructor
     *
     * @param verbose         a boolean value to set the debug/verbose mode.
     * @param json            a boolean value to use json or bytes in network transfer.
     * @param background      a boolean value which defines if the service must listen messages
     *                        to background.
     * @param remoteAddress
     * @param port
     * @param timeOut         an integer value which represents the timeout of a connection.
     * @param attempts        a short value which represents the number of attempts.
     * @param serviceMessageListener a serviceMessageListener object which serves as callback functions.
     */
    public WifiServiceClient(boolean verbose, boolean json, boolean background,
                             String remoteAddress, int port, int timeOut, short attempts,
                             ServiceMessageListener serviceMessageListener) {

        super(verbose, attempts, json, background, serviceMessageListener);
        this.remoteAddress = remoteAddress;
        this.port = port;
        this.timeOut = timeOut;
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
                    Thread.sleep((getBackOffTime()));
                } catch (InterruptedException e1) {
                    handler.obtainMessage(Service.CONNECTION_FAILED, e1).sendToTarget();
                }
            }
        } while (i < this.attempts);
    }

    /**
     * Method allowing to connect to a remote bluetooth device.
     */
    public void connect() throws NoConnectionException {
        if (v) Log.d(TAG, "connect to: " + remoteAddress + ":" + port);

        if (state == STATE_NONE || state == STATE_CONNECTING) {

            // Change the state
            setState(STATE_CONNECTING);

            try {

                // Connect to the remote host
                Socket socket = new Socket();
                socket.bind(null);
                socket.connect((new InetSocketAddress(remoteAddress, port)), timeOut);

                network = new SocketManager(new AdHocSocketWifi(socket), json);
                if (listenerAutoConnect != null) {
                    listenerAutoConnect.connected(remoteAddress, network);
                }

                // Notify handler
                String remoteSocket = "";
                if (socket.getRemoteSocketAddress() != null) {
                    remoteSocket = socket.getRemoteSocketAddress().toString().split(":")[0].substring(1);
                }
                handler.obtainMessage(Service.CONNECTION_PERFORMED, remoteSocket).sendToTarget();

                // Update state
                setState(STATE_CONNECTED);

                // Listen in Background
                if (background) {
                    listenInBackground();
                }
            } catch (IOException e) {
                setState(STATE_NONE);
                throw new NoConnectionException("Unable to connect to " + remoteAddress);
            }
        }
    }

    public void setListenerAutoConnect(WifiServiceClient.ListenerAutoConnect listenerAutoConnect) {
        this.listenerAutoConnect = listenerAutoConnect;
    }

    public interface ListenerAutoConnect {
        void connected(String remoteAddress, SocketManager network) throws IOException, NoConnectionException;
    }
}
