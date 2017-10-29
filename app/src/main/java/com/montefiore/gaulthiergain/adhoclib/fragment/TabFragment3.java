package com.montefiore.gaulthiergain.adhoclib.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.montefiore.gaulthiergain.adhoclib.R;
import com.montefiore.gaulthiergain.adhoclib.wifi.WifiP2P;


public class TabFragment3 extends Fragment {

    private View fragmentView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment_tab_fragment3, container, false);
        Button button = fragmentView.findViewById(R.id.buttonDiscoveryWifi);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                WifiP2P wifiP2P = new WifiP2P(getContext());
                wifiP2P.discover();



            }
        });

        return fragmentView;
    }
}