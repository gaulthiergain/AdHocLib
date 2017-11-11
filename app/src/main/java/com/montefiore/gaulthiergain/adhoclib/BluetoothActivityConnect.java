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
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothServiceClient;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothServiceServer;

import java.io.IOException;
import java.util.UUID;

public class BluetoothActivityConnect extends AppCompatActivity {

    private boolean onClickConnect = false;
    private boolean onClickListen = false;

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

                if (!onClickListen) {
                    // Listen on a particular thread
                    try {
                        bluetoothServiceServer = new BluetoothServiceServer(getApplicationContext(), true);
                        bluetoothServiceServer.listen(3, true, "test",
                                BluetoothAdapter.getDefaultAdapter(),
                                UUID.fromString("e0917680-d427-11e4-8830-" +
                                        BluetoothManager.getcurrentMac(
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

                if (!onClickConnect) {
                    // Start the thread to connect with the given device
                    bluetoothServiceClient = new BluetoothServiceClient(getApplicationContext(), true);
                    bluetoothServiceClient.connect(true, adHocDevice);

                    onClickConnect = true;
                    buttonConnect.setText(R.string.stop);

                } else {
                    bluetoothServiceClient.cancel();
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
                    bluetoothService.sendMessage(editTextChat.getText().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });*/

    }

    // The Handler that gets information back from the BluetoothChatService

}
