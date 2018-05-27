package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAction;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;

/**
 * <p>This interface allows to define common specifications/functions for Wi-Fi wrappers.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public interface IWrapperWifi {

    /**
     * Method allowing to remove a current Wi-Fi group.
     *
     * @param listenerAction a ListenerAction object which contains callback functions.
     */
    void removeGroup(ListenerAction listenerAction);

    /**
     * Method allowing to update the Group Owner value to influence the choice of the Group Owner
     * negotiation.
     *
     * @param valueGroupOwner an integer value between 0 and 15 where 0 indicates the least
     *                        inclination to be a group owner and 15 indicates the highest inclination
     *                        to be a group owner. A value of -1 indicates the system can choose
     *                        an appropriate value.
     * @throws GroupOwnerBadValue signals that the value for the Group Owner intent is invalid.
     */
    void setGroupOwnerValue(int valueGroupOwner) throws GroupOwnerBadValue;

    /**
     * Method allowing to cancel a Wi-Fi connection (during the Group Owner negotiation).
     *
     * @param listenerAction a ListenerAction object which contains callback functions.
     */
    void cancelConnect(ListenerAction listenerAction);

    /**
     * Method allowing to check if Wi-Fi is enabled.
     *
     * @return a boolean value which is true if  Wi-Fi has is enabled. Otherwise, false.
     */
    boolean isEnabled();

    /**
     * Method allowing to check if the current device is the Group Owner.
     *
     * @return a boolean value which is true if the current device is the Group Owner. Otherwise, false.
     */
    boolean isWifiGroupOwner();
}
