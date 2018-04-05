package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.AodvManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.DeviceAlreadyConnectedException;

import java.io.IOException;
import java.util.ArrayList;
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


    public void sendMessageTo(Object msg, String remoteDest) throws IOException {
        aodvManager.sendMessageTo(msg, remoteDest);
    }

    //TODO BROADCAST and BRODCAST_EXCEPT

    /*-------------------------------------DataLink methods---------------------------------------*/

    public void connect(AdHocDevice adHocDevice) throws DeviceException, DeviceAlreadyConnectedException {
        dataLinkManager.connect(adHocDevice);
    }

    public void connect(HashMap<String, AdHocDevice> hashMap) throws DeviceException, DeviceAlreadyConnectedException {
        dataLinkManager.connect(hashMap);
    }

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
        dataLinkManager.enable(0, DataLinkManager.WIFI, listenerAdapter);
    }

    public void enableBluetooth(int duration, ListenerAdapter listenerAdapter) {
        dataLinkManager.enable(duration, DataLinkManager.BLUETOOTH, listenerAdapter);
    }

    public void disableAll() throws IOException {
        dataLinkManager.disableAll();
    }

    public void disableWifi() throws IOException {
        dataLinkManager.disable(DataLinkManager.WIFI);
    }

    public void disableBluetooth() throws IOException {
        dataLinkManager.disable(DataLinkManager.BLUETOOTH);
    }

    public boolean isWifiEnable() {
        return dataLinkManager.isEnable(DataLinkManager.WIFI);
    }

    public boolean isBluetoothEnable() {
        return dataLinkManager.isEnable(DataLinkManager.BLUETOOTH);
    }

    public boolean updateBluetoothName(String newName) throws DeviceException {
        return dataLinkManager.updateAdapterName(DataLinkManager.BLUETOOTH, newName);
    }

    public boolean updateWifiName(String newName) throws DeviceException {
        return dataLinkManager.updateAdapterName(DataLinkManager.WIFI, newName);
    }

    public void resetBluetoothName() throws DeviceException {
        dataLinkManager.resetAdapterName(DataLinkManager.BLUETOOTH);
    }

    public void resetWifiName() throws DeviceException {
        dataLinkManager.resetAdapterName(DataLinkManager.WIFI);
    }

    public ArrayList<String> getActifAdapterNames() {
        return dataLinkManager.getActifAdapterNames();
    }

    public void disconnectAll() throws IOException {
        dataLinkManager.disconnectAll();
    }

    public void disconnect(String remoteDest) throws IOException {
        dataLinkManager.disconnect(remoteDest);
    }

    /*-----------------------------------------Getters--------------------------------------------*/

    public String getOwnAddress() {
        return config.getLabel();
    }

    public String getOwnName() {
        return config.getName();
    }

    public Config getConfig() {
        return config;
    }


}
