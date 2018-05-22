package com.montefiore.gaulthiergain.adhoclibrary.datalink.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * <p>This class contains stateless and utilities functions.</p>
 *
 * @author Gaulthier Gain
 * @version 1.0
 */
public class Utils {

    /**
     * Method allowing to deserialize a byte array to a MessageAdHoc object.
     *
     * @param byteArray an array of bytes which represents a MessageAdHoc object serialized as bytes.
     * @return a MessageAdHoc object which represents the message to send through the network.
     * @throws IOException            signals that an I/O exception of some sort has occurred.
     * @throws ClassNotFoundException signals that a class was not found.
     */
    public static MessageAdHoc deserialize(byte[] byteArray) throws IOException, ClassNotFoundException {

        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        ObjectInput in = new ObjectInputStream(bis);
        MessageAdHoc messageAdHoc = (MessageAdHoc) in.readObject();
        in.close();
        return messageAdHoc;
    }

    /**
     * Method allowing to serialize a MessageAdHoc object to a byte array.
     *
     * @param messageAdHoc a MessageAdHoc object which represents the message to send through
     *                     the network.
     * @return an array of bytes which represents a MessageAdHoc object serialized as bytes.
     * @throws IOException signals that an I/O exception of some sort has occurred.
     */
    public static byte[] serialize(MessageAdHoc messageAdHoc) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(messageAdHoc);
        out.flush();
        byte[] byteArray = bos.toByteArray();
        bos.close();
        return byteArray;
    }
}
