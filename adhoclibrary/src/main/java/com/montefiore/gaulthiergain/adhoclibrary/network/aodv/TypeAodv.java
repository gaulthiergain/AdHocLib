package com.montefiore.gaulthiergain.adhoclibrary.network.aodv;


/**
 * <p>This class represents the different status code used in AODV protocol.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */

public enum TypeAodv {

    /**
     * Represents the different types used in AODV protocol
     */
    RREQ(Constants.RREQ, "RREQ", "Route Request"),
    RREP(Constants.RREP, "RREP", "Route Reply"),
    RERR(Constants.RERR, "RERR", "Route Error"),
    RREP_GRATUITOUS(Constants.RREP_GRATUITOUS, "RREP_GRATUITOUS", "Gratuitous Route Reply"),
    DATA(Constants.DATA, "DATA", "Data"),
    HELLO(Constants.HELLO, "HELLO", "Hello");

    private final int type;
    private final String code;
    private final String label;

    /**
     * Constructor
     *
     * @param type  an integer value which represents the type of AODV message.
     * @param code  a String value which represents the code of AODV message.
     * @param label a String value which represents the label of AODV message.
     */
    TypeAodv(int type, String code, String label) {
        this.type = type;
        this.code = code;
        this.label = label;
    }

    /**
     * Method allowing to get the AODV type.
     *
     * @return an integer value which represents the AODV type number.
     */
    public int getType() {
        return type;
    }

    /**
     * Method allowing to get the AODV code.
     *
     * @return a String value which represents the AODV code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Method allowing to get the label.
     *
     * @return a String value which represents the label of the AODV type.
     */
    public String getLabel() {
        return label;
    }
}