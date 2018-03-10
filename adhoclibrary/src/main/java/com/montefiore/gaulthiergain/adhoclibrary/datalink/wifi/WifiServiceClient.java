package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.connection.RemoteWifiConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceClient;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Random;

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
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param background      a boolean value which defines if the service must listen messages
     *                        to background.
     * @param remoteAddress
     * @param port
     * @param timeOut         an integer value which represents the timeout of a connection.
     * @param attempts        a short value which represents the number of attempts.
     * @param messageListener a messageListener object which serves as callback functions.
     */
    public WifiServiceClient(boolean verbose, Context context, boolean background,
                             String remoteAddress, int port, int timeOut, short attempts,
                             MessageListener messageListener) {

        super(verbose, context, attempts, messageListener, background);
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
                    handler.obtainMessage(Service.CATH_EXCEPTION,
                            new NoConnectionException(e.getMessage())).sendToTarget();
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

                network = new NetworkObject(new AdHocSocketWifi(socket));
                if (listenerAutoConnect != null) {
                    listenerAutoConnect.connected(remoteAddress, network);
                }

                // Notify handler
                handler.obtainMessage(Service.CONNECTION_PERFORMED,
                        new RemoteWifiConnection(socket.getRemoteSocketAddress().toString()
                                .split(":")[0].substring(1),
                                socket.getLocalAddress().toString().substring(1))).sendToTarget();

                // Update state
                setState(STATE_CONNECTED);

                // Listen in Background
                if (background) {
                    listenInBackground();
                }
            } catch (IOException e) {
                setState(STATE_NONE);
                throw new NoConnectionException("No remote connection to " + remoteAddress);
            }
        }
    }

    public void setListenerAutoConnect(WifiServiceClient.ListenerAutoConnect listenerAutoConnect) {
        this.listenerAutoConnect = listenerAutoConnect;
    }

    public interface ListenerAutoConnect {
        void connected(String remoteAddress, NetworkObject network) throws IOException, NoConnectionException;
    }
}
