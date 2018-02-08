package com.montefiore.gaulthiergain.adhoclibrary.util;

import com.montefiore.gaulthiergain.adhoclibrary.aodv.RREQ;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 17/11/17.
 */

public class MessageAdHoc implements Serializable {
    private IHeader header;
    private Serializable pdu;

    public MessageAdHoc(IHeader header, Serializable pdu) {
        this.header = header;
        this.pdu = pdu;
    }

    public IHeader getHeader() {
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

    public void setPdu(Serializable pdu) {
        this.pdu = pdu;
    }

    public void setHeader(Header header) {
        this.header = header;
    }
}
