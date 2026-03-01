package com.netplay.example.shared.messages;

public class NetworkMessageChat {
    private String sender;
    private String message;

    public NetworkMessageChat() {}

    public NetworkMessageChat(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ChatMessage{sender='" + sender + "', message='" + message + "'}";
    }
}
