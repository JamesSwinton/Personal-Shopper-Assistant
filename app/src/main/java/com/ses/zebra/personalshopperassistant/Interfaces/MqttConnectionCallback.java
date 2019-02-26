package com.ses.zebra.personalshopperassistant.Interfaces;

public interface MqttConnectionCallback {
    void onConnected();
    void onConnectionLost();
}
