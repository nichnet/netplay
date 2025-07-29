package com.netplay.example.server;

import com.netplay.example.server.MyServer;
import com.netplay.example.shared.Constants;
import com.netplay.example.shared.messages.NetworkMessageChat;
import com.netplay.example.shared.messages.NetworkMessageLogin;
import com.netplay.example.shared.messages.NetworkMessageNotification;
import com.netplay.shared.events.NetworkEventHandler;
import com.netplay.shared.messages.NetworkMessage;

public class ServerMessageHandlers {
    
    @NetworkEventHandler(Constants.NETWORK_MESSAGE_LOGIN)
    public void handleLoginMessage(NetworkMessage message) throws Exception {
        NetworkMessageLogin loginMessage = message.deserialize(NetworkMessageLogin.class);
        String username = loginMessage.getUsername();
        String connectionId = message.getSenderConnectionId();

        if (MyServer.getInstance().isUserLoggedIn(username)) {
            NetworkMessageNotification notification = new NetworkMessageNotification(username + " is already logged in. Kicking...");
            NetworkMessage networkMessage = new NetworkMessage(Constants.NETWORK_MESSAGE_NOTIFICATION, notification);
            MyServer.getInstance().sendMessageUnicast(connectionId, networkMessage);

            new Thread(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                MyServer.getInstance().kick(connectionId);
            }).start();
            return;
        }
        
        System.out.println("User logged in: " + username);
        
        MyServer.getInstance().setConnectionUsername(connectionId, username);
        
        NetworkMessageNotification notification = new NetworkMessageNotification(username + " joined the chat");
        NetworkMessage networkMessage = new NetworkMessage(Constants.NETWORK_MESSAGE_NOTIFICATION, notification);
        MyServer.getInstance().sendMessageBroadcastExcept(new String[]{
            connectionId
        }, networkMessage);
    }
    
    @NetworkEventHandler(Constants.NETWORK_MESSAGE_CHAT)
    public void handleChatMessage(NetworkMessage message) throws Exception {
        NetworkMessageChat chatMessage = message.deserialize(NetworkMessageChat.class);
        String senderUsername = MyServer.getInstance().getConnectionUsername(message.getSenderConnectionId());
        
        if (senderUsername == null) {
            System.out.println("Chat message from unauthenticated user, ignoring");
            return;
        }
        
        System.out.println(senderUsername + ": " + chatMessage.getMessage());

        NetworkMessageChat verifiedMessage = new NetworkMessageChat(senderUsername, chatMessage.getMessage());
        NetworkMessage broadcastMessage = new NetworkMessage(Constants.NETWORK_MESSAGE_CHAT, verifiedMessage);
        
        MyServer.getInstance().sendMessageBroadcastExcept(new String[] {
            message.getSenderConnectionId(),
        }, broadcastMessage);
    }
}