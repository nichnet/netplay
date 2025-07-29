package com.netplay.example.server;

import java.util.Scanner;

public class ChatServerApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try {
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
        
        System.out.print("Enter max connections (or press Enter for 10): ");
        String maxConnInput = scanner.nextLine().trim();
        int maxConnections = 10;
        if (!maxConnInput.isEmpty()) {
            try {
                maxConnections = Integer.parseInt(maxConnInput);
            } catch (NumberFormatException e) {
                System.out.println("Invalid max connections, using 10");
                maxConnections = 10;
            }
        }
        
        MyServer server = new MyServer();
        server.setMaxConnections(maxConnections);
        server.registerHandlerPackages("com.netplay.example.server");
        
        System.out.println("Starting chat server on " + host + ":" + port + " (max " + maxConnections + " connections)");
        server.start(host, port);
        
        System.out.println("Press Enter to stop the server.");
        scanner.nextLine();
        
            server.stop();
        } catch (java.util.NoSuchElementException e) {
            // User closed the terminal/app - exit gracefully
            System.out.println("\nInput closed. Shutting down server...");
        } finally {
            scanner.close();
        }
    }
}