package com.montefiore.gaulthiergain.adhoclib.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.montefiore.gaulthiergain.adhoclib.R;
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothManager;

public class TabFragment2 extends Fragment {

    private View fragmentView;
    private BluetoothManager bluetoothManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_tab_fragment2, container, false);

        Button button = fragmentView.findViewById(R.id.button2);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                BluetoothManager bluetoothManager = new BluetoothManager();
                if(bluetoothManager.activeBluetooth()){
                    Log.d("[AdHoc]", "Bluetooth is enabled");
                }else{
                    Log.d("[AdHoc]", "Bluetooth is disabled");
                }

            }
        });


        return fragmentView;
    }

}