package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by gaulthiergain on 30/10/17.
 */

public class ServerTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "[AdHoc]";
    private Context context;
    private OnReceiveListener onReceiveListener;

    public ServerTask(Context context, OnReceiveListener onReceiveListener) {
        this.context = context;
        this.onReceiveListener = onReceiveListener;
    }

    @Override
    protected String doInBackground(Void... voids) {
        ServerSocket serverSocket = null;
        Socket client = null;
        try {
            Log.d(TAG, "Listening ... ");
             serverSocket = new ServerSocket(8988);
            Log.d(TAG, "Server: Socket opened");
             client = serverSocket.accept();
            Log.d(TAG, "Server: connection done");
            OutputStream outputStream;
            InputStream inputStream;
            int length = 0;
            do{
                inputStream = client.getInputStream();
                byte[] buffer = new byte[256];

                length = inputStream.read(buffer);
                if(length > 0){
                    Log.d(TAG, "Size length: " + length);
                    onReceiveListener.OnReceive(context, new String(buffer, 0, length));
                }
            }while(length > 0);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return null;
    }
}
