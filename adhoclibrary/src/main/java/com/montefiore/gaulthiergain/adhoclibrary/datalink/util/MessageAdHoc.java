package com.montefiore.gaulthiergain.adhoclibrary.datalink.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.json.MessageDeserializer;
import com.montefiore.gaulthiergain.adhoclibrary.datalink.json.Serializer;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 17/11/17.
 */

@JsonDeserialize(using = MessageDeserializer.class)
public class MessageAdHoc implements Serializable {
    @JsonProperty("header")
    private Header header;

    @JsonSerialize(using = Serializer.class)
    @JsonProperty("pdu")
    private Object pdu;

    public MessageAdHoc() {
    }

    public MessageAdHoc(Header header) {
        this.header = header;
    }

    public MessageAdHoc(Header header, Object pdu) {
        this.header = header;
        this.pdu = pdu;
    }

    public Header getHeader() {
        return header;
    }

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

    public void setPdu(Object pdu) {
        this.pdu = pdu;
    }

    public void setHeader(Header header) {
        this.header = header;
    }
}