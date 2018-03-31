package com.montefiore.gaulthiergain.adhoclibrary.appframework;

import java.io.IOException;

public interface ListenerAdapter {

    void onEnableBluetooth() throws IOException;

    void onEnableWifi() throws IOException;

}
