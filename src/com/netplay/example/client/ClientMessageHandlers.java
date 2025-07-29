package com.netplay.example.client;

import com.netplay.example.shared.Constants;
import com.netplay.example.shared.messages.NetworkMessageChat;
import com.netplay.example.shared.messages.NetworkMessageNotification;
import com.netplay.shared.events.NetworkEventHandler;
import com.netplay.shared.messages.NetworkMessage;

public class ClientMessageHandlers {
    
    @NetworkEventHandler(Constants.NETWORK_MESSAGE_CHAT)
    public void handleChatMessage(NetworkMessage message) throws Exception {
        NetworkMessageChat chatMessage = message.deserialize(NetworkMessageChat.class);
        System.out.println(chatMessage.getSender() + ": " + chatMessage.getMessage());
    }
    
    @NetworkEventHandler(Constants.NETWORK_MESSAGE_NOTIFICATION)
    public void handleNotificationMessage(NetworkMessage message) throws Exception {
        NetworkMessageNotification notification = message.deserialize(NetworkMessageNotification.class);
        System.out.println("Notification: " + notification.getMessage());
    }
}