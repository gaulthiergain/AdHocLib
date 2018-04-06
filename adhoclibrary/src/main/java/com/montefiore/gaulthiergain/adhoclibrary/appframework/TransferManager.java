package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wrappers.AbstractWrapper;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.AodvManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.DeviceAlreadyConnectedException;
import com.montefiore.gaulthiergain.adhoclibrary.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

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

    public void broadcast(Object msg) throws IOException {
        dataLinkManager.broadcast(new MessageAdHoc(
                new Header(AbstractWrapper.BROADCAST, config.getLabel(), config.getName()), msg));
    }

    public void broadcastExcept(Object msg, String excludedAddress) throws IOException {
        dataLinkManager.broadcastExcept(new MessageAdHoc(
                        new Header(AbstractWrapper.BROADCAST, config.getLabel(), config.getName()), msg),
                excludedAddress);
    }

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
        dataLinkManager.enable(0, Service.WIFI, listenerAdapter);
    }

    public void enableBluetooth(int duration, ListenerAdapter listenerAdapter) {
        dataLinkManager.enable(duration, Service.BLUETOOTH, listenerAdapter);
    }

    public void disableAll() throws IOException {
        dataLinkManager.disableAll();
    }

    public void disableWifi() throws IOException {
        dataLinkManager.disable(Service.WIFI);
    }

    public void disableBluetooth() throws IOException {
        dataLinkManager.disable(Service.BLUETOOTH);
    }

    public boolean isWifiEnable() {
        return dataLinkManager.isEnable(Service.WIFI);
    }

    public boolean isBluetoothEnable() {
        return dataLinkManager.isEnable(Service.BLUETOOTH);
    }

    public boolean updateBluetoothAdapterName(String newName) throws DeviceException {
        return dataLinkManager.updateAdapterName(Service.BLUETOOTH, newName);
    }

    public boolean updateWifiAdapterName(String newName) throws DeviceException {
        return dataLinkManager.updateAdapterName(Service.WIFI, newName);
    }

    public void resetBluetoothAdapterName() throws DeviceException {
        dataLinkManager.resetAdapterName(Service.BLUETOOTH);
    }

    public void resetWifiAdapterName() throws DeviceException {
        dataLinkManager.resetAdapterName(Service.WIFI);
    }

    public ArrayList<String> getActifAdapterNames() {
        return dataLinkManager.getActifAdapterNames();
    }

    public String getWifiAdapterName() {
        return dataLinkManager.getAdapterName(Service.WIFI);
    }

    public String getBluetoothAdapterName() {
        return dataLinkManager.getAdapterName(Service.BLUETOOTH);
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
