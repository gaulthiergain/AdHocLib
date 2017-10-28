package com.montefiore.gaulthiergain.adhoclib;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothAdHocDevice;
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

        final BluetoothAdHocDevice adHocDevice = intent.getParcelableExtra(BluetoothAdHocDevice.EXTRA_DEVICE);
        Log.d("[AdHoc]", "UUID: " + adHocDevice.getUuid());
        Log.d("[AdHoc]", "RSSI: " + adHocDevice.getRssi());
        Log.d("[AdHoc]", "DEVICE: " + adHocDevice.getDevice().getName());

        TextView textView = (TextView) findViewById(R.id.textView2);
        textView.setText(adHocDevice.toString());

        final Button buttonListen = (Button) findViewById(R.id.buttonListen);
        buttonListen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(!onClickListen){
                    // Listen on a particular thread
                    mListenThread = new BluetoothListenThread(true, "test", BluetoothAdapter.getDefaultAdapter()
                            , UUID.fromString(adHocDevice.getUuid()));
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
                    mConnectThread = new BluetoothConnectThread(adHocDevice.getDevice(), true);
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
