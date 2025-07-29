package com.netplay.example.client;

import com.netplay.client.Client;
import com.netplay.example.shared.Constants;
import com.netplay.example.shared.messages.NetworkMessageLogin;
import com.netplay.shared.messages.NetworkMessage;

public class MyClient extends Client {
    private static MyClient instance;

    private String username;

    public MyClient() {
        instance = this;
    }

    @Override
    public void onConnected() {
        System.out.println("Connected to Server.");
        
        // Send login message with username
        if (username != null) {
            sendLoginMessage(username);
        }
    }
    
    private void sendLoginMessage(String username) {
        NetworkMessageLogin loginMessage = new NetworkMessageLogin(username);
        NetworkMessage networkMessage = new NetworkMessage(Constants.NETWORK_MESSAGE_LOGIN, loginMessage);
        sendNetworkMessage(networkMessage);
    }

    @Override
    public void onDisconnected() {
        System.out.println("Disconnected from Server.");
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public static MyClient getInstance() {
        return instance;
    }
}
