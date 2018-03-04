package com.montefiore.gaulthiergain.adhoclibrary.util;

import android.util.Log;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Created by gaulthiergain on 25/10/17.
 * Test Class
 *
 */

public class UtilInterfaces {

    private ArrayList<NetworkInterface> arrayNetworkInterfaces;


    public UtilInterfaces() {
    }

    public ArrayList<NetworkInterface> getAllNetworkInterfaces(){

        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            arrayNetworkInterfaces = new ArrayList<>();
            arrayNetworkInterfaces = Collections.list(ifs);
        } catch (SocketException ex) {
            Log.d("[ADHOC] ERROR: ", "Error");
            return null;
        }

        return arrayNetworkInterfaces;

    }
}
