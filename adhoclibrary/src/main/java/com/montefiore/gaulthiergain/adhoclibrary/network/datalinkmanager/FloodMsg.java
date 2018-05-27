package com.montefiore.gaulthiergain.adhoclibrary.network.datalinkmanager;

import com.montefiore.gaulthiergain.adhoclibrary.datalink.service.AdHocDevice;

import java.io.Serializable;
import java.util.HashSet;

/**
 * <p>This class represents a message exchanged if the connection flooding is enabled. it
 * encapsulates information about remote nodes' neighbours </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class FloodMsg implements Serializable {

    private String id;
    private HashSet<AdHocDevice> adHocDevices;

    /**
     * Default constructor
     */
    public FloodMsg() {
    }

    /**
     * Constructor
     *
     * @param id           an integer value which identifies uniquely a message.
     * @param adHocDevices a HashSet<AdHocDevice> object which contains remote nodes' neighbours.
     */
    FloodMsg(String id, HashSet<AdHocDevice> adHocDevices) {
        this.id = id;
        this.adHocDevices = adHocDevices;
    }

    /**
     * Method allowing to get the id.
     *
     * @return an integer value which identifies uniquely a message.
     */
    public String getId() {
        return id;
    }

    /**
     * Method allowing to get the hashset that contains remote nodes' neighbours..
     *
     * @return a HashSet<AdHocDevice> which contains remote nodes' neighbours.
     */
    public HashSet<AdHocDevice> getAdHocDevices() {
        return adHocDevices;
    }

    @Override
    public String toString() {
        return "Test{" +
                "id=" + id +
                ", adHocDevices=" + adHocDevices +
                '}';
    }
}
