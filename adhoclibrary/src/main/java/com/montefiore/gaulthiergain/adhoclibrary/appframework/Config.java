package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.exceptions.BadServerPortException;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.exceptions.MaxThreadReachedException;

import java.util.UUID;

/**
 * <p>This class defines several different parameters to setup the library's behaviour. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class Config {

    private static final short MAX_THREAD = 8;
    private static final int MIN_PORT = 1023;
    private static final int MAX_PORT = 65535;

    private String label;
    private int timeout;
    private int serverPort;
    private short nbThreadBt;
    private short nbThreadWifi;
    private boolean json;
    private boolean secure;
    private boolean connectionFlooding;
    private boolean reliableTransportWifi;

    public Config() {
        this.json = true;
        this.secure = true;
        this.nbThreadBt = 7;
        this.nbThreadWifi = 10;
        this.timeout = 5000;
        this.serverPort = 52000;
        this.connectionFlooding = false;
        this.reliableTransportWifi = true;
        this.label = String.valueOf(UUID.randomUUID());
    }

    public Config(String label, boolean json, boolean secure, int serverPort,
                  short nbThreadBt, short nbThreadWifi, boolean reliableTransportWifi,
                  boolean connectionFlooding) {
        this.label = label;
        this.json = json;
        this.secure = secure;
        this.serverPort = serverPort;
        this.nbThreadBt = nbThreadBt;
        this.nbThreadWifi = nbThreadWifi;
        this.connectionFlooding = connectionFlooding;
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

    public int getTimeout() {
        return timeout;
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

    public boolean isConnectionFlooding() {
        return connectionFlooding;
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

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
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

    public void setReliableTransportWifi(boolean reliableTransportWifi) {
        this.reliableTransportWifi = reliableTransportWifi;
    }

    public void setJson(boolean json) {
        this.json = json;
    }

    public void setConnectionFlooding(boolean connectionFlooding) {
        this.connectionFlooding = connectionFlooding;
    }
}
