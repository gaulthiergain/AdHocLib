package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.appframework.ListenerAction;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.exceptions.GroupOwnerBadValue;

public interface IWrapperWifi {

    void removeGroup(ListenerAction listenerAction);

    void setGroupOwnerValue(int valueGroupOwner) throws GroupOwnerBadValue;

    void cancelConnect(ListenerAction listenerAction);

    boolean isEnabled();
}
