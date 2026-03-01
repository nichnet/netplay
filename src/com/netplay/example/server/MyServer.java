package com.netplay.example.server;

import com.netplay.example.shared.messages.NetworkMessageNotification;
import com.netplay.server.NetworkConnection;
import com.netplay.server.Server;
import com.netplay.shared.Network;

import java.util.HashMap;

public class MyServer extends Server {
    private static MyServer instance;
    private HashMap<String, String> users = new HashMap<>();

    public MyServer() {
        instance = this;
    }

    @Override
    public void onUserConnected(NetworkConnection connection) {
        users.put(connection.getId(), null);
        System.out.println("User connected: " + connection + " (waiting for login)");
    }

    @Override
    public void onUserDisconnected(NetworkConnection connection) {
        String username = users.get(connection.getId());
        System.out.println("User disconnected: " + connection);

        if (username != null) {
            String leaveMessage = username + " left the chat";
            NetworkMessageNotification notification = new NetworkMessageNotification(leaveMessage);
            Network.broadcast(notification);
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
