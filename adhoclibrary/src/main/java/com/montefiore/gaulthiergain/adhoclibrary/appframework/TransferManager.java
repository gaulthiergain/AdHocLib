package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.AodvManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.AdHocDevice;

import java.io.IOException;
import java.util.HashMap;

public class TransferManager {

    private final boolean v;
    private final Context context;

    private Config config;
    private AodvManager aodvManager;
    private ListenerApp listenerApp;

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

    public void start() throws DeviceException, IOException {
        this.aodvManager = new AodvManager(v, context, config, listenerApp);
    }

    public void stopListening() throws IOException, DeviceException {
        aodvManager.stopListening();
    }

    public void connect(AdHocDevice adHocDevice) throws DeviceException {
        aodvManager.connect(adHocDevice);
    }

    public void connect(HashMap<String, AdHocDevice> hashMap) throws DeviceException {
        aodvManager.connect(hashMap);
    }

    public void sendMessageTo(Object msg, String remoteDest) throws IOException {
        aodvManager.sendMessageTo(msg, remoteDest);
    }

    public void discovery() throws DeviceException {
        aodvManager.discovery();
    }

    public HashMap<String, AdHocDevice> getPairedDevices(){
        return aodvManager.getPaired();
    }

    public void enable(int duration) {
        aodvManager.enable(duration);
    }

    public String getOwnAddress() {
        return config.getLabel();
    }

    public Config getConfig() {
        return config;
    }
}
