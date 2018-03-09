package com.montefiore.gaulthiergain.adhoclibrary.datalink.wifi;

import java.util.HashMap;


public class MapNameAddr {
    private final static HashMap<String, String> hashmapAddrName = new HashMap<String, String>();

    public static void addMapping(String mac, String name) {
        hashmapAddrName.put(mac, name);
    }

    public static void getName(String mac) {
        hashmapAddrName.get(mac);
    }

}
