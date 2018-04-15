package com.montefiore.gaulthiergain.adhoclibrary.appframework;

public interface ListenerAction {

    void onSuccess();

    void onFailure(Exception e);
}