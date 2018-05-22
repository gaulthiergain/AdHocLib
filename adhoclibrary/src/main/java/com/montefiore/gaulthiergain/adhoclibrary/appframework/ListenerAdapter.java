package com.montefiore.gaulthiergain.adhoclibrary.appframework;

/**
 * <p>This interface allows to define callback functions to notify if the Bluetooth or Wi-FI adapter
 * has been enabled.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface ListenerAdapter {

    /**
     * Method allowing to notify if the Bluetooth adapter has been enabled.
     *
     * @param success true if the Bluetooth adapter is enabled, false otherwise.
     */
    void onEnableBluetooth(boolean success);

    /**
     * Method allowing to notify if the Wi-Fi adapter has been enabled.
     *
     * @param success true if the Wi-Fi adapter is enabled, false otherwise.
     */
    void onEnableWifi(boolean success);
}
