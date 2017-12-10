package com.montefiore.gaulthiergain.adhoclibrary.auto;

import android.net.wifi.p2p.WifiP2pDevice;

public class SmartWifiDevice extends SmartDevice {
    private WifiP2pDevice wifiP2pDevice;
    private int rssi;

    public SmartWifiDevice(WifiP2pDevice wifiP2pDevice, int rssi) {
        super(wifiP2pDevice.deviceName, wifiP2pDevice.deviceAddress);
        this.wifiP2pDevice = wifiP2pDevice;
        this.rssi = rssi;
    }
}
