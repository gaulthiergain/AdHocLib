package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.ServiceClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;
import java.util.UUID;

/**
 * <p>This class defines the client's logic for bluetooth implementation. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class WifiServiceClient extends ServiceClient implements Runnable {

    //todo refactor this? (backoff algorithm)
    private static final int LOW = 500;
    private static final int HIGH = 2500;

    private final int port;
    private final int timeOut;
    private final int attempts;
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
     * @param messageListener a messageListener object which serves as callback functions.
     * @param timeOut         an integer value which represents the timeout of a connection.
     */
    public WifiServiceClient(boolean verbose, Context context, boolean background,
                             String remoteAddress, int port, int timeOut, int attempts,
                             MessageListener messageListener) {

        super(verbose, context, messageListener, background);
        this.remoteAddress = remoteAddress;
        this.port = port;
        this.timeOut = timeOut;
        this.attempts = attempts;
    }


    @Override
    public void run() {
        int i = 0;
        do {
            try {
                connect();
                i = attempts;
            } catch (SocketTimeoutException e) {
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

    /**
     * Method allowing to connect to a remote bluetooth device.
     */
    public void connect() throws SocketTimeoutException {
        if (v) Log.d(TAG, "connect to: " + remoteAddress + ":" + port);

        if (state == STATE_NONE) {

            // Change the state
            setState(STATE_CONNECTING);

            try {
                // Connect to the remote host
                Socket socket = new Socket();
                socket.bind(null);
                socket.connect((new InetSocketAddress(remoteAddress, port)), timeOut);

                network = new NetworkObject(new AdHocSocketWifi(socket));
                if(listenerAutoConnect != null){
                    listenerAutoConnect.connected(remoteAddress, network);
                }

                // Notify handler
                String[] messageHandle = new String[3];
                messageHandle[0] = "name"; //TODO
                // Set remote address
                messageHandle[1] = socket.getRemoteSocketAddress().toString()
                        .split(":")[0].substring(1);
                // Set local address
                messageHandle[2] = socket.getLocalAddress().toString().substring(1);
                handler.obtainMessage(Service.CONNECTION_PERFORMED, messageHandle).sendToTarget();

                // Update state
                setState(STATE_CONNECTED);

                // Listen in Background
                if (background) {
                    listenInBackground();
                }
            } catch (IOException e) {
                setState(STATE_NONE);
                e.printStackTrace();
            } catch (NoConnectionException e) {
                e.printStackTrace();
            }
            //TODO run() exception
        }
    }

    public void setListenerAutoConnect(WifiServiceClient.ListenerAutoConnect listenerAutoConnect) {
        this.listenerAutoConnect = listenerAutoConnect;
    }

    public interface ListenerAutoConnect {
        void connected(String remoteAddress, NetworkObject network) throws IOException, NoConnectionException;
    }
}
