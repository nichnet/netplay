package com.netplay.example.server.actions;

import com.netplay.example.server.MyServer;
import com.netplay.example.shared.Constants;
import com.netplay.example.shared.messages.NetworkMessageChat;
import com.netplay.shared.actions.NetworkAction;
import com.netplay.shared.messages.NetworkMessage;
import com.netplay.shared.messages.NetworkMessageRegistry;

public class NetworkActionChat extends NetworkAction {
    public NetworkActionChat() {
        super(Constants.NETWORK_MESSAGE_CHAT);
    }

    @Override
    public void perform(NetworkMessage message) throws Exception {
        NetworkMessageChat chatMessage = NetworkMessageRegistry.getInstance().create(message);
        String senderUsername = MyServer.getInstance().getConnectionUsername(message.getSenderConnectionId());
        
        // Don't trust the client's claim of the sender name - use server-side username
        if (senderUsername == null) {
            System.out.println("Chat message from unauthenticated user, ignoring");
            return;
        }
        
        System.out.println(senderUsername + ": " + chatMessage.getMessage());

        // Create new message with verified username and broadcast it
        NetworkMessageChat verifiedMessage = new NetworkMessageChat(senderUsername, chatMessage.getMessage());
        NetworkMessage broadcastMessage = new NetworkMessage(Constants.NETWORK_MESSAGE_CHAT, verifiedMessage);
        
        MyServer.getInstance().sendMessageBroadcastExcept(new String[] {
            message.getSenderConnectionId(),
        }, broadcastMessage);
    }
}
