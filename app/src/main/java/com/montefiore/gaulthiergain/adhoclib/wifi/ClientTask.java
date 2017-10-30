package com.montefiore.gaulthiergain.adhoclib.wifi;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by gaulthiergain on 30/10/17.
 */

public class ClientTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "[AdHoc]";
    private Context context;
    private String addr;

    public ClientTask(Context context, String addr) {
        this.context = context;
        this.addr = addr;
    }

    @Override
    protected String doInBackground(Void... voids) {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(addr, 8988)), 5000);
            Log.d(TAG, "Client socket - " + socket.isConnected());
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
