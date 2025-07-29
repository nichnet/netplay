package com.netplay.example.shared.messages;

import com.netplay.shared.NetworkSerializable;
import com.netplay.shared.NetworkSerializableProperty;

public class NetworkMessageChat implements NetworkSerializable {
    private String sender, message;

    public NetworkMessageChat() {} // Required for factory

    public NetworkMessageChat(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    @NetworkSerializableProperty(0)
    public String getSender() { return sender; }

    @NetworkSerializableProperty(1)
    public String getMessage() { return message; }


    @Override
    public String toString() {
        return "ChatMessage{sender='" + getSender() + "', message='" + getMessage() + "'}";
    }
}
