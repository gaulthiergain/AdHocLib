package com.montefiore.gaulthiergain.adhoclibrary.network.aodv;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public abstract class AodvMessage implements Serializable{

    @JsonProperty("type")
    protected final int type;

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
