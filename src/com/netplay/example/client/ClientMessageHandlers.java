package com.netplay.example.client;

import com.netplay.example.shared.messages.NetworkMessageChat;
import com.netplay.example.shared.messages.NetworkMessageNotification;
import com.netplay.shared.Network;

public class ClientMessageHandlers {

    public static void register() {
        Network.on(NetworkMessageChat.class, (chatMessage, senderId) -> {
            System.out.println(chatMessage.getSender() + ": " + chatMessage.getMessage());
        });

        Network.on(NetworkMessageNotification.class, (notification, senderId) -> {
            System.out.println("Notification: " + notification.getMessage());
        });
    }
}
