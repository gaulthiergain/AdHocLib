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

import com.montefiore.gaulthiergain.adhoclib.R;
import com.montefiore.gaulthiergain.adhoclibrary.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;
import com.montefiore.gaulthiergain.adhoclibrary.wifi.WifiServiceClient;
import com.montefiore.gaulthiergain.adhoclibrary.wifi.WifiServiceServer;
import com.montefiore.gaulthiergain.adhoclibrary.wifiListener.ConnectionListener;
import com.montefiore.gaulthiergain.adhoclibrary.wifi.WifiManager;
import com.montefiore.gaulthiergain.adhoclibrary.wifiListener.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.wifiListener.WifiMessageListener;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;


public class TabFragment3 extends Fragment {

    private View fragmentView;
    private WifiManager wifiManager;
    private String TAG = "[AdHoc]";
    private final int PORT = 8888;

    private WifiServiceClient wifiServiceClient;
    private WifiServiceServer wifiServiceServer;

    private int nbReceive = 0;

    private HashMap<String, WifiP2pDevice> tmpDevices = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_tab_fragment3, container, false);

        wifiManager = new WifiManager(getContext(), true, new DiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {
                Log.d(TAG, "Discovery onSuccess");
            }

            @Override
            public void onDiscoveryFailed(int reasonCode) {
                Log.d(TAG, "Discovery failed. Retry." + reasonCode);
            }

            @Override
            public void onDiscoveryCompleted(HashMap<String, WifiP2pDevice> peers) {
                updateGUI(fragmentView, peers);
            }
        }
                , new ConnectionListener() {
            @Override
            public void onConnectionStarted() {
                Log.d(TAG, "Connect onSuccess");
            }

            @Override
            public void onConnectionFailed(int reasonCode) {
                Log.d(TAG, "Connect failed. Retry." + reasonCode);
            }

            @Override
            public void onGroupOwner(InetAddress groupOwnerAddr) {
                Log.d(TAG, "I am the groupOwner" + groupOwnerAddr.toString());
                addTextChat(fragmentView);
                wifiServiceServer = new WifiServiceServer(getContext(), true, new WifiMessageListener() {
                    @Override
                    public void onMessageReceived(MessageAdHoc message) {
                        Log.d(TAG, "onMessageReceived EVENT: " + message.getPdu());
                    }

                    @Override
                    public void onMessageSent(MessageAdHoc message) {
                        Log.d(TAG, "onMessageSent EVENT: " + message.getPdu());
                    }

                    @Override
                    public void onBroadcastSend(MessageAdHoc message) {
                        Log.d(TAG, "onBroadcastSend EVENT: " + message.getPdu());
                    }

                    @Override
                    public void onConnectionClosed(String deviceAddr) {
                        Log.d(TAG, "onConnectionClosed EVENT: " + deviceAddr);
                    }

                    @Override
                    public void onConnection(String deviceAddr) {
                        Log.d(TAG, "onConnection EVENT: " + deviceAddr);
                    }
                });
                try {
                    wifiServiceServer.listen(3, PORT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClient(InetAddress groupOwnerAddr) {
                addTextChat(fragmentView);
                Log.d(TAG, "The groupOwner is " + groupOwnerAddr.toString());
                wifiServiceClient = new WifiServiceClient(getContext(), true, new WifiMessageListener() {
                    @Override
                    public void onMessageReceived(MessageAdHoc message) {
                        Log.d(TAG, "onMessageReceived EVENT: " + message.getPdu());
                    }

                    @Override
                    public void onMessageSent(MessageAdHoc message) {
                        Log.d(TAG, "onMessageSent EVENT: " + message.getPdu());
                    }

                    @Override
                    public void onBroadcastSend(MessageAdHoc message) {
                        Log.d(TAG, "onBroadcastSend EVENT: " + message.getPdu());
                    }

                    @Override
                    public void onConnectionClosed(String deviceAddr) {
                        Log.d(TAG, "onConnectionClosed EVENT: " + deviceAddr);
                    }

                    @Override
                    public void onConnection(String deviceAddr) {
                        Log.d(TAG, "onConnection EVENT: " + deviceAddr);
                    }
                }, groupOwnerAddr.getHostAddress(), PORT);
                (new Thread(wifiServiceClient)).start();
            }
        });

        Button button = fragmentView.findViewById(R.id.buttonDiscoveryWifi);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                wifiManager.discover();

            }
        });


        return fragmentView;
    }


    private void updateGUI(final View fragmentView, final HashMap<String, WifiP2pDevice> peers) {
        LinearLayout layout = fragmentView.findViewById(R.id.linearLayout);

        int i = 0;
        for (final WifiP2pDevice wifiP2pDevice : peers.values()) {

            if (!tmpDevices.containsKey(wifiP2pDevice.deviceAddress)) {
                tmpDevices.put(wifiP2pDevice.deviceAddress, wifiP2pDevice);

                LinearLayout row = new LinearLayout(this.getContext());
                row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                final Button btnTag = new Button(this.getContext());
                btnTag.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                btnTag.setText(wifiP2pDevice.deviceName + " " + wifiP2pDevice.deviceAddress);
                btnTag.setId(i++);
                btnTag.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        // wifiManager.unregister();
                        wifiManager.connect(wifiP2pDevice.deviceAddress);
                        removeBtnGUI(fragmentView, peers);
                    }
                });

                row.addView(btnTag);
                layout.addView(row);
            }

        }
    }

    private void removeBtnGUI(View fragmentView, HashMap<String, WifiP2pDevice> peers) {
        int i = 0;
        for (final WifiP2pDevice wifiP2pDevice : peers.values()) {
            Button button = fragmentView.findViewById(i);
            ViewGroup layout = (ViewGroup) button.getParent();
            if (null != layout) //for safety only  as you are doing onClick
                layout.removeView(button);
        }

    }

    private void updateViewChat(final View fragmentView, String msg) {

        if (nbReceive == 0) {

            for (int i = 0; i < 10; i++) {
                Button button = fragmentView.findViewById(i);
                if (button != null) {
                    ViewGroup layout = (ViewGroup) button.getParent();
                    if (null != layout) //for safety only  as you are doing onClick
                        layout.removeView(button);
                }
            }

            LinearLayout layout = fragmentView.findViewById(R.id.linearLayout);

            int i = 10;

            TextView textView = new TextView(this.getContext()); // Pass it an Activity or Context
            textView.setId(i);
            textView.setText("--> " + msg);
            textView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            layout.addView(textView);
        } else {
            int i = 10;
            TextView textView = fragmentView.findViewById(i);
            textView.setText(textView.getText().toString() + "\n" + "--> " + msg);
        }
    }

    private void addTextChat(final View fragmentView) {
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
                // wifiManager.unregister();

                int i = 0;
                EditText myEditText = fragmentView.findViewById(i);
                try {
                    wifiServiceClient.send(new MessageAdHoc(new Header("Object", "TEST", "Test"), "SALUT  a tous"));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoConnectionException e) {
                    e.printStackTrace();
                }
            }
        });

        row.addView(btnTag);
        layout.addView(row);
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        wifiManager.unregister();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        wifiManager.unregister();
    }
}