package com.montefiore.gaulthiergain.adhoclibrary.datalink.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class Utils {

    public static com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc deserialize(byte[] byteArray) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        ObjectInput in;

        in = new ObjectInputStream(bis);
        com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc messageAdHoc = (com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc) in.readObject();
        in.close();
        return messageAdHoc;
    }

    public static byte[] serialize(com.montefiore.gaulthiergain.adhoclibrary.datalink.util.MessageAdHoc messageAdHoc) throws IOException {
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
