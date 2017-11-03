package com.montefiore.gaulthiergain.adhoclib.fragment;


import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclib.wifi.OnDiscoveryCompleteListener;
import com.montefiore.gaulthiergain.adhoclib.wifi.WifiP2P;

import java.net.Socket;
import java.util.HashMap;


public class TabFragment3 extends Fragment {

    private View fragmentView;
    private WifiP2P wifiP2P;
    private String TAG = "[AdHoc]";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_tab_fragment3, container, false);

        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        Log.d(TAG, "String rcv: " + message.obj);
                        Toast.makeText(getContext(), (String) message.obj, Toast.LENGTH_LONG);
                        break;
                    default:
                        Log.d(TAG, "error");
                }

            }
        };

        wifiP2P = new WifiP2P(getContext(), mHandler, new OnDiscoveryCompleteListener() {
            @Override
            public void OnDiscoveryComplete(HashMap<String, WifiP2pDevice> peers) {
                updateGUI(fragmentView, peers);
            }
        });

        Button button = fragmentView.findViewById(R.id.buttonDiscoveryWifi);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                wifiP2P.discover();

            }
        });


        return fragmentView;
    }


    private void updateGUI(View fragmentView, HashMap<String, WifiP2pDevice> peers) {
        LinearLayout layout = fragmentView.findViewById(R.id.linearLayout);

        int i = 0;
        for (final WifiP2pDevice wifiP2pDevice : peers.values()) {

            LinearLayout row = new LinearLayout(this.getContext());
            row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            final Button btnTag = new Button(this.getContext());
            btnTag.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            btnTag.setText(wifiP2pDevice.deviceName + " " + wifiP2pDevice.deviceAddress);
            btnTag.setId(i++);
            btnTag.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                 //   Intent intent = new Intent(getContext(), BluetoothConnect.class);
                 //   intent.putExtra(, wifiP2pDevice);
                 //   startActivity(intent);
                }
            });

            row.addView(btnTag);
            layout.addView(row);
        }
    }
}