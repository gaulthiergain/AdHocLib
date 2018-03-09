package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import java.util.HashMap;


public class MapNameAddr {
    private final static HashMap<String, String> hashmapNameAddr = new HashMap<String, String>();

    public static void addMapping(String ip, String name) {
        hashmapNameAddr.put(ip, name);
    }

    public static void getMapping(String ip) {
        hashmapNameAddr.get(ip);
    }

}
