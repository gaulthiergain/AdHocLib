package com.montefiore.gaulthiergain.adhoclib;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.montefiore.gaulthiergain.adhoclib.services.InfosInterface;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Click", Toast.LENGTH_SHORT).show();

                // Get infos Interface
                InfosInterface infosInterface = new InfosInterface();
                ArrayList<NetworkInterface> arrayNetworkInterfaces = infosInterface.getAllNetworkInterfaces();
                displayInfosNetInterface(arrayNetworkInterfaces);

            }
        });
    }

    /**
     * Display in console Infos Interface
     * @param arrayNetworkInterfaces array of network interfaces
     */
    void displayInfosNetInterface(ArrayList<NetworkInterface> arrayNetworkInterfaces) {
        for (int i = 0; i < arrayNetworkInterfaces.size(); i++) {
            Log.d("[AdHoc]", "Name: " + arrayNetworkInterfaces.get(i).getName());

            try {
                if (arrayNetworkInterfaces.get(i).getHardwareAddress() != null) {
                    Log.d("[AdHoc]", "MAC: " + new String(arrayNetworkInterfaces.get(i).getHardwareAddress(), StandardCharsets.UTF_8));
                } else {
                    Log.d("[AdHoc]", "MAC: /");
                }

                Log.d("[AdHoc]", "MTU: " + arrayNetworkInterfaces.get(i).getMTU());

                Enumeration<InetAddress> en = arrayNetworkInterfaces.get(i).getInetAddresses();
                while (en.hasMoreElements()) {
                    InetAddress addr = en.nextElement();
                    String s = addr.getHostAddress();
                    int end = s.lastIndexOf("%");
                    if (end > 0)
                        Log.d("[AdHoc]","\tAddr: " + s.substring(0, end));
                    else
                        Log.d("[AdHoc]","\tAddr: " + s);
                }

            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }
}
