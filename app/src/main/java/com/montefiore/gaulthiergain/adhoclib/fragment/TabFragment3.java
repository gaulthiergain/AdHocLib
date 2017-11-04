package com.montefiore.gaulthiergain.adhoclib.fragment;


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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.montefiore.gaulthiergain.adhoclib.R;
import com.montefiore.gaulthiergain.adhoclib.wifi.ClientTask;
import com.montefiore.gaulthiergain.adhoclib.wifi.OnDiscoveryCompleteListener;
import com.montefiore.gaulthiergain.adhoclib.wifi.WifiP2P;

import java.util.HashMap;


public class TabFragment3 extends Fragment {

    private View fragmentView;
    private WifiP2P wifiP2P;
    private String TAG = "[AdHoc]";
    private String addr;

    private int nbReceive = 0;

    private HashMap<String, WifiP2pDevice> tmpDevices = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_tab_fragment3, container, false);

        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case 1:
                        String msg = (String) message.obj;
                        Log.d(TAG, "String rcv: " + msg);
                        updateViewChat(fragmentView, msg);
                        nbReceive++;
                        break;
                    case 2: //Connected
                        addr = (String) message.obj;
                        Log.d(TAG, "YEAH it's connected: " + addr);
                        addTextChat(fragmentView);
                    //new ClientTask(context, info.groupOwnerAddress.getHostAddress()).execute();
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



    private void updateGUI(final View fragmentView, final HashMap<String, WifiP2pDevice> peers) {
        LinearLayout layout = fragmentView.findViewById(R.id.linearLayout);

        int i = 0;
        for (final WifiP2pDevice wifiP2pDevice : peers.values()) {

            if(!tmpDevices.containsKey(wifiP2pDevice.deviceAddress)){
                tmpDevices.put(wifiP2pDevice.deviceAddress, wifiP2pDevice);

                LinearLayout row = new LinearLayout(this.getContext());
                row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                final Button btnTag = new Button(this.getContext());
                btnTag.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                btnTag.setText(wifiP2pDevice.deviceName + " " + wifiP2pDevice.deviceAddress);
                btnTag.setId(i++);
                btnTag.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // wifiP2P.unregister();
                        wifiP2P.connect(wifiP2pDevice.deviceAddress);
                        removeBtnGUI(fragmentView, peers);
                    }
                });

                row.addView(btnTag);
                layout.addView(row);
            }

        }
    }

    private void removeBtnGUI(View fragmentView, HashMap<String, WifiP2pDevice> peers){
        int i = 0;
        for (final WifiP2pDevice wifiP2pDevice : peers.values()) {
            Button button = fragmentView.findViewById(i);
            ViewGroup layout = (ViewGroup) button.getParent();
            if(null!=layout) //for safety only  as you are doing onClick
                layout.removeView(button);
        }

    }

    private void updateViewChat(final View fragmentView, String msg){

        if(nbReceive == 0){

            for(int i = 0; i < 10; i++){
                Button button = fragmentView.findViewById(i);
                if(button != null){
                    ViewGroup layout = (ViewGroup) button.getParent();
                    if(null!=layout) //for safety only  as you are doing onClick
                        layout.removeView(button);
                }
            }

            LinearLayout layout = fragmentView.findViewById(R.id.linearLayout);

            int i = 10;

            TextView textView = new TextView(this.getContext()); // Pass it an Activity or Context
            textView.setId(i);
            textView.setText("--> " +msg);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            layout.addView(textView);
        }else{
            int i = 10;
            TextView textView = fragmentView.findViewById(i);
            textView.setText(textView.getText().toString() + "\n" + "--> " + msg);
        }
    }

    private void addTextChat(final View fragmentView){
        LinearLayout layout = fragmentView.findViewById(R.id.linearLayout);

        LinearLayout row = new LinearLayout(this.getContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        int i = 0;

        EditText myEditText = new EditText(this.getContext()); // Pass it an Activity or Context
        myEditText.setId(i++);
        myEditText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        layout.addView(myEditText);

        final Button btnTag = new Button(this.getContext());
        btnTag.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        btnTag.setText("CHAT");
        btnTag.setId(i);
        btnTag.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // wifiP2P.unregister();

                int i = 0;
                EditText myEditText = fragmentView.findViewById(i);
                new ClientTask(getContext(), addr, myEditText.getText().toString()).execute();
            }
        });

        row.addView(btnTag);
        layout.addView(row);
    }
}