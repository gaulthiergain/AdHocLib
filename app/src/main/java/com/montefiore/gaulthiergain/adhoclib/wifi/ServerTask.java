package com.montefiore.gaulthiergain.adhoclib.wifi;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by gaulthiergain on 30/10/17.
 */

public class ServerTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "[AdHoc]";
    private Context context;

    public ServerTask(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            Log.d(TAG, "Listening ... ");
            ServerSocket serverSocket = new ServerSocket(8988);
            Log.d(TAG, "Server: Socket opened");
            Socket client = serverSocket.accept();
            Log.d(TAG, "Server: connection done");
            client.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
