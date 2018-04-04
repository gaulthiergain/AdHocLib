package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.bluetooth.BluetoothAdapter;

import java.util.UUID;

public class ServiceConfig {

    private short nbThreads;
    private int serverPort;

    private boolean secure;
    private BluetoothAdapter btAdapter;
    private UUID uuid;

    /**
     * Constructor (Wifi)
     *
     * @param nbThreads  a short value to determine the number of threads managed by the
     *                   server.
     * @param serverPort an integer value to set the listening port number.
     */
    public ServiceConfig(short nbThreads, int serverPort) {
        this.nbThreads = nbThreads;
        this.serverPort = serverPort;
    }

    /**
     * Constructor (Bluetooth)
     *
     * @param nbThreads a short value to determine the number of threads managed by the
     *                  server.
     * @param secure    a boolean value to determine if the connection is secure.
     * @param btAdapter a BluetoothAdapter object which represents the local device Bluetooth
     *                  adapter.
     * @param uuid      an UUID object which identify the physical device.
     */
    public ServiceConfig(short nbThreads, boolean secure, BluetoothAdapter btAdapter, UUID uuid) {
        this.nbThreads = nbThreads;
        this.secure = secure;
        this.btAdapter = btAdapter;
        this.uuid = uuid;
    }

    public short getNbThreads() {
        return nbThreads;
    }

    public int getServerPort() {
        return serverPort;
    }

    public boolean isSecure() {
        return secure;
    }

    public BluetoothAdapter getBtAdapter() {
        return btAdapter;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return "ServiceConfig{" +
                "nbThreads=" + nbThreads +
                ", serverPort=" + serverPort +
                ", secure=" + secure +
                ", bluetoothAdapter=" + btAdapter +
                ", uuid=" + uuid +
                '}';
    }
}
