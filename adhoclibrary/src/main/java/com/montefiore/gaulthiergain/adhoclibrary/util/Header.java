package com.montefiore.gaulthiergain.adhoclibrary.util;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.io.Serializable;

/**
 * Created by gaulthiergain on 17/11/17.
 */

@JsonTypeName("Header")
public class Header implements Serializable {
    protected int type;
    protected String label;
    protected String name;

    public Header() {

    }

    public Header(int type, String label, String name) {
        this.type = type;
        this.label = label;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "SHeader{" +
                "type=" + type +
                ", label='" + label + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}