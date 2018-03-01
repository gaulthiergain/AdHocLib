package com.montefiore.gaulthiergain.adhoclibrary.wifi;

import android.net.wifi.p2p.WifiP2pManager;

import java.lang.reflect.Method;

/**
 * Created by gaulthiergain on 1/03/18.
 */

public class WifiUtil {
    public static void updateWifiName(WifiManager wifimngr, WifiP2pManager.Channel wifichannel, String new_name) {
        try {

            Method m = wifimngr.getClass().getMethod(
                    "setDeviceName",
                    WifiP2pManager.Channel.class, String.class,
                    WifiP2pManager.ActionListener.class);

            m.invoke(wifimngr, wifichannel, new_name, new WifiP2pManager.ActionListener() {
                public void onSuccess() {
                    //Code for Success in changing name
                }

                public void onFailure(int reason) {
                    //Code to be done while name change Fails
                }
            });
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
