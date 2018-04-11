package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.AodvManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.exceptions.DeviceAlreadyConnectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class TransferManager {

    protected final boolean v;

    protected Config config;
    private AodvManager aodvManager;
    private ListenerApp listenerApp;
    private DataLinkManager dataLinkManager;

    private TransferManager(boolean verbose, final ListenerApp listenerApp,
                            Config config) {
        this.v = verbose;
        this.config = config;
        this.listenerApp = listenerApp;
    }

    public TransferManager(boolean verbose, final ListenerApp listenerApp) {
        this(verbose, listenerApp, new Config());
    }

    public TransferManager(boolean verbose, Config config, final ListenerApp listenerApp) {
        this(verbose, listenerApp, config);
    }

    public void start(Context context) throws IOException {
        aodvManager = new AodvManager(v, context, config, listenerApp);
        dataLinkManager = aodvManager.getDataLink();
    }

    public TransferManager(boolean verbose) {
        this(verbose, null, new Config());
    }

    public TransferManager(boolean verbose, Config config) {
        this(verbose, null, config);
    }

    public void updateListenerApp(ListenerApp listenerApp) {
        this.listenerApp = listenerApp;
        this.aodvManager.updateListener(listenerApp);
    }

    protected void setListenerApp(ListenerApp listenerApp) {
        this.listenerApp = listenerApp;
    }

    public void updateContext(Context context) {
        this.dataLinkManager.updateContext(context);
    }

    /*--------------------------------------Network methods---------------------------------------*/


    public void sendMessageTo(Object msg, AdHocDevice adHocDevice) throws IOException {
        aodvManager.sendMessageTo(msg, adHocDevice.getLabel());
    }

    public void broadcast(Object object) throws IOException {
        dataLinkManager.broadcast(object);
    }

    public void broadcastExcept(Object object, AdHocDevice excludedDevice) throws IOException {
        dataLinkManager.broadcastExcept(object, excludedDevice.getLabel());
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

    public void enableAll(Context context, ListenerAdapter listenerAdapter) {
        try {
            dataLinkManager.enableAll(context, listenerAdapter);
        } catch (BluetoothBadDuration ignored) {
        }
    }

    public void enableWifi(Context context, ListenerAdapter listenerAdapter) {
        try {
            dataLinkManager.enable(0, context, Service.WIFI, listenerAdapter);
        } catch (BluetoothBadDuration ignored) {

        }
    }

    public void enableBluetooth(int duration, Context context, ListenerAdapter listenerAdapter)
            throws BluetoothBadDuration {
        dataLinkManager.enable(duration, context, Service.BLUETOOTH, listenerAdapter);
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

    public void setWifiGroupOwnerValue(int valueGroupOwner) throws GroupOwnerBadValue {
        dataLinkManager.setWifiGroupOwnerValue(valueGroupOwner);
    }

    public void removeGroup(ListenerAction listenerAction) {
        dataLinkManager.removeGroup(listenerAction);
    }

    public void cancelConnection(ListenerAction listenerAction) {
        dataLinkManager.cancelConnection(listenerAction);
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

    public void disconnect(AdHocDevice adHocDevice) throws IOException {
        dataLinkManager.disconnect(adHocDevice.getLabel());
    }

    /*-----------------------------------------Getters--------------------------------------------*/

    public String getOwnAddress() {
        return config.getLabel();
    }

    public Config getConfig() {
        return config;
    }
}
