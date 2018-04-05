package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.exceptions.BadServerPortException;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.exceptions.MaxThreadReachedException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothUtil;

import java.util.UUID;

/**
 * Created by gaulthiergain on 21/03/18.
 */

public class Config {

    private static final short MAX_THREAD = 8;
    private static final int MIN_PORT = 1023;
    private static final int MAX_PORT = 65535;

    private String name;
    private String label;
    private boolean json;
    private boolean secure;
    private int serverPort;
    private short nbThreadBt;
    private short nbThreadWifi;
    private boolean background;
    private boolean reliableTransportWifi;

    public Config() {
        this.json = true;
        this.secure = true;
        this.nbThreadBt = 7;
        this.nbThreadWifi = 10;
        this.serverPort = 52000;
        this.background = true;
        this.reliableTransportWifi = true;
        this.name = BluetoothUtil.getCurrentName();
        this.label = String.valueOf(UUID.randomUUID());
    }

    public Config(String name, String label, boolean json, boolean secure, int serverPort,
                  short nbThreadBt, short nbThreadWifi, boolean background, boolean reliableTransportWifi) {
        this.name = name;
        this.label = label;
        this.json = json;
        this.secure = secure;
        this.serverPort = serverPort;
        this.nbThreadBt = nbThreadBt;
        this.nbThreadWifi = nbThreadWifi;
        this.background = background;
        this.reliableTransportWifi = reliableTransportWifi;
    }

    public Config(boolean secure, int nbThreadBt) throws MaxThreadReachedException {
        this();
        this.secure = secure;
        this.setNbThreadBt(nbThreadBt);
    }

    public Config(int serverPort, int nbThreadWifi) throws BadServerPortException {
        this();
        this.setServerPort(serverPort);
        this.nbThreadWifi = (short) nbThreadWifi;
    }

    public Config(int serverPort, int nbThreadWifi, boolean reliableTransportWifi)
            throws BadServerPortException {
        this();
        this.setServerPort(serverPort);
        this.nbThreadWifi = (short) nbThreadWifi;
        this.reliableTransportWifi = reliableTransportWifi;
    }

    public int getServerPort() {
        return serverPort;
    }

    public short getNbThreadBt() {
        return nbThreadBt;
    }

    public short getNbThreadWifi() {
        return nbThreadWifi;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public boolean isReliableTransportWifi() {
        return reliableTransportWifi;
    }

    public boolean isSecure() {
        return secure;
    }

    public boolean isJson() {
        return json;
    }

    public boolean isBackground() {
        return background;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public void setServerPort(int serverPort) throws BadServerPortException {
        if (serverPort <= MIN_PORT || serverPort >= MAX_PORT) {
            throw new BadServerPortException("The server port must be in range ["
                    + (MIN_PORT + 1) + " , " + (MAX_PORT - 1) + "]");
        } else {
            this.serverPort = serverPort;
        }
    }

    public void setNbThreadBt(int nbThreadBt) throws MaxThreadReachedException {

        if (nbThreadBt >= MAX_THREAD) {
            throw new MaxThreadReachedException("Number of threads must be smaller than " + MAX_THREAD);
        } else {
            this.nbThreadBt = (short) nbThreadBt;
        }
    }

    public void setNbThreadWifi(int nbThreadWifi) {
        this.nbThreadWifi = (short) nbThreadWifi;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setReliableTransportWifi(boolean reliableTransportWifi) {
        this.reliableTransportWifi = reliableTransportWifi;
    }

    public void setJson(boolean json) {
        this.json = json;
    }

    public void setBackground(boolean background) {
        this.background = background;
    }

}
