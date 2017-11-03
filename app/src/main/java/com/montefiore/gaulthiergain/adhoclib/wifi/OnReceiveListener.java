package com.montefiore.gaulthiergain.adhoclib.wifi;

import android.content.Context;

import com.montefiore.gaulthiergain.adhoclib.bluetooth.BluetoothAdHocDevice;

import java.util.HashMap;

/**
 * Created by gaulthiergain on 26/10/17.
 *
 */

public interface OnReceiveListener {
    void OnReceive(Context context, String str);
}
