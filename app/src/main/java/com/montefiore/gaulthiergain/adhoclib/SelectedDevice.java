package com.montefiore.gaulthiergain.adhoclib;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;

/**
 * Created by gaulthiergain on 29/03/18.
 */

public class SelectedDevice extends AdHocDevice {

    private boolean selected;

    public SelectedDevice(AdHocDevice device) {
        super(device.getMacAddress(), device.getDeviceName(), device.getType());
        this.selected = false;
    }

    public SelectedDevice(String address, String name, byte type) {
        super(address, name, type);
        this.selected = false;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return deviceName + " - " + macAddress + " - " + display(type);
    }
}
