package com.netplay.example.server.actions;

import com.netplay.example.server.MyServer;
import com.netplay.example.shared.Constants;
import com.netplay.example.shared.messages.NetworkMessageLogin;
import com.netplay.example.shared.messages.NetworkMessageNotification;
import com.netplay.shared.actions.NetworkAction;
import com.netplay.shared.messages.NetworkMessage;
import com.netplay.shared.messages.NetworkMessageRegistry;

public class NetworkActionLogin extends NetworkAction {
    public NetworkActionLogin() {
        super(Constants.NETWORK_MESSAGE_LOGIN);
    }

    @Override
    public void perform(NetworkMessage message) throws Exception {
        NetworkMessageLogin loginMessage = NetworkMessageRegistry.getInstance().create(message);
        String username = loginMessage.getUsername();
        String connectionId = message.getSenderConnectionId();

        if (MyServer.getInstance().isUserLoggedIn(username)) {
            // Notify the user that this user already exists
            NetworkMessageNotification notification = new NetworkMessageNotification(username + " is already logged in. Kicking...");
            NetworkMessage networkMessage = new NetworkMessage(Constants.NETWORK_MESSAGE_NOTIFICATION, notification);
            MyServer.getInstance().sendMessageUnicast(connectionId, networkMessage);

            // Give a moment for the message to be sent before kicking
            new Thread(() -> {
                try {
                    Thread.sleep(100); // Small delay to ensure message delivery
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                MyServer.getInstance().kick(connectionId);
            }).start();
            return;
        }
        
        System.out.println("User logged in: " + username);
        
        // Set the username for this connection
        MyServer.getInstance().setConnectionUsername(connectionId, username);
        
        // Send notification to all clients about the user joining
        NetworkMessageNotification notification = new NetworkMessageNotification(username + " joined the chat");
        NetworkMessage networkMessage = new NetworkMessage(Constants.NETWORK_MESSAGE_NOTIFICATION, notification);
        MyServer.getInstance().sendMessageBroadcastExcept(new String[]{
            connectionId
        }, networkMessage);
    }
}