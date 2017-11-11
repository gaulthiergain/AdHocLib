package com.montefiore.gaulthiergain.adhoclib;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothConnectThread;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothListenThread;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclib.threadPool.ListSocketDevice;
import com.montefiore.gaulthiergain.adhoclib.threadPool.ThreadServer;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnect extends AppCompatActivity {

    private boolean onClickConnect = false;
    private boolean onClickListen = false;

    public static final int MESSAGE_READ = 2;

    private ThreadServer threadListen;
    private BluetoothConnectThread mConnectThread;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        Intent intent = getIntent();
        final BluetoothAdHocDevice adHocDevice = intent.getParcelableExtra(BluetoothAdHocDevice.EXTRA_DEVICE);

        TextView textView = (TextView) findViewById(R.id.textView2);
        textView.setText(adHocDevice.toString());

        final Button buttonListen = (Button) findViewById(R.id.buttonListen);
        buttonListen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(!onClickListen){
                    // Listen on a particular thread
                    try {
                        threadListen = new ThreadServer(mHandler , 3, true, "test",
                                BluetoothAdapter.getDefaultAdapter(),
                                UUID.fromString("e0917680-d427-11e4-8830-" +
                                        BluetoothManager.getcurrentMac(
                                                getApplicationContext()).replace(":", "")), new ListSocketDevice());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    threadListen.start();
                    onClickListen = true;
                    buttonListen.setText(R.string.stop);
                }else{
                    try {
                        threadListen.cancel();
                    } catch (IOException e) {
                        Log.d("[AdHoc]", "C'est fini");
                        e.printStackTrace();
                    }
                    onClickListen = false;
                    buttonListen.setText(R.string.listen);
                }
            }
        });

        final Button buttonConnect = (Button) findViewById(R.id.buttonConnect);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(!onClickConnect){
                    // Start the thread to connect with the given device
                    mConnectThread = new BluetoothConnectThread(mHandler , adHocDevice.getDevice(),
                            true,
                            UUID.fromString(adHocDevice.getUuid()));
                    mConnectThread.start();

                    onClickConnect = true;
                    buttonConnect.setText(R.string.stop);

                }else{
                    mConnectThread.cancel();
                    onClickConnect = false;
                    buttonConnect.setText(R.string.connect_to);
                }
            }
        });


        /*final Button buttonChat = (Button) findViewById(R.id.buttonChat);
        buttonChat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText editTextChat = (EditText) findViewById(R.id.editTextChat);
                try {
                    threadListen.sendMessage(editTextChat.getText().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });*/

    }

    // The Handler that gets information back from the BluetoothChatService
    @SuppressLint("HandlerLeak")
    private final  Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d("[AdHoc]", "WITHIN handleMessage" + msg);
            switch (msg.what) {

                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    // Update GUI
                    final TextView textViewChat = (TextView) findViewById(R.id.textViewChat);
                    textViewChat.setText(textViewChat.getText() + "\n------> " + readMessage);
                    break;
            }
        }
    };
}
