package com.montefiore.gaulthiergain.adhoclib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothConnectThread;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothListenThread;

import java.util.UUID;

public class BluetoothConnect extends AppCompatActivity {

    private boolean onClickConnect = false;
    private boolean onClickListen = false;

    private BluetoothListenThread mListenThread;
    private BluetoothConnectThread mConnectThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        Intent intent = getIntent();

        String extra_addr = intent.getStringExtra(BluetoothDevice.EXTRA_CLASS);
        final BluetoothDevice device = intent.getParcelableExtra(extra_addr);

        TextView textView = (TextView) findViewById(R.id.textView2);
        textView.setText("device: " + device.getName() + " - "
                + device.getAddress());

        final Button buttonListen = (Button) findViewById(R.id.buttonListen);
        buttonListen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(!onClickListen){
                    // Listen on a particular thread
                    mListenThread = new BluetoothListenThread(true, "test", BluetoothAdapter.getDefaultAdapter()
                            , UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"));
                    mListenThread.start();
                    onClickListen = true;
                    buttonListen.setText(R.string.stop);
                }else{
                    mListenThread.cancel();
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
                    mConnectThread = new BluetoothConnectThread(device, true);
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


        final Button buttonChat = (Button) findViewById(R.id.buttonChat);
        buttonChat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


            }
        });




    }
}
