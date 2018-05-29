package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.network.aodv.AodvManager;
import com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager.DataLinkManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>This class provide high-Level APIs to manage ad hoc networks and communications.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class TransferManager {

    protected final boolean v;
    protected final Config config;
    private AodvManager aodvManager;
    private ListenerApp listenerApp;
    private DataLinkManager dataLinkManager;

    /**
     * Constructor
     *
     * @param verbose     a boolean value to set the debug/verbose mode.
     * @param listenerApp a ListenerApp object which contains callback functions.
     * @param config      a Config object which contains specific configurations.
     */
    private TransferManager(boolean verbose, final ListenerApp listenerApp,
                            Config config) {
        this.v = verbose;
        this.config = config;
        this.listenerApp = listenerApp;
    }

    /**
     * Constructor
     *
     * @param verbose     a boolean value to set the debug/verbose mode.
     * @param listenerApp a ListenerApp object which contains callback functions.
     */
    public TransferManager(boolean verbose, final ListenerApp listenerApp) {
        this(verbose, listenerApp, new Config());
    }

    /**
     * Constructor
     *
     * @param verbose     a boolean value to set the debug/verbose mode.
     * @param config      a Config object which contains specific configurations.
     * @param listenerApp a ListenerApp object which contains callback functions.
     */
    public TransferManager(boolean verbose, Config config, final ListenerApp listenerApp) {
        this(verbose, listenerApp, config);
    }

    /**
     * Constructor
     *
     * @param verbose a boolean value to set the debug/verbose mode.
     */
    public TransferManager(boolean verbose) {
        this(verbose, null, new Config());
    }

    /**
     * Constructor
     *
     * @param verbose a boolean value to set the debug/verbose mode.
     * @param config  a Config object which contains specific configurations.
     */
    public TransferManager(boolean verbose, Config config) {
        this(verbose, null, config);
    }

    /**
     * Method allowing to initialize the library parameters. This method must be called directly
     * after the constructor if no configuration is performed. Otherwise, directly after the
     * configuration statements.
     *
     * @param context a Context object which gives global information about an application
     *                environment.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void start(Context context) throws IOException {
        aodvManager = new AodvManager(v, context, config, listenerApp);
        dataLinkManager = aodvManager.getDataLink();
    }

    /**
     * Method allowing to update the listenerApp.
     *
     * @param listenerApp a ListenerApp object which contains callback functions.
     */
    public void updateListenerApp(ListenerApp listenerApp) {
        this.listenerApp = listenerApp;
        this.aodvManager.updateListener(listenerApp);
    }

    /**
     * Method allowing to set the listenerApp.
     *
     * @param listenerApp a ListenerApp object which contains callback functions.
     */
    protected void setListenerApp(ListenerApp listenerApp) {
        this.listenerApp = listenerApp;
    }

    /**
     * Method allowing to update the current context.
     *
     * @param context a Context object which gives global information about an application
     *                environment.
     */
    public void updateContext(Context context) {
        this.dataLinkManager.updateContext(context);
    }

    /*--------------------------------------Network methods---------------------------------------*/

    /**
     * Method allowing to send a message to a remote node represented by the adHocDevice parameter.
     *
     * @param message     a generic object which represents the data to send through the network.
     * @param adHocDevice an AdHocDevice object which represents the remote peer to send message.
     * @throws IOException     signals that an I/O exception of some sort has occurred.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void sendMessageTo(Object message, AdHocDevice adHocDevice) throws IOException, DeviceException {

        if (dataLinkManager.checkState() == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        aodvManager.sendMessageTo(message, adHocDevice.getLabel());
    }

    /**
     * Method allowing to broadcast a message to all directly connected nodes.
     *
     * @param message a generic object which represents the data to send through the network.
     * @return a boolean value which is true if message has been broadcasted to direct neighbors. Otherwise, false.
     * @throws IOException     signals that an I/O exception of some sort has occurred.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public boolean broadcast(Object message) throws IOException, DeviceException {

        if (dataLinkManager.checkState() == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        return dataLinkManager.broadcast(message);
    }

    /**
     * Method allowing to broadcast a message to all directly connected nodes.
     *
     * @param message        a generic object which represents the data to send through the network.
     * @param excludedDevice a AdHocDevice object which represents the device that must be excluded
     *                       during the broadcast process.
     * @return a boolean value which is true if message has been broadcasted to direct neighbors. Otherwise, false.
     * @throws IOException     signals that an I/O exception of some sort has occurred.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public boolean broadcastExcept(Object message, AdHocDevice excludedDevice) throws IOException, DeviceException {

        if (dataLinkManager.checkState() == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        return dataLinkManager.broadcastExcept(message, excludedDevice.getLabel());
    }

    /*-------------------------------------DataLink methods---------------------------------------*/

    /**
     * Method allowing to connect to a remote peer represented by the adHocDevice parameter.
     *
     * @param attempts    an integer value which represents the number of attempts to try to connect
     *                    to the remote peer. Its initial default value is 1.
     * @param adHocDevice an AdHocDevice object which represents the remote peer to connect.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void connect(int attempts, AdHocDevice adHocDevice) throws DeviceException {

        if (dataLinkManager.checkState() == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        dataLinkManager.connect((short) attempts, adHocDevice);
    }

    /**
     * Method allowing to connect to a remote peer represented by the adHocDevice parameter.
     *
     * @param adHocDevice an AdHocDevice object which represents the remote peer to connect.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void connect(AdHocDevice adHocDevice) throws DeviceException {

        if (dataLinkManager.checkState() == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        dataLinkManager.connect((short) 1, adHocDevice);
    }

    /**
     * Method allowing to stop a listening on incoming connections.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void stopListening() throws IOException {
        dataLinkManager.stopListening();
    }

    /**
     * Method allowing to perform a discovery depending the technology used. If the Bluetooth and
     * Wi-Fi are enabled, the two discoveries are performed in parallel. A discovery stands for at
     * least 10/12 seconds.
     *
     * @param discoveryListener a DiscoveryListener object which contains callback function.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void discovery(DiscoveryListener discoveryListener) throws DeviceException {
        dataLinkManager.discovery(discoveryListener);
    }

    /**
     * Method allowing to get all the Bluetooth devices which are already paired.
     *
     * @return a HashMap<String, AdHocDevice> object which contains all paired Bluetooth devices.
     */
    public HashMap<String, AdHocDevice> getPairedBluetoothDevices() {
        return dataLinkManager.getPaired();
    }

    /**
     * Method allowing to get the direct neighbours of the current mobile.
     *
     * @return an ArrayList<AdHocDevice> object which represents the direct neighbours of the
     * current mobile.
     */
    public ArrayList<AdHocDevice> getDirectNeighbors() {
        return dataLinkManager.getDirectNeighbors();
    }

    /**
     * Method allowing to enable both Bluetooth and Wi-Fi technologies.
     *
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param listenerAdapter a ListenerAdapter object which contains callback functions.
     */
    public void enableAll(Context context, ListenerAdapter listenerAdapter) {
        try {
            dataLinkManager.enableAll(context, listenerAdapter);
        } catch (BluetoothBadDuration ignored) {
        }
    }

    /**
     * Method allowing to enable Wi-Fi adapter.
     *
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param listenerAdapter a ListenerAdapter object which contains callback functions.
     */
    public void enableWifi(Context context, ListenerAdapter listenerAdapter) {
        try {
            dataLinkManager.enable(0, context, Service.WIFI, listenerAdapter);
        } catch (BluetoothBadDuration ignored) {

        }
    }

    /**
     * Method allowing to enable Bluetooth adapter.
     *
     * @param duration        an integer value which is used to set up the time of the bluetooth
     *                        discovery.
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param listenerAdapter a ListenerAdapter object which contains callback functions.
     * @throws BluetoothBadDuration signals that the duration for the bluetooth discovery is invalid.
     */
    public void enableBluetooth(int duration, Context context, ListenerAdapter listenerAdapter)
            throws BluetoothBadDuration {
        dataLinkManager.enable(duration, context, Service.BLUETOOTH, listenerAdapter);
    }

    /**
     * Method allowing to disable all technologies.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void disableAll() throws IOException {
        dataLinkManager.disableAll();
    }

    /**
     * Method allowing to disable Wi-Fi adapter.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void disableWifi() throws IOException {
        dataLinkManager.disable(Service.WIFI);
    }

    /**
     * Method allowing to disable Bluetooth adapter.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void disableBluetooth() throws IOException {
        dataLinkManager.disable(Service.BLUETOOTH);
    }

    /**
     * Method allowing to check if Wi-Fi is enabled.
     *
     * @return a boolean value which is true if Wi-Fi is enabled. Otherwise, false.
     */
    public boolean isWifiEnabled() {
        return dataLinkManager.isEnabled(Service.WIFI);
    }

    /**
     * Method allowing to check if Bluetooth is enabled.
     *
     * @return a boolean value which is true if Bluetooth is enabled. Otherwise, false.
     */
    public boolean isBluetoothEnabled() {
        return dataLinkManager.isEnabled(Service.BLUETOOTH);
    }

    /**
     * Method allowing to update the name of the Bluetooth adapter.
     *
     * @param newName a String value which represents the new name of the device adapter.
     * @return a boolean value which is true if the name was correctly updated. Otherwise, false.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public boolean updateBluetoothAdapterName(String newName) throws DeviceException {
        return dataLinkManager.updateAdapterName(Service.BLUETOOTH, newName);
    }

    /**
     * Method allowing to update the name of the Wi-Fi adapter.
     *
     * @param newName a String value which represents the new name of the device adapter.
     * @return a boolean value which is true if the name was correctly updated. Otherwise, false.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public boolean updateWifiAdapterName(String newName) throws DeviceException {
        return dataLinkManager.updateAdapterName(Service.WIFI, newName);
    }

    /**
     * Method allowing to reset the name of the Wi-Fi adapter.
     *
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void resetBluetoothAdapterName() throws DeviceException {
        dataLinkManager.resetAdapterName(Service.BLUETOOTH);
    }

    /**
     * Method allowing to reset the name of the Bluetooth adapter.
     *
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void resetWifiAdapterName() throws DeviceException {
        dataLinkManager.resetAdapterName(Service.WIFI);
    }

    /**
     * Method allowing to update the Group Owner value to influence the choice of the Group Owner
     * negotiation.
     *
     * @param valueGroupOwner an integer value between 0 and 15 where 0 indicates the least
     *                        inclination to be a group owner and 15 indicates the highest inclination
     *                        to be a group owner. A value of -1 indicates the system can choose
     *                        an appropriate value.
     * @throws GroupOwnerBadValue signals that the value for the Group Owner intent is invalid.
     * @throws DeviceException    signals that a Device Exception exception has occurred.
     */
    public void setWifiGroupOwnerValue(int valueGroupOwner) throws GroupOwnerBadValue, DeviceException {
        dataLinkManager.setWifiGroupOwnerValue(valueGroupOwner);
    }

    /**
     * Method allowing to remove a current Wi-Fi group.
     *
     * @param listenerAction a ListenerAction object which contains callback functions.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void removeWifiGroup(ListenerAction listenerAction) throws DeviceException {
        dataLinkManager.removeGroup(listenerAction);
    }

    /**
     * Method allowing to check if the current device is the Group Owner.
     *
     * @return a boolean value which is true if the current device is the Group Owner. Otherwise, false.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public boolean isWifiGroupOwner() throws DeviceException {
        return dataLinkManager.isWifiGroupOwner();
    }

    /**
     * Method allowing to un-pair a bluetooth device.
     *
     * @param adHocDevice a BluetoothAdHocDevice object which represents a remote Bluetooth device.
     * @throws InvocationTargetException signals that a method does not exist.
     * @throws IllegalAccessException    signals that an application tries to reflectively create
     *                                   an instance which has no access to the definition of
     *                                   the specified class
     * @throws NoSuchMethodException     signals that a method does not exist.
     * @throws DeviceException           signals that a Device Exception exception has occurred.
     */
    public void unpairBtDevice(AdHocDevice adHocDevice)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, DeviceException {
        if (adHocDevice instanceof BluetoothAdHocDevice) {
            dataLinkManager.unpairDevice((BluetoothAdHocDevice) adHocDevice);
        } else {
            throw new DeviceException("Only bluetooth device can be unpaired");
        }
    }

    /**
     * Method allowing to cancel a Wi-Fi connection (during the Group Owner negotiation).
     *
     * @param listenerAction a ListenerAction object which contains callback functions.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void cancelConnection(ListenerAction listenerAction) throws DeviceException {
        dataLinkManager.cancelConnection(listenerAction);
    }

    /**
     * Method allowing to get the adapter's names.
     *
     * @return a HashMap<Integer, String> object which contains the name of all active adapters.
     */
    public HashMap<Integer, String> getActifAdapterNames() {
        return dataLinkManager.getActifAdapterNames();
    }

    /**
     * Method allowing to get the Wi-Fi adapter name.
     *
     * @return a String value which represents the Wi-Fi adapter name.
     */
    public String getWifiAdapterName() {
        return dataLinkManager.getAdapterName(Service.WIFI);
    }

    /**
     * Method allowing to get the Bluetooth adapter name.
     *
     * @return a String value which represents the Bluetooth adapter name.
     */
    public String getBluetoothAdapterName() {
        return dataLinkManager.getAdapterName(Service.BLUETOOTH);
    }

    /**
     * Method allowing to disconnect the mobile from all the remote peers.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void disconnectAll() throws IOException {
        dataLinkManager.disconnectAll();
    }

    /**
     * Method allowing to disconnect the mobile from a remote peer represented by the adHocDevice
     * parameter.
     *
     * @param adHocDevice an AdHocDevice object which represents the remote peer to disconnect.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void disconnect(AdHocDevice adHocDevice) throws IOException {
        dataLinkManager.disconnect(adHocDevice.getLabel());
    }

    /*-----------------------------------------Getters--------------------------------------------*/

    /**
     * Method allowing to get the current label that identifies uniquely the mobile.
     *
     * @return a String value which represents the label.
     */
    public String getOwnAddress() {
        return config.getLabel();
    }

    /**
     * Method allowing to get the current configuration used by the libraries.
     *
     * @return a Config object which represents the current configuration.
     */
    public Config getConfig() {
        return config;
    }
}
