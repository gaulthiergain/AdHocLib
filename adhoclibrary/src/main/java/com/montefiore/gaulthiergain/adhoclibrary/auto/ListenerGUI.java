package com.montefiore.gaulthiergain.adhoclibrary.auto;

import com.montefiore.gaulthiergain.adhoclibrary.bluetooth.BluetoothAdHocDevice;

import java.util.HashMap;

/**
 * Created by gaulthiergain on 12/12/17.
 */

public interface ListenerGUI {

    void onDiscoveryCompleted(HashMap<String, BluetoothAdHocDevice> hashMapBluetoothDevice);
}
