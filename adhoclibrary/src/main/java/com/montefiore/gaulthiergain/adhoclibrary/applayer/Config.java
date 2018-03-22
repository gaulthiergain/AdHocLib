package com.montefiore.gaulthiergain.adhoclibrary.applayer;

import com.montefiore.gaulthiergain.adhoclibrary.applayer.exceptions.BadServerPortException;
import com.montefiore.gaulthiergain.adhoclibrary.applayer.exceptions.MaxThreadReachedException;
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
    private boolean secure;
    private int serverPort;
    private short nbThreadBt;
    private short nbThreadWifi;
    private boolean reliableTransportWifi;

    public Config() {
        this.secure = true;
        this.nbThreadBt = 7;
        this.nbThreadWifi = 10;
        this.serverPort = 52000;
        this.reliableTransportWifi = true;
        this.name = BluetoothUtil.getCurrentName();
        this.label = String.valueOf(UUID.randomUUID());
    }

    public Config(boolean secure, int serverPort, int nbThreadBt, int nbThreadWifi, String label,
                  String name, boolean reliableTransportWifi)
            throws BadServerPortException, MaxThreadReachedException {
        this.name = name;
        this.label = label;
        this.secure = secure;
        this.setServerPort(serverPort);
        this.setNbThreadBt(nbThreadBt);
        this.nbThreadWifi = (short) nbThreadWifi;
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

    public boolean getSecure() {
        return secure;
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

    @Override
    public String toString() {
        return "Config{" +
                "name='" + name + '\'' +
                ", label='" + label + '\'' +
                ", secure=" + secure +
                ", serverPort=" + serverPort +
                ", nbThreadBt=" + nbThreadBt +
                ", nbThreadWifi=" + nbThreadWifi +
                ", reliableTransportWifi=" + reliableTransportWifi +
                '}';
    }
}
