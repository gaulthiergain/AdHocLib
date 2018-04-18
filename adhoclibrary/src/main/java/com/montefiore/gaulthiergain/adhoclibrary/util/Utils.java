package com.montefiore.gaulthiergain.adhoclibrary.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class Utils {

    public static int getBatteryPercentage(Context context) {

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
    }

    public static MessageAdHoc deserialize(byte[] byteArray) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        ObjectInput in;

        in = new ObjectInputStream(bis);
        MessageAdHoc messageAdHoc = (MessageAdHoc) in.readObject();
        in.close();
        return messageAdHoc;
    }

    public static byte[] serialize(MessageAdHoc messageAdHoc) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;

        out = new ObjectOutputStream(bos);
        out.writeObject(messageAdHoc);
        out.flush();
        byte[] byteArray = bos.toByteArray();
        bos.close();
        return byteArray;
    }
}
