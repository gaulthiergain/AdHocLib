package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by gaulthiergain on 30/10/17.
 */

public class ClientTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "[AdHoc]";
    private Context context;
    private String addr;
    private String message;

    public ClientTask(Context context, String addr, String msg) {
        this.context = context;
        this.addr = addr;
        this.message = msg;
    }

    @Override
    protected String doInBackground(Void... voids) {
        Socket socket = new Socket();
        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(addr, 8988)), 5000);
            Log.d(TAG, "Client socket - " + socket.isConnected());
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(message.getBytes());
            outputStream.flush();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
