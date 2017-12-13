package com.montefiore.gaulthiergain.adhoclibrary.auto;
/**
 * Created by gaulthiergain on 12/12/17.
 */

public interface ListenerGUI {
    void deviceDiscover(String deviceName, String deviceAddress);

    void deviceConnection(String deviceName, String deviceAddress);
}
