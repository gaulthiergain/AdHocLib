package com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkwrappers;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.NoConnectionException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.aodv.ListenerDataLinkAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ActiveConnections;
import com.montefiore.gaulthiergain.adhoclibrary.routing.datalinkmanager.ListenerAodv;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownDestException;
import com.montefiore.gaulthiergain.adhoclibrary.routing.exceptions.AodvUnknownTypeException;
import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

public abstract class AbstractWrapper {

    final static int ATTEMPTS = 3;

    final boolean v;
    final Context context;

    final ListenerAodv listenerAodv;
    final ActiveConnections activeConnections;
    final ListenerDataLinkAodv listenerDataLinkAodv;

    String ownMac;
    String ownName;

    AbstractWrapper(boolean v, Context context, ActiveConnections activeConnections,
                    ListenerAodv listenerAodv, ListenerDataLinkAodv listenerDataLinkAodv) {

        this.v = v;
        this.context = context;
        this.listenerAodv = listenerAodv;
        this.activeConnections = activeConnections;
        this.listenerDataLinkAodv = listenerDataLinkAodv;

    }

    abstract void listenServer(short nbThreadsWifi) throws IOException;

    abstract void discovery();

    abstract void connect();

    abstract void processMsgReceived(MessageAdHoc message) throws IOException, NoConnectionException,
            AodvUnknownTypeException, AodvUnknownDestException;

    abstract void stopListening() throws IOException;
}
