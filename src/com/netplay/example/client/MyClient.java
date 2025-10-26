package com.netplay.example.client;

import com.netplay.client.Client;
import com.netplay.example.shared.Constants;
import com.netplay.example.shared.messages.NetworkMessageChat;
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

        // TEST: Send really long messages to test chunking (> 1024 bytes)
        System.out.println("Sending test long messages...");
        sendTestLongMessage();

        // Send a second large message to test the original bug scenario
        try {
            Thread.sleep(100); // Small delay between messages
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        sendTestLongMessage2();
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

    private void sendTestLongMessage() {
        // Create a message that's approximately 2000 bytes (definitely > 1024)
        StringBuilder longText = new StringBuilder();
        longText.append("=== LARGE MESSAGE TEST #1 === ");

        // Add repetitive content to reach ~2000 bytes
        for (int i = 0; i < 50; i++) {
            longText.append("This is test message number ").append(i).append(". ");
            longText.append("Testing chunking protocol with large messages over 1024 bytes. ");
        }

        longText.append("=== END OF LARGE MESSAGE TEST #1 ===");

        String messageContent = longText.toString();
        System.out.println("Test message #1 size: ~" + messageContent.length() + " characters");

        NetworkMessageChat testMessage = new NetworkMessageChat(username, messageContent);
        NetworkMessage networkMessage = new NetworkMessage(Constants.NETWORK_MESSAGE_CHAT, testMessage);
        sendNetworkMessage(networkMessage);

        System.out.println("Long message #1 sent successfully!");
    }

    private void sendTestLongMessage2() {
        // Create another large message to test multiple large messages in sequence
        StringBuilder longText = new StringBuilder();
        longText.append("=== LARGE MESSAGE TEST #2 === ");

        // Different content for the second message
        for (int i = 0; i < 60; i++) {
            longText.append("Second large message, iteration ").append(i).append(". ");
            longText.append("This tests that the chunking system can handle consecutive large messages. ");
        }

        longText.append("=== END OF LARGE MESSAGE TEST #2 ===");

        String messageContent = longText.toString();
        System.out.println("Test message #2 size: ~" + messageContent.length() + " characters");

        NetworkMessageChat testMessage = new NetworkMessageChat(username, messageContent);
        NetworkMessage networkMessage = new NetworkMessage(Constants.NETWORK_MESSAGE_CHAT, testMessage);
        sendNetworkMessage(networkMessage);

        System.out.println("Long message #2 sent successfully!");
    }

    public static MyClient getInstance() {
        return instance;
    }
}
