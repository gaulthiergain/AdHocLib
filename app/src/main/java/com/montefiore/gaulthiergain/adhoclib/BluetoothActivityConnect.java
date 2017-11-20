package com.montefiore.gaulthiergain.adhoclib;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.bluetoothListener.MessageListener;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.UUID;

public class BluetoothActivityConnect extends AppCompatActivity {

    private static final String TAG = "[AdHoc]";
    private boolean onClickConnect = false;
    private boolean onClickListen = false;

    private boolean server;

    private BluetoothServiceServer bluetoothServiceServer;
    private BluetoothServiceClient bluetoothServiceClient;

    BluetoothAdHocDevice adHocDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        Intent intent = getIntent();
        adHocDevice = intent.getParcelableExtra(BluetoothAdHocDevice.EXTRA_DEVICE);

        TextView textView = (TextView) findViewById(R.id.textView2);
        textView.setText(adHocDevice.toString());

        final Button buttonListen = (Button) findViewById(R.id.buttonListen);
        buttonListen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                server = true;

                if (!onClickListen) {
                    // Listen on a particular thread
                    try {
                        bluetoothServiceServer = new BluetoothServiceServer(getApplicationContext(), true, new MessageListener() {
                            @Override
                            public void onMessageReceived(MessageAdHoc message) {
                                //Update GUI
                                final TextView textViewChat = (TextView) findViewById(R.id.textViewChat);
                                textViewChat.setText(textViewChat.getText() + "\n------> " + message.getHeader().getSenderName() + ":" + message);
                                Log.d(TAG, "Sender: " + message.getPdu().toString());
                            }

                            @Override
                            public void onMessageSent(MessageAdHoc message) {

                            }

                            @Override
                            public void onBroadcastSend(MessageAdHoc message) {

                            }

                            @Override
                            public void onConnectionClosed(String deviceName, String deviceAddr) {

                            }

                            @Override
                            public void onConnection(String deviceName, String deviceAddr) {

                            }

                        });
                        bluetoothServiceServer.listen(3, true, "test",
                                BluetoothAdapter.getDefaultAdapter(),
                                UUID.fromString("e0917680-d427-11e4-8830-" +
                                        BluetoothManager.getCurrentMac(
                                                getApplicationContext()).replace(":", "")));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    onClickListen = true;
                    buttonListen.setText(R.string.stop);
                } else {
                    try {
                        bluetoothServiceServer.stopListening();
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

                server = false;

                if (!onClickConnect) {
                    // Start the thread to connect with the given device
                    bluetoothServiceClient = new BluetoothServiceClient(getApplicationContext(), true, new MessageListener() {

                        @Override
                        public void onMessageReceived(MessageAdHoc message) {

                        }

                        @Override
                        public void onMessageSent(MessageAdHoc message) {
                            Log.d(TAG, "Message sent " + message.getPdu());
                        }

                        @Override
                        public void onBroadcastSend(MessageAdHoc message) {

                        }

                        @Override
                        public void onConnectionClosed(String deviceName, String deviceAddr) {

                        }

                        @Override
                        public void onConnection(String deviceName, String deviceAddr) {

                        }
                    });
                    try {
                        bluetoothServiceClient.connect(true, adHocDevice);
                        bluetoothServiceClient.listenInBackground();
                    } catch (NoConnectionException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    onClickConnect = true;
                    buttonConnect.setText(R.string.stop);

                } else {
                    try {
                        bluetoothServiceClient.disconnect();
                    } catch (NoConnectionException e) {
                        e.printStackTrace();
                    }
                    onClickConnect = false;
                    buttonConnect.setText(R.string.connect_to);
                }
            }
        });


        final Button buttonChat = (Button) findViewById(R.id.buttonChat);
        buttonChat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final EditText editTextChat = (EditText) findViewById(R.id.editTextChat);


                if (!server) {
                    try {
                        String msg = "";
                        for (int i = 0; i < 200; i++) {
                            msg = "VALUE :" + i;
                            bluetoothServiceClient.send(new MessageAdHoc(
                                    new Header("Object", BluetoothManager.getCurrentMac(getApplicationContext()),
                                            BluetoothManager.getCurrentName()), msg));
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NoConnectionException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }
}
