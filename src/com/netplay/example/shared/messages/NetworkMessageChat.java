package com.netplay.example.shared.messages;

import com.netplay.example.shared.Constants;
import com.netplay.shared.NetworkSerializable;
import com.netplay.shared.NetworkSerializableProperty;
import com.netplay.shared.messages.NetworkMessageHandler;

@NetworkMessageHandler(value = Constants.NETWORK_MESSAGE_CHAT, compressed = true)
public class NetworkMessageChat extends NetworkSerializable {
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

    public static NetworkMessageChat deserialize(byte[] data) throws Exception {
        NetworkMessageChat msg = new NetworkMessageChat();
        msg.readFrom(data);
        return msg;
    }

    @Override
    public String toString() {
        return "ChatMessage{sender='" + getSender() + "', message='" + getMessage() + "'}";
    }
}
