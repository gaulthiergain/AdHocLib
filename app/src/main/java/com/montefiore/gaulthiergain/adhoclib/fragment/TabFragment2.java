package com.montefiore.gaulthiergain.adhoclib.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.montefiore.gaulthiergain.adhoclib.BluetoothConnect;
import com.montefiore.gaulthiergain.adhoclib.R;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.AdHocBluetoothDevice;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.OnDiscoveryCompleteListener;

import java.util.HashMap;

public class TabFragment2 extends Fragment {

    private BluetoothManager bluetoothManager;

    private void updateGUI(View fragmentView) {
        LinearLayout layout = fragmentView.findViewById(R.id.linearLayout);

        LinearLayout row = new LinearLayout(this.getContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        int i = 0;
        for (final AdHocBluetoothDevice device : bluetoothManager.getHashMapBluetoothDevice().values()) {

            final Button btnTag = new Button(this.getContext());
            btnTag.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            btnTag.setText(device.getName() + " - " + device.getAddress());
            btnTag.setId(i++);
            btnTag.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Toast.makeText(getContext(), btnTag.getText(), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(getContext(), BluetoothConnect.class);
                    intent.putExtra(AdHocBluetoothDevice.EXTRA_ADDR, device.getAddress());
                    intent.putExtra(device.getAddress(), (Parcelable) device);
                    bluetoothManager.unregisterDiscovery();
                    startActivity(intent);
                }
            });

            row.addView(btnTag);
        }
        layout.addView(row);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View fragmentView = inflater.inflate(R.layout.fragment_tab_fragment2, container, false);

        bluetoothManager = new BluetoothManager(getContext(), new OnDiscoveryCompleteListener() {
            public void OnDiscoveryComplete(HashMap<String, AdHocBluetoothDevice> hashMapBluetoothDevice) {
                if (bluetoothManager.getHashMapBluetoothDevice().size() != 0) {
                    updateGUI(fragmentView);
                }
            }
        });

        Button button = fragmentView.findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (bluetoothManager.isEnabled()) {

                    Log.d("[AdHoc]", "Bluetooth is enabled");

                    bluetoothManager.getPairedDevices();
                    bluetoothManager.discovery();
                } else {
                    Log.d("[AdHoc]", "Bluetooth is disabled");
                    bluetoothManager.enable();
                }

            }
        });

        return fragmentView;
    }

    @Override
    public void onDestroy() {
        Log.d("[AdHoc]", "On Destroy");
        super.onDestroy();
        bluetoothManager.unregisterDiscovery();
    }

}