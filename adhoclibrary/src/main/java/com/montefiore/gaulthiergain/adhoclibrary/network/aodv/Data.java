package com.montefiore.gaulthiergain.adhoclibrary.network.aodv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.json.DataDeserializer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.json.Serializer;

import java.io.Serializable;

/**
 * <p>This class represents a DATA message and all theses fields for the AODV protocol. </p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
@JsonTypeName("Data")
@JsonDeserialize(using = DataDeserializer.class)
public class Data implements Serializable {

    @JsonProperty("destIpAddress")
    private String destIpAddress;

    @JsonProperty("payload")
    @JsonSerialize(using = Serializer.class)
    private Object payload;

    /**
     * Default constructor
     */
    public Data() {
        destIpAddress = "";
        payload = null;
    }

    /**
     * Constructor
     *
     * @param destIpAddress a String value which represents the destination IP address of the
     *                      DATA message.
     * @param payload       a Serializable value which represents the PDU of the DATA message.
     */
    public Data(String destIpAddress, Object payload) {
        this.destIpAddress = destIpAddress;
        this.payload = payload;
    }

    public void setDestIpAddress(String destIpAddress) {
        this.destIpAddress = destIpAddress;
    }

    /**
     * Method allowing to get the destination IP address of the DATA message.
     *
     * @return a String value which represents the destination IP address of the DATA message.
     */
    public String getDestIpAddress() {
        return destIpAddress;
    }

    /**
     * Method allowing to get the PDU of the DATA message.
     *
     * @return a Serializable object which represents the PDU of the DATA message.
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * Method allowing to set the PDU of the DATA message.
     *
     * @param payload a Serializable object which represents the PDU of the DATA message.
     */
    public void setPayload(Object payload) {
        this.payload = payload;
    }


    @Override
    public String toString() {
        return "Data{" +
                "destIpAddress='" + destIpAddress + '\'' +
                ", payload=" + payload +
                '}';
    }
}