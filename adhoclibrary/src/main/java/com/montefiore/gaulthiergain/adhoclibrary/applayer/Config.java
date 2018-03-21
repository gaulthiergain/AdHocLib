package com.montefiore.gaulthiergain.adhoclibrary.applayer;

import com.montefiore.gaulthiergain.adhoclibrary.applayer.exceptions.BadServerPortException;
import com.montefiore.gaulthiergain.adhoclibrary.applayer.exceptions.MaxThreadReachedException;

/**
 * Created by gaulthiergain on 21/03/18.
 */

public class Config {

    private static final short MAX_THREAD = 8;
    private static final int MIN_PORT = 1023;
    private static final int MAX_PORT = 65535;

    private boolean secure;
    private int serverPort;
    private short nbThreadBt;
    private short nbThreadWifi;

    public Config() {
        this.secure = true;
        this.serverPort = 52000;
        this.nbThreadBt = 7;
        this.nbThreadWifi = 10;
    }

    public Config(boolean secure, int serverPort, int nbThreadBt, int nbThreadWifi)
            throws BadServerPortException, MaxThreadReachedException {
        this.secure = secure;
        this.setServerPort(serverPort);
        this.setNbThreadBt(nbThreadBt);
        this.nbThreadWifi = (short) nbThreadWifi;
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

    @Override
    public String toString() {
        return "Config{" +
                "secure=" + secure +
                ", serverPort=" + serverPort +
                ", nbThreadBt=" + nbThreadBt +
                ", nbThreadWifi=" + nbThreadWifi +
                '}';
    }
}
