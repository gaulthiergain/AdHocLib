package com.montefiore.gaulthiergain.adhoclibrary;

import android.content.Context;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Created by gaulthiergain on 25/10/17.
 *
 */

public class NetworkInfo {

    NetworkInfo(){

    }

    String getSsid(Context context){

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo;
        String ssid = null;

        wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            ssid = wifiInfo.getSSID();
        }
        return ssid;
    }
}
