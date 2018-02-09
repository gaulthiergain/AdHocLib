package com.montefiore.gaulthiergain.adhoclibrary.aodv;

/**
 * This class represents the different status code used in AODV protocol.
 *
 * @author Gain Gaulthier
 */
public enum TypeAodv {

    /**
     * Represents the different types used in AODV protocol
     */
    RREQ(1, "RREQ", "Route Request"),
    RREP(2, "RREP", "Route Reply"),
    RERR(3, "RERR", "Route Error"),
    RREP_ACK(4, "RREP_ACK", "Route Reply Acknowledgment"),

    DATA(8, "DATA", "Data"),
    DATA_ACK(9, "DATA_ACK", "Data Acknowledgment");

    private final int type;
    private final String code;
    private final String label;

    TypeAodv(int type, String code, String label) {
        this.type = type;
        this.code = code;
        this.label = label;
    }

    /**
     * Method allowing to get the AODV type.
     *
     * @return the aodv type number.
     */
    public int getType() {
        return type;
    }

    /**
     * Method allowing to get the AODV code.
     *
     * @return the aodv code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Method allowing to get the label.
     *
     * @return the label of the AODV type.
     */
    public String getLabel() {
        return label;
    }
}
