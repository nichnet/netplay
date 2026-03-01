package com.netplay.example.server;

import com.netplay.example.shared.messages.NetworkMessageChat;
import com.netplay.example.shared.messages.NetworkMessageLogin;
import com.netplay.example.shared.messages.NetworkMessageNotification;
import com.netplay.shared.Network;

public class ServerMessageHandlers {

    public static void register() {
        Network.on(NetworkMessageLogin.class, (loginMessage, senderId) -> {
            String username = loginMessage.getUsername();

            if (MyServer.getInstance().isUserLoggedIn(username)) {
                NetworkMessageNotification notification = new NetworkMessageNotification(
                    username + " is already logged in. Kicking...");
                Network.send(senderId, notification);

                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    MyServer.getInstance().kick(senderId);
                }).start();
                return;
            }

            System.out.println("User logged in: " + username);

            MyServer.getInstance().setConnectionUsername(senderId, username);

            NetworkMessageNotification notification = new NetworkMessageNotification(
                username + " joined the chat");
            Network.broadcastExcept(new String[]{senderId}, notification);
        });

        Network.on(NetworkMessageChat.class, (chatMessage, senderId) -> {
            String senderUsername = MyServer.getInstance().getConnectionUsername(senderId);

            if (senderUsername == null) {
                System.out.println("Chat message from unauthenticated user, ignoring");
                return;
            }

            System.out.println(senderUsername + ": " + chatMessage.getMessage());

            NetworkMessageChat verifiedMessage = new NetworkMessageChat(
                senderUsername, chatMessage.getMessage());
            Network.broadcastExcept(new String[]{senderId}, verifiedMessage);
        });
    }
}
