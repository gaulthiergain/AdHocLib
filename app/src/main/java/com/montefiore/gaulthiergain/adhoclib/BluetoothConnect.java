package com.montefiore.gaulthiergain.adhoclib;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.montefiore.gaulthiergain.adhoclib.bluetooth.AdHocBluetoothDevice;

public class BluetoothConnect extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        Intent intent = getIntent();

        String extra_addr = intent.getStringExtra(AdHocBluetoothDevice.EXTRA_ADDR);
        AdHocBluetoothDevice adHocBluetoothDevice = intent.getParcelableExtra(extra_addr);

        TextView textView = (TextView) findViewById(R.id.textView2);
        textView.setText("device: " + adHocBluetoothDevice.getName() + " - "
                + adHocBluetoothDevice.getAddress() + " - "
                + String.valueOf(adHocBluetoothDevice.getRssi()));

    }
}
