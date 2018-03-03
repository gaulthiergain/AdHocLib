package com.montefiore.gaulthiergain.adhoclibrary.auto;

import com.montefiore.gaulthiergain.adhoclibrary.util.MessageAdHoc;

import java.io.IOException;

public class DataLinkWifiManager implements IDataLink {
    @Override
    public void connect() {

    }

    @Override
    public void stopListening() throws IOException {

    }

    @Override
    public void sendMessage(MessageAdHoc message, String address) throws IOException {

    }

    @Override
    public boolean isDirectNeighbors(String address) {
        return false;
    }

    @Override
    public void broadcast(MessageAdHoc message) throws IOException {

    }

    @Override
    public void broadcastExcept(String originateAddr, MessageAdHoc message) throws IOException {

    }

    @Override
    public void discovery() {

    }

    @Override
    public void getPaired() {

    }
}
