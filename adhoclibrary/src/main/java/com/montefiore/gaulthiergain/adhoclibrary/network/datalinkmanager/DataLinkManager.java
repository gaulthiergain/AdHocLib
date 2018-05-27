package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.Config;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAction;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAdapter;
import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerApp;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.bluetooth.BluetoothAdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.BluetoothBadDuration;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.DeviceException;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.DiscoveryListener;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.Service;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.Header;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>This class acts as an intermediary sub-layer between the lower layer and the sub-layer related
 * to the routing protocol. It chooses which wrapper to use to transmit data.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class DataLinkManager {

    private static final int POOLING_DISCOVERY = 1000;
    private static final short NB_WRAPPERS = 2;

    private Config config;
    private final AbstractWrapper wrappers[];
    private final HashMap<String, AdHocDevice> mapAddressDevice;

    /**
     * Constructor
     *
     * @param verbose          a boolean value to set the debug/verbose mode.
     * @param context          a Context object which gives global information about an application
     *                         environment.
     * @param config           a Config object which contains specific configurations.
     * @param listenerApp      a ListenerApp object which contains callback functions.
     * @param listenerDataLink a listenerDataLink object which contains callback functions.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public DataLinkManager(boolean verbose, Context context, Config config,
                           final ListenerApp listenerApp, final ListenerDataLink listenerDataLink)
            throws IOException {

        this.config = config;
        this.mapAddressDevice = new HashMap<>();
        this.wrappers = new AbstractWrapper[NB_WRAPPERS];

        if (config.isReliableTransportWifi()) {
            // TCP connection
            this.wrappers[Service.WIFI] = new WrapperWifi(verbose, context, config, mapAddressDevice,
                    listenerApp, listenerDataLink);
        } else {
            // UDP stream
            this.wrappers[Service.WIFI] = new WrapperWifiUdp(verbose, context, config, mapAddressDevice,
                    listenerApp, listenerDataLink);
        }


        this.wrappers[Service.BLUETOOTH] = new WrapperBluetooth(verbose, context, config, mapAddressDevice,
                listenerApp, listenerDataLink);

        // Check if data link communications are enabled (0 : all is disabled)
        checkState();
    }

    /**
     * Method allowing to perform a discovery depending the technology used. If the Bluetooth and
     * Wi-Fi is enabled, the two discoveries are performed in parallel. A discovery stands for at
     * least 10/12 seconds.
     *
     * @param discovery a DiscoveryListener object which contains callback function.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void discovery(final DiscoveryListener discovery) throws DeviceException {

        int enabled = checkState();
        if (enabled == 0) {
            throw new DeviceException("No wifi and bluetooth connectivity");
        }

        if (enabled == wrappers.length) {
            // Both data link communications are enabled
            bothDiscovery(discovery);
        } else {
            // Discovery one by one depending their status
            for (AbstractWrapper wrapper : wrappers) {
                if (wrapper.isEnabled()) {
                    wrapper.setDiscoveryListener(new ListenerBothDiscovery() {
                        @Override
                        public void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice) {
                            discovery.onDiscoveryCompleted(mapAddressDevice);
                        }
                    });
                    wrapper.discovery(discovery);
                }
            }
        }
    }

    /**
     * Method allowing to connect to a remote peer.
     *
     * @param attempts    an integer value which represents the number of attempts to try to connect
     *                    to the remote peer.
     * @param adHocDevice an AdHocDevice object which represents the remote peer.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void connect(short attempts, AdHocDevice adHocDevice) throws DeviceException {

        switch (adHocDevice.getType()) {
            case Service.WIFI:
                wrappers[Service.WIFI].connect(attempts, adHocDevice);
                break;
            case Service.BLUETOOTH:
                wrappers[Service.BLUETOOTH].connect(attempts, adHocDevice);
                break;
        }
    }

    /**
     * Method allowing to stop a listening on incoming connections.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void stopListening() throws IOException {

        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.stopListening();
                wrapper.unregisterConnection();
            }
        }
    }

    /**
     * Method allowing to send a message to a remote peer.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @param address a String value which represents the address of the remote device.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void sendMessage(MessageAdHoc message, String address) throws IOException {

        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.sendMessage(message, address);
            }
        }
    }

    /**
     * Method allowing to check if a node is a direct neighbour.
     *
     * @param address a String value which represents the address of the remote device.
     * @return a boolean value which is true if the device is a direct neighbors. Otherwise, false.
     */
    public boolean isDirectNeighbors(String address) {

        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                if (wrapper.isDirectNeighbors(address)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Method allowing to broadcast a message to all directly connected nodes.
     *
     * @param message a MessageAdHoc object which represents the message to send through
     *                the network.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void broadcast(MessageAdHoc message) throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.broadcast(message);
            }
        }
    }

    /**
     * Method allowing to broadcast a message to all directly connected nodes.
     *
     * @param object a generic object used which will be encapsulated into a MessageAdHoc object.
     * @return a boolean value which is true if the broadcast was successful. Otherwise, false.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public boolean broadcast(Object object) throws IOException {
        boolean sent = false;
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                Header header = new Header(AbstractWrapper.BROADCAST, wrapper.getMac(),
                        config.getLabel(), wrapper.getAdapterName(), wrapper.getType());
                if (wrapper.broadcast(new MessageAdHoc(header, object))) {
                    sent = true;
                }
            }
        }
        return sent;
    }

    /**
     * Method allowing to broadcast a message to all directly connected nodes excepted the excluded
     * node.
     *
     * @param message         a MessageAdHoc object which represents the message to send through
     *                        the network.
     * @param excludedAddress a String value which represents the excluded address.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void broadcastExcept(MessageAdHoc message, String excludedAddress) throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.broadcastExcept(message, excludedAddress);
            }
        }
    }

    /**
     * Method allowing to broadcast a message to all directly connected nodes excepted the excluded
     * node.
     *
     * @param object          a generic object used which will be encapsulated into a MessageAdHoc object.
     * @param excludedAddress a String value which represents the excluded address.
     * @return a boolean value which is true if the broadcast was successful. Otherwise, false.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public boolean broadcastExcept(Object object, String excludedAddress) throws IOException {

        boolean sent = false;
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                Header header = new Header(AbstractWrapper.BROADCAST, wrapper.getMac(),
                        config.getLabel(), wrapper.getAdapterName(), wrapper.getType());
                if (wrapper.broadcastExcept(new MessageAdHoc(header, object), excludedAddress)) {
                    sent = true;
                }
            }
        }
        return sent;
    }

    /**
     * Method allowing to get all the Bluetooth devices which are already paired.
     *
     * @return a HashMap<String, AdHocDevice> object which contains all paired Bluetooth devices.
     */
    public HashMap<String, AdHocDevice> getPaired() {
        if (wrappers[Service.BLUETOOTH].isEnabled()) {
            return wrappers[Service.BLUETOOTH].getPaired();
        }
        return null;
    }

    /**
     * Method allowing to enable both Bluetooth and Wi-Fi technologies.
     *
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param listenerAdapter a ListenerAdapter object which contains callback functions.
     * @throws BluetoothBadDuration signals that the duration for the bluetooth discovery is invalid.
     */
    public void enableAll(Context context, final ListenerAdapter listenerAdapter) throws BluetoothBadDuration {
        for (AbstractWrapper wrapper : wrappers) {
            enable(0, context, wrapper.getType(), listenerAdapter);
        }
    }

    /**
     * Method allowing to enabled a particular technology depending the input type.
     *
     * @param duration        an integer value which is used to set up the time of the bluetooth discovery.
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param type            an integer value which represents the type of the wrapper. 1: Bluetooth and 0: Wi-Fi
     * @param listenerAdapter a ListenerAdapter object which contains callback functions.
     * @throws BluetoothBadDuration signals that the duration for the bluetooth discovery is invalid.
     */
    public void enable(int duration, final Context context, final int type,
                       final ListenerAdapter listenerAdapter) throws BluetoothBadDuration {

        if (!wrappers[type].isEnabled()) {
            wrappers[type].enable(context, duration, new ListenerAdapter() {
                @Override
                public void onEnableBluetooth(boolean success) {
                    processListenerAdapter(type, success, context, listenerAdapter);
                }

                @Override
                public void onEnableWifi(boolean success) {
                    processListenerAdapter(type, success, context, listenerAdapter);
                }
            });
        }
    }

    /**
     * Method allowing to disable all technologies.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void disableAll() throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                disable(wrapper.getType());
            }
        }
    }

    /**
     * Method allowing to disable a particular technology depending the input type.
     *
     * @param type an integer value which represents the type of the wrapper. 1: Bluetooth and 0: Wi-Fi
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void disable(int type) throws IOException {
        if (wrappers[type].isEnabled()) {
            wrappers[type].stopListening();
            wrappers[type].disable();
        }
    }

    /**
     * Method allowing to get the direct neighbours of the current mobile.
     *
     * @return an ArrayList<AdHocDevice> object which represents the direct neighbours of the current mobile.
     */
    public ArrayList<AdHocDevice> getDirectNeighbors() {

        ArrayList<AdHocDevice> adHocDevices = new ArrayList<>();
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                adHocDevices.addAll(wrapper.getDirectNeighbors());
            }
        }

        return adHocDevices;
    }

    /**
     * Method allowing to check if a particular technology is enabled depending the input type.
     *
     * @param type an integer value which represents the type of the wrapper. 1: Bluetooth and 0: Wi-Fi
     * @return a boolean value which is true is enabled. Otherwise, false.
     */
    public boolean isEnabled(int type) {
        return wrappers[type].isEnabled();
    }

    /**
     * Method allowing to update the name of a particular technology depending the input type.
     *
     * @param type    an integer value which represents the type of the wrapper. 1: Bluetooth and 0: Wi-Fi
     * @param newName a String value which represents the new name of the device adapter.
     * @return a boolean value which is true if the name was correctly updated. Otherwise, false.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public boolean updateAdapterName(int type, String newName) throws DeviceException {
        if (wrappers[type].isEnabled()) {
            return wrappers[type].updateDeviceName(newName);
        } else {
            throw new DeviceException(getTypeString(type) + " adapter is not enabled");
        }
    }

    /**
     * Method allowing to reset the adapter name of a particular technology depending the input type.
     *
     * @param type an integer value which represents the type of the wrapper. 1: Bluetooth and 0: Wi-Fi
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void resetAdapterName(int type) throws DeviceException {
        if (wrappers[type].isEnabled()) {
            wrappers[type].resetDeviceName();
        } else {
            throw new DeviceException(getTypeString(type) + " adapter is not enabled");
        }
    }

    /**
     * Method allowing to disconnect the mobile from a remote mobile.
     *
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void disconnectAll() throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.disconnectAll();
            }
        }
    }

    /**
     * Method allowing to disconnect the mobile from a a particular destination.
     *
     * @param remoteDest a String value which represents the current address of the destination.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public void disconnect(String remoteDest) throws IOException {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.disconnect(remoteDest);
            }
        }
    }

    /**
     * Method allowing to get the adapter's names.
     *
     * @return a HashMap<Integer, String> object which contains the name of all active adapters.
     */
    public HashMap<Integer, String> getActifAdapterNames() {
        @SuppressLint("UseSparseArrays") HashMap<Integer, String> adapterNames = new HashMap<>();
        for (AbstractWrapper wrapper : wrappers) {
            String name = getAdapterName(wrapper.getType());
            if (name != null) {
                adapterNames.put(wrapper.getType(), name);
            }
        }

        return adapterNames;
    }

    /**
     * Method allowing to get the a particular adapter name depending the input type.
     *
     * @param type an integer value which represents the type of the wrapper. 1: Bluetooth and 0: Wi-Fi
     * @return a String value which represents the adapter name depending the input type.
     */
    public String getAdapterName(int type) {
        if (wrappers[type].isEnabled()) {
            return wrappers[type].getAdapterName();
        }
        return null;
    }

    /**
     * Method allowing to update the current context.
     *
     * @param context a Context object which gives global information about an application
     *                environment.
     */
    public void updateContext(Context context) {
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                wrapper.updateContext(context);
            }
        }
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
    public void unpairDevice(BluetoothAdHocDevice adHocDevice)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, DeviceException {
        WrapperBluetooth wrapperBt = (WrapperBluetooth) wrappers[Service.BLUETOOTH];
        if (wrapperBt.isEnabled()) {
            wrapperBt.unpairDevice(adHocDevice);
        } else {
            throw new DeviceException("Bluetooth is not enabled");
        }
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
        IWrapperWifi wrapperWifi = (IWrapperWifi) wrappers[Service.WIFI];
        if (wrapperWifi.isEnabled()) {
            wrapperWifi.setGroupOwnerValue(valueGroupOwner);
        } else {
            throw new DeviceException("Wifi is not enabled");
        }
    }

    /**
     * Method allowing to remove a current Wi-Fi group.
     *
     * @param listenerAction a ListenerAction object which contains callback functions.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void removeGroup(ListenerAction listenerAction) throws DeviceException {
        IWrapperWifi wrapperWifi = (IWrapperWifi) wrappers[Service.WIFI];
        if (wrapperWifi.isEnabled()) {
            wrapperWifi.removeGroup(listenerAction);
        } else {
            throw new DeviceException("Wifi is not enabled");
        }
    }

    /**
     * Method allowing to check if the current device is the Group Owner.
     *
     * @return a boolean value which is true if the current device is the Group Owner. Otherwise, false.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public boolean isWifiGroupOwner() throws DeviceException {
        IWrapperWifi wrapperWifi = (IWrapperWifi) wrappers[Service.WIFI];
        if (wrapperWifi.isEnabled()) {
            return wrapperWifi.isWifiGroupOwner();
        } else {
            throw new DeviceException("Wifi is not enabled");
        }
    }

    /**
     * Method allowing to cancel a Wi-Fi connection (during the Group Owner negotiation).
     *
     * @param listenerAction a ListenerAction object which contains callback functions.
     * @throws DeviceException signals that a Device Exception exception has occurred.
     */
    public void cancelConnection(ListenerAction listenerAction) throws DeviceException {
        IWrapperWifi wrapperWifi = (IWrapperWifi) wrappers[Service.WIFI];
        if (wrapperWifi.isEnabled()) {
            wrapperWifi.cancelConnect(listenerAction);
        } else {
            throw new DeviceException("Wifi is not enabled");
        }
    }

    /**
     * Method allowing to update the ListenerApp.
     *
     * @param listenerApp a ListenerApp object which contains callback functions.
     */
    public void updateListener(ListenerApp listenerApp) {
        for (AbstractWrapper wrapper : wrappers) {
            wrapper.updateListener(listenerApp);
        }
    }

    /**
     * Method allowing to get the state (enabled/disabled) of the different technologies.
     *
     * @return an integer value which represents the number of technologies enabled.
     */
    public int checkState() {
        int enabled = 0;
        for (AbstractWrapper wrapper : wrappers) {
            if (wrapper.isEnabled()) {
                enabled++;
            }
        }
        return enabled;
    }

    /**
     * Method allowing to perform the discovery in parallel for both technologies.
     *
     * @param discovery A DiscoveryListener object which contains callback methods.
     */
    private void bothDiscovery(final DiscoveryListener discovery) {

        @SuppressLint("HandlerLeak") final Handler mHandler = new Handler(Looper.getMainLooper()) {
            // Used handler to avoid updating views in other threads than the main thread
            public void handleMessage(Message msg) {
                discovery.onDiscoveryCompleted(mapAddressDevice);
            }
        };

        // Launch discovery independently
        for (AbstractWrapper wrapper : wrappers) {
            wrapper.discovery(discovery);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    // Use pooling to check if the discovery is completed
                    while (true) {
                        Thread.sleep(POOLING_DISCOVERY);

                        boolean finished = true;
                        for (AbstractWrapper wrapper : wrappers) {
                            if (!wrapper.isDiscoveryCompleted()) {
                                finished = false;
                                break;
                            }
                        }

                        if (finished) {
                            // Used handler to avoid using runOnUiThread in main app
                            mHandler.obtainMessage(1).sendToTarget();
                            break;
                        }
                    }

                    // Reset flag to perform a new discovery
                    for (AbstractWrapper wrapper : wrappers) {
                        wrapper.resetDiscoveryFlag();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Method allowing to process the activation of the adapter.
     *
     * @param type            an integer value which represents the type of the wrapper. 1: Bluetooth and 0: Wi-Fi
     * @param success         a boolean value which is true if the adapter has been correctly enabled.
     * @param context         a Context object which gives global information about an application
     *                        environment.
     * @param listenerAdapter a ListenerAdapter object which contains callback functions.
     */
    private void processListenerAdapter(int type, boolean success, Context context,
                                        final ListenerAdapter listenerAdapter) {
        if (success) {
            try {
                wrappers[type].init(config, context);
                wrappers[type].unregisterAdapter();
                if (type == Service.BLUETOOTH) {
                    listenerAdapter.onEnableBluetooth(true);
                } else {
                    listenerAdapter.onEnableWifi(true);
                }
            } catch (IOException e) {
                if (type == Service.BLUETOOTH) {
                    listenerAdapter.onEnableBluetooth(false);
                } else {
                    listenerAdapter.onEnableWifi(false);
                }
            }
        } else {
            if (type == Service.BLUETOOTH) {
                listenerAdapter.onEnableBluetooth(false);
            } else {
                listenerAdapter.onEnableWifi(false);
            }
        }
    }

    /**
     * Method allowing to get the type of the wrapper into a String a value.
     *
     * @param type an integer value which represents the type of the wrapper. 1: Bluetooth and 0: Wi-Fi
     * @return a String value which represents the type of the wrapper.
     */
    private String getTypeString(int type) {
        switch (type) {
            case Service.BLUETOOTH:
                return "Bluetooth";
            case Service.WIFI:
                return "WiFi";
            default:
                return "Unknown";
        }
    }

    public interface ListenerBothDiscovery {
        void onDiscoveryCompleted(HashMap<String, AdHocDevice> mapAddressDevice);
    }
}
