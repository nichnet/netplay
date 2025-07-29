package com.netplay.example.shared.messages;

import com.netplay.shared.NetworkSerializable;
import com.netplay.shared.NetworkSerializableProperty;

public class NetworkMessageNotification extends NetworkSerializable {
    private String message;

    public NetworkMessageNotification() {} // Required for factory

    public NetworkMessageNotification(String message) {
        this.message = message;
    }

    @NetworkSerializableProperty(0)
    public String getMessage() { return message; }

    public static NetworkMessageNotification deserialize(byte[] data) throws Exception {
        NetworkMessageNotification msg = new NetworkMessageNotification();
        msg.readFrom(data);
        return msg;
    }

    @Override
    public String toString() {
        return "Notification{message='" + getMessage() + "'}";
    }
}