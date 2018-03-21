package com.montefiore.gaulthiergain.adhoclibrary.applayer;

/**
 * Created by gaulthiergain on 21/03/18.
 */

public class Config {
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

    public Config(boolean secure, int serverPort, int nbThreadBt, int nbThreadWifi) {
        this.secure = secure;
        this.serverPort = serverPort;
        this.nbThreadBt = (short) nbThreadBt;
        this.nbThreadWifi = (short) nbThreadWifi;
    }

    public Config(boolean secure, int nbThreadBt) {
        this();
        this.secure = secure;
        this.nbThreadBt = (short) nbThreadBt;
    }

    public Config(int serverPort, int nbThreadWifi) {
        this();
        this.serverPort = serverPort;
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

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public void setNbThreadBt(short nbThreadBt) {
        this.nbThreadBt = nbThreadBt;
    }

    public void setNbThreadWifi(short nbThreadWifi) {
        this.nbThreadWifi = nbThreadWifi;
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
