package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.content.Context;
import android.util.Log;

import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.network.AdHocSocketWifi;
import com.montefiore.gaulthiergain.adhoclibrary.network.NetworkObject;
import com.montefiore.gaulthiergain.adhoclibrary.service.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.service.ServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by gaulthiergain on 28/10/17.
 */

public class WifiServiceClient extends ServiceClient implements Runnable {

    private final int port;
    private final boolean background;
    private final String remoteAddr;


    public WifiServiceClient(Context context, boolean verbose, boolean background, MessageListener messageListener,
                             String remoteAddr, int port) {
        super(verbose, context, messageListener, background);
        this.remoteAddr = remoteAddr;
        this.background = background;
        this.port = port;
    }

    @Override
    public void run() {
        if (v) Log.d(TAG, "connect to: " + remoteAddr + ":" + port);

        if (state == STATE_NONE) {

            // Change the state
            setState(STATE_CONNECTING);

            try {
                // Connect to the remote host
                Socket socket = new Socket();
                socket.bind(null);
                socket.connect((new InetSocketAddress(remoteAddr, port)), 5000);

                network = new NetworkObject(new AdHocSocketWifi(socket));

                // Notify handler
                String[] messageHandle = new String[2];
                // Set remote address
                messageHandle[0] = socket.getRemoteSocketAddress().toString().split(":")[0].substring(1);
                // Set local address
                messageHandle[1] = socket.getLocalAddress().toString().substring(1);
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
        }
    }
}
