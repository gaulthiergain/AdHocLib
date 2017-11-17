package com.montefiore.gaulthiergain.adhoclibrary.util;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 17/11/17.
 */

public class MessageAdHoc implements Serializable {
    private Header header;
    private Serializable pdu;

    public MessageAdHoc(Header header, Serializable pdu) {
        this.header = header;
        this.pdu = pdu;
    }

    public Header getHeader() {
        return header;
    }

    public Serializable getPdu() {
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
