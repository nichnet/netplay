package com.netplay.example.client;

import com.netplay.example.shared.Constants;
import com.netplay.example.shared.messages.NetworkMessageChat;
import com.netplay.shared.messages.NetworkMessage;
import java.util.Scanner;

public class ChatClientApp {
    private static String username;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.print("Enter your username: ");
            username = scanner.nextLine().trim();
        
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty!");
            return;
        }
        
        MyClient client = new MyClient();
        client.setUsername(username);
        client.registerHandlerPackages("com.netplay.example.client");

        System.out.print("Enter server host (or press Enter for localhost): ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) {
            host = "localhost";
        }
        
        System.out.print("Enter server port (or press Enter for 8080): ");
        String portInput = scanner.nextLine().trim();
        int port = 8080;
        if (!portInput.isEmpty()) {
            try {
                port = Integer.parseInt(portInput);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port, using 8080");
                port = 8080;
            }
        }
        
        client.connect(host, port);
        
        // Wait a moment for connection
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        if (!client.isConnected()) {
            System.out.println("Failed to connect to server. Make sure the server is running or it may be full.");
            return;
        }
        
        System.out.println("Type messages and press Enter to send. Type '/quit' to exit.");

        String message;
        while (client.isConnected()) {
            if (scanner.hasNextLine()) {
                message = scanner.nextLine();
                
                if (message.equals("/quit")) {
                    break;
                }
                
                if (!message.trim().isEmpty()) {
                    NetworkMessageChat chatMessage = new NetworkMessageChat(username, message);
                    NetworkMessage networkMessage = new NetworkMessage(Constants.NETWORK_MESSAGE_CHAT, chatMessage);
                    client.sendNetworkMessage(networkMessage);
                }
            } else {
                // No more input available (probably stdin closed)
                break;
            }
        }
        
            client.disconnect();
        } catch (java.util.NoSuchElementException e) {
            // User closed the terminal/app - exit gracefully
            System.out.println("\nInput closed. Exiting...");
        } finally {
            scanner.close();
        }
    }
}