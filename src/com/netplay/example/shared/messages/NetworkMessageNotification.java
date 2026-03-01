package com.netplay.example.shared.messages;

public class NetworkMessageNotification {
    private String message;

    public NetworkMessageNotification() {}

    public NetworkMessageNotification(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Notification{message='" + message + "'}";
    }
}
