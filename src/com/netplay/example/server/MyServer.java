package com.netplay.example.server;

import com.netplay.example.server.actions.ServerActions;
import com.netplay.example.shared.Constants;
import com.netplay.example.shared.messages.NetworkMessageNotification;
import com.netplay.server.NetworkConnection;
import com.netplay.server.Server;
import com.netplay.shared.messages.NetworkMessage;

import java.util.HashMap;

public class MyServer extends Server {
    private static MyServer instance;
    private HashMap<String, String> users = new HashMap<>();

    public MyServer() {
        instance = this;
        try {
            registerMessagePackages(
                    "com.netplay.example.shared.messages",
                    "com.netplay.example.server.messages"
            );
            registerActions(new ServerActions());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUserConnected(NetworkConnection connection) {
        users.put(connection.getId(), null); // Initialize with no username yet
        System.out.println("User connected:" + connection + " (waiting for login)");
        // Note: Join notification will be sent after login message is received
    }

    @Override
    public void onUserDisconnected(NetworkConnection connection) {
        String username = users.get(connection.getId());
        System.out.println("User disconnected:" + connection);

        // Notify all remaining connected clients about the user leaving
        if (username != null) {
            String leaveMessage = username + " left the chat";

            NetworkMessageNotification notification = new NetworkMessageNotification(leaveMessage);
            NetworkMessage networkMessage = new NetworkMessage(Constants.NETWORK_MESSAGE_NOTIFICATION, notification);
            sendMessageBroadcast(networkMessage);
            users.remove(connection.getId());
        }
    }

    public String getConnectionUsername(String connectionId) {
        return users.get(connectionId);
    }

    public void setConnectionUsername(String connectionId, String username) {
        if (!users.containsKey(connectionId)) {
            return;
        }
        users.put(connectionId, username);
    }

    public boolean isUserLoggedIn(String username) {
        return users.containsValue(username);
    }

    public static MyServer getInstance() {
        return instance;
    }
}
