package com.montefiore.gaulthiergain.adhoclibrary.applayer;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.AodvManager;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DiscoveredDevice;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ListenerAodv;

import java.io.IOException;
import java.util.HashMap;


public class TransferManager {

    private final boolean v;
    private final Context context;

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
    }

    public void start() throws DeviceException, IOException {
        this.aodvManager = new AodvManager(v, context, nbThreadWifi, serverPort, secure, nbThreadBt,
                listenerAodv);
    }

    public void stopListening() throws IOException, DeviceException {
        aodvManager.stopListening();
    }

    public void connect(HashMap<String, DiscoveredDevice> hashMap) throws DeviceException {
        aodvManager.connect(hashMap);
    }

    public void sendMessageTo(String msg, String remoteDest) throws IOException {
        aodvManager.sendMessageTo(msg, remoteDest);
    }

    public void discovery() throws DeviceException {
        aodvManager.discovery();
    }

    //todo remove
    public void setListenerAodv(ListenerAodv listenerAodv) {
        this.listenerAodv = listenerAodv;
    }
}
