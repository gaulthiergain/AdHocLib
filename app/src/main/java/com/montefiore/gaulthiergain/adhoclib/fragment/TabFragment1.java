package com.montefiore.gaulthiergain.adhoclib.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.montefiore.gaulthiergain.adhoclib.R;
import com.montefiore.gaulthiergain.adhoclib.util.InfosInterface;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;

public class TabFragment1 extends Fragment {


    private View fragmentView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        fragmentView = inflater.inflate(R.layout.fragment_tab_fragment1, container, false);
        Button button = fragmentView.findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Log.d("[AdHoc]", "Get Infos Interfaces");

                // Get infos Interface
                InfosInterface infosInterface = new InfosInterface();
                ArrayList<NetworkInterface> arrayNetworkInterfaces = infosInterface.getAllNetworkInterfaces();
                displayScreenInfosNetInterface(arrayNetworkInterfaces);


            }
        });

        return fragmentView;
    }

    /**
     * Display in screen Infos Interface
     *
     * @param arrayNetworkInterfaces array of network interfaces
     */
    void displayScreenInfosNetInterface(ArrayList<NetworkInterface> arrayNetworkInterfaces) {

        StringBuilder txt = new StringBuilder();
        for (int i = 0; i < arrayNetworkInterfaces.size(); i++) {

            txt.append("Name: ").append(arrayNetworkInterfaces.get(i).getName()).append("\n");

            try {
                if (arrayNetworkInterfaces.get(i).getHardwareAddress() != null) {
                    txt.append("MAC: ").append(new String(arrayNetworkInterfaces.get(i).getHardwareAddress(), StandardCharsets.UTF_8)).append("\n");
                } else {
                    txt.append("MAC: /").append("\n");
                }

                txt.append("MTU: ").append(arrayNetworkInterfaces.get(i).getMTU()).append("\n");

                Enumeration<InetAddress> en = arrayNetworkInterfaces.get(i).getInetAddresses();
                while (en.hasMoreElements()) {
                    InetAddress addr = en.nextElement();
                    String s = addr.getHostAddress();
                    int end = s.lastIndexOf("%");
                    if (end > 0)
                        txt.append("\tAddr: ").append(s.substring(0, end)).append("\n");
                    else
                        txt.append("\tAddr: ").append(s).append("\n");
                }
                txt.append("\n");

            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        TextView textView = fragmentView.findViewById(R.id.textView1);
        textView.setText(txt.toString());

    }
}