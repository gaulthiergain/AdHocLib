package com.montefiore.gaulthiergain.adhoclibrary.applayer;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.RemoteConnection;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.AodvManager;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.DiscoveredDevice;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ListenerAodv;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;
import java.util.HashMap;

public class TransferManager {

    private final boolean v;
    private final Context context;

    private Config config;
    private AodvManager aodvManager;
    private ListenerAodv listenerAodv;

    private TransferManager(boolean verbose, Context context, final ListenerApp listenerApp,
                            Config config) {
        this.v = verbose;
        this.context = context;
        this.config = config;
        this.listenerAodv = new ListenerAodv() {
            @Override
            public void onDiscoveryCompleted(HashMap<String, DiscoveredDevice> mapAddressDevice) {
                listenerApp.onDiscoveryCompleted(mapAddressDevice);
            }

            @Override
            public void onPairedCompleted() {
                listenerApp.onPairedCompleted();
            }

            @Override
            public void receivedRREQ(MessageAdHoc message) {
                listenerApp.receivedDATA(message);
            }

            @Override
            public void receivedRREP(MessageAdHoc message) {
                listenerApp.receivedDATA(message);
            }

            @Override
            public void receivedRREP_GRAT(MessageAdHoc message) {
                listenerApp.receivedDATA(message);
            }

            @Override
            public void receivedRERR(MessageAdHoc message) {
                listenerApp.receivedDATA(message);
            }

            @Override
            public void receivedDATA(MessageAdHoc message) {
                listenerApp.receivedDATA(message.getPdu());
            }

            @Override
            public void timerExpiredRREQ(String destAddr, int retry) {

            }

            @Override
            public void catchException(Exception e) {
                e.printStackTrace();
                if (!(e instanceof IOException)) {
                    listenerApp.catchException(e);
                }
            }

            @Override
            public void onConnectionClosed(RemoteConnection remoteDevice) {
                listenerApp.onConnectionClosed(remoteDevice.getDeviceName(), remoteDevice.getDeviceAddress());
            }

            @Override
            public void onConnection(RemoteConnection remoteDevice) {
                listenerApp.onConnection(remoteDevice.getDeviceName(), remoteDevice.getDeviceAddress());
            }
        };
    }


    public TransferManager(boolean verbose, Context context, final ListenerApp listenerApp) {
        this(verbose, context, listenerApp, new Config());
    }

    public TransferManager(boolean verbose, Context context, Config config,
                           final ListenerApp listenerApp) {
        this(verbose, context, listenerApp, config);
    }

    public void start() throws DeviceException, IOException {
        this.aodvManager = new AodvManager(v, context, config, listenerAodv);
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
}
