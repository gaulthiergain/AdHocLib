package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.service.ServiceClient;

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
    public WifiServiceClient(Context context, boolean verbose, MessageListener messageListener,
                             boolean background, String remoteAddress, int port, int timeOut) {
        super(verbose, context, messageListener, background);
        this.remoteAddress = remoteAddress;
        this.port = port;
        this.timeOut = timeOut;
    }

    /**
     * Method allowing to connect to a remote bluetooth device.
     */
    @Override
    public void run() {
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
}
