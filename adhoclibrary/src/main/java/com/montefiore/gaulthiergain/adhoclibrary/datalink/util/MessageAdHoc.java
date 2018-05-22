package com.montefiore.gaulthiergain.adhoclibrary.datalink.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.json.MessageDeserializer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.json.Serializer;

import java.io.Serializable;

/**
 * <p>This class represents and defines the format of the messages exchanged by applications using
 * the library.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
@JsonDeserialize(using = MessageDeserializer.class)
public class MessageAdHoc implements Serializable {
    @JsonProperty("header")
    private Header header;

    @JsonSerialize(using = Serializer.class)
    @JsonProperty("pdu")
    private Object pdu;

    /**
     * Default constructor
     */
    public MessageAdHoc() {
    }

    /**
     * Constructor
     *
     * @param header a Header object which represents the header information of a message.
     */
    public MessageAdHoc(Header header) {
        this.header = header;
    }

    /**
     * Constructor
     *
     * @param header a Header object which represents the header information of a message.
     * @param pdu    a generic object which represents the PDU (Data) of a message.
     */
    public MessageAdHoc(Header header, Object pdu) {
        this.header = header;
        this.pdu = pdu;
    }

    /**
     * Method allowing to set the Header of a message.
     *
     * @param header a Header object which represents the header information of a message.
     */
    public void setHeader(Header header) {
        this.header = header;
    }

    /**
     * Method allowing to set the PDU (Data) of a message.
     *
     * @param pdu a generic object which represents the PDU of a message.
     */
    public void setPdu(Object pdu) {
        this.pdu = pdu;
    }

    /**
     * Method allowing to get the header of the message.
     *
     * @return a Header object which represents the header information of a message.
     */
    public Header getHeader() {
        return header;
    }

    /**
     * Method allowing to get the PDU (Data) of the message.
     *
     * @return a generic object which represents the PDU of a message.
     */
    public Object getPdu() {
        return pdu;
    }

    @Override
    public String toString() {
        return "MessageAdHoc{" +
                "header=" + header +
                ", pdu=" + pdu +
                '}';
    }
}
