package com.montefiore.gaulthiergain.adhoclibrary.network.aodv;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * <p>This class represents an abstract AODV message for the AODV protocol. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public abstract class AodvMessage implements Serializable {

    @JsonProperty("type")
    protected final int type;

    /**
     * Constructor
     *
     * @param type an integer value which represents the type of the AODV message.
     */
    AodvMessage(int type) {
        this.type = type;
    }

    /**
     * Method allowing to get the type of the AODV message.
     *
     * @return an integer value which represents the type of the AODV message.
     */
    protected int getType() {
        return type;
    }
}
