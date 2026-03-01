package com.netplay.example.client;

import com.netplay.client.Client;
import com.netplay.example.shared.messages.NetworkMessageLogin;

public class MyClient extends Client {
    private static MyClient instance;

    private String username;

    public MyClient() {
        instance = this;
    }

    @Override
    public void onConnected() {
        System.out.println("Connected to Server.");

        if (username != null) {
            sendLoginMessage(username);
        }
    }

    @Override
    public void onConnectionFailed() {
        System.out.println("Failed to connect to server.");
    }

    private void sendLoginMessage(String username) {
        NetworkMessageLogin loginMessage = new NetworkMessageLogin(username);
        send(loginMessage);
    }

    @Override
    public void onDisconnected() {
        System.out.println("Disconnected from Server.");
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public static MyClient getInstance() {
        return instance;
    }
}
