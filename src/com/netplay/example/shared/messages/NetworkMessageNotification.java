package com.netplay.example.shared.messages;

import com.netplay.shared.NetworkSerializable;
import com.netplay.shared.NetworkSerializableProperty;

public class NetworkMessageNotification implements NetworkSerializable {
    private String message;

    public NetworkMessageNotification() {} // Required for factory

    public NetworkMessageNotification(String message) {
        this.message = message;
    }

    @NetworkSerializableProperty(0)
    public String getMessage() { return message; }


    @Override
    public String toString() {
        return "Notification{message='" + getMessage() + "'}";
    }
}