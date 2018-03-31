package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.AodvManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;

import java.io.IOException;
import java.util.HashMap;

public class TransferManager {

    private final boolean v;
    private final Context context;

    private Config config;
    private AodvManager aodvManager;
    private ListenerApp listenerApp;
    private DataLinkManager dataLinkManager;

    private TransferManager(boolean verbose, Context context, final ListenerApp listenerApp,
                            Config config) {
        this.v = verbose;
        this.context = context;
        this.config = config;
        this.listenerApp = listenerApp;
    }

    public TransferManager(boolean verbose, Context context, final ListenerApp listenerApp) {
        this(verbose, context, listenerApp, new Config());
    }

    public TransferManager(boolean verbose, Context context, Config config,
                           final ListenerApp listenerApp) {
        this(verbose, context, listenerApp, config);
    }

    public void start() throws IOException {
        aodvManager = new AodvManager(v, context, config, listenerApp);
        dataLinkManager = aodvManager.getDataLink();
    }

    /*--------------------------------------Network methods---------------------------------------*/

    public void connect(AdHocDevice adHocDevice) throws DeviceException {
        aodvManager.connect(adHocDevice);
    }

    public void connect(HashMap<String, AdHocDevice> hashMap) throws DeviceException {
        aodvManager.connect(hashMap);
    }

    public void sendMessageTo(Object msg, String remoteDest) throws IOException {
        aodvManager.sendMessageTo(msg, remoteDest);
    }

    /*-------------------------------------DataLink methods---------------------------------------*/

    public void stopListening() throws IOException {
        dataLinkManager.stopListening();
    }

    public void discovery() throws DeviceException {
        dataLinkManager.discovery();
    }

    public HashMap<String, AdHocDevice> getPairedDevices() {
        return dataLinkManager.getPaired();
    }

    public void enableAll(ListenerAdapter listenerAdapter) {
        dataLinkManager.enableAll(listenerAdapter);
    }

    public void enableWifi(ListenerAdapter listenerAdapter) {
        dataLinkManager.enableWifi(listenerAdapter);
    }

    public void enableBluetooth(int duration, ListenerAdapter listenerAdapter) {
        dataLinkManager.enableBluetooth(duration, listenerAdapter);
    }

    public void disableAll() {
        dataLinkManager.disableAll();
    }

    public void disableWifi() {
        dataLinkManager.disableWifi();
    }

    public void disableBluetooth() throws IOException {
        dataLinkManager.disableBluetooth();
    }

    public boolean isWifiEnable() {
        return dataLinkManager.isWifiEnable();
    }

    public boolean isBluetoothEnable() {
        return dataLinkManager.isBluetoothEnable();
    }

    public void unregisterAdapter() {
        dataLinkManager.unregisterAdapter();
    }

    /*-----------------------------------------Getters--------------------------------------------*/

    public String getOwnAddress() {
        return config.getLabel();
    }

    public Config getConfig() {
        return config;
    }
}
