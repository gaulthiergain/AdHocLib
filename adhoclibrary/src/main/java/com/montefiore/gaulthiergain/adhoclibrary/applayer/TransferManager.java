package com.montefiore.gaulthiergain.adhoclibrary.applayer;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothManager;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi.WifiAdHocManager;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.AodvManager;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DiscoveredDevice;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ListenerAodv;

import java.io.IOException;
import java.util.HashMap;


public class TransferManager {

    private final boolean v;
    private final Context context;

    private boolean wifiEnabled;
    private boolean bluetoothEnabled;
    private AodvManager aodvManager;

    private boolean secure;
    private int serverPort;
    private short nbThreadBt;
    private short nbThreadWifi;
    private ListenerAodv listenerAodv;

    public TransferManager(boolean verbose, Context context) {
        this.v = verbose;
        this.context = context;
        this.secure = true;
        this.serverPort = 52000;
        this.nbThreadBt = 7;
        this.nbThreadWifi = 10;
        this.checkConnectivity();
    }

    private void checkConnectivity() {

        // Check if bluetooth is enabled
        try {
            bluetoothEnabled = new BluetoothManager(v, context).isEnabled();
        } catch (DeviceException e) {
            bluetoothEnabled = false;
        }

        // Check if Wifi is enabled
        try {
            wifiEnabled = new WifiAdHocManager(v, context, null).isEnabled();
        } catch (DeviceException e) {
            wifiEnabled = false;
        }
    }

    public void start() throws DeviceException, IOException {
        if (bluetoothEnabled && wifiEnabled) {
            hybrid();
        } else if (bluetoothEnabled) {
            bluetooth();
        } else if (wifiEnabled) {
            wifi();
        } else {
            throw new DeviceException("No connectivity is enabled");
        }
    }

    public void stopListening() throws IOException {
        aodvManager.stopListening();
    }

    public void connect(HashMap<String, DiscoveredDevice> hashMap) {
        aodvManager.connect(hashMap);
    }

    public void sendMessageTo(String msg, String remoteDest) throws IOException {
        aodvManager.sendMessageTo(msg, remoteDest);
    }

    public void discovery() {
        aodvManager.discovery();
    }

    private void hybrid() throws DeviceException, IOException {
        this.aodvManager = new AodvManager(v, context, nbThreadWifi, serverPort, secure, nbThreadBt,
                listenerAodv);
    }

    private void wifi() throws DeviceException, IOException {
        this.aodvManager = new AodvManager(v, context, nbThreadWifi, serverPort, listenerAodv);

    }

    private void bluetooth() throws DeviceException, IOException {
        this.aodvManager = new AodvManager(v, context, secure, nbThreadBt, listenerAodv);
    }

    //todo remove
    public void setListenerAodv(ListenerAodv listenerAodv) {
        this.listenerAodv = listenerAodv;
    }
}
