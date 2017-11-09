package com.montefiore.gaulthiergain.adhoclib.fragment;


import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.montefiore.gaulthiergain.adhoclib.BluetoothConnect;
import com.montefiore.gaulthiergain.adhoclib.R;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclib.bluetoothListener.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclib.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclib.exceptions.BluetoothDeviceException;

import java.util.HashMap;

public class TabFragment2 extends Fragment {

    private final String TAG = "[AdHoc]";
    private BluetoothManager bluetoothManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View fragmentView = inflater.inflate(R.layout.fragment_tab_fragment2, container, false);

        try {
            bluetoothManager = new BluetoothManager(getContext(), true);
        } catch (BluetoothDeviceException e) {
            e.printStackTrace();
        }

        Button button = fragmentView.findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    bluetoothManager.enableDiscovery(10);
                } catch (BluetoothBadDuration bluetoothBadDuration) {
                    bluetoothBadDuration.printStackTrace();
                }

                if (bluetoothManager.isEnabled()) {
                    Log.d(TAG, "Bluetooth is enabled");
                    bluetoothManager.getPairedDevices();
                    bluetoothManager.discovery(new ConnectionListener() {
                        @Override
                        public void onDiscoveryFinished(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice) {
                            if (bluetoothManager.getHashMapBluetoothDevice().size() != 0) {
                                updateGUI(fragmentView);
                            }
                        }

                        @Override
                        public void onDiscoveryStarted() {
                            Log.d(TAG, "EVENT: onDiscoveryStarted()");
                        }

                        @Override
                        public void onDeviceFound(BluetoothDevice device) {
                            Log.d(TAG, "EVENT: onDeviceFound() " + device.getName());
                        }

                        @Override
                        public void onScanModeChange(int currentMode, int oldMode) {
                            Log.d(TAG, "EVENT: onScanModeChange() " + String.valueOf(currentMode) + " " + String.valueOf(oldMode));
                        }

                    });
                } else {
                    Log.d(TAG, "Bluetooth is disabled");
                    bluetoothManager.enable();
                }
            }
        });

        return fragmentView;
    }

    private void updateGUI(View fragmentView) {
        LinearLayout layout = fragmentView.findViewById(R.id.linearLayout);

        int i = 0;
        for (final BluetoothAdHocDevice adHocDevice : bluetoothManager.getHashMapBluetoothDevice().values()) {

            LinearLayout row = new LinearLayout(this.getContext());
            row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            final Button btnTag = new Button(this.getContext());
            btnTag.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            btnTag.setText(adHocDevice.toString());
            btnTag.setId(i++);
            btnTag.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), BluetoothConnect.class);
                    intent.putExtra(BluetoothAdHocDevice.EXTRA_DEVICE, adHocDevice);
                    bluetoothManager.unregisterDiscovery();
                    startActivity(intent);
                }
            });

            row.addView(btnTag);
            layout.addView(row);
        }
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        //bluetoothManager.unregisterDiscovery();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        bluetoothManager.unregisterDiscovery();
    }
}