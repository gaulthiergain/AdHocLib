package com.montefiore.gaulthiergain.adhoclibrary.datalink.service;

import android.os.Parcelable;

import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;

public abstract class AbstractAdHocDevice {

    protected final String deviceAddress;
    protected final String deviceName;
    protected int type;

    public AbstractAdHocDevice(String deviceAddress, String deviceName, int type) {
        this.deviceAddress = deviceAddress;
        this.deviceName = deviceName;
        this.type = type;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public int getType() {
        return type;
    }

    protected String display(int type) {
        if (type == DataLinkManager.BLUETOOTH) {
            return "Bt";
        }

        return "Wifi";
    }
}
