package com.netplay.example.client.actions;

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
        System.out.println(chatMessage.getSender() + ": " + chatMessage.getMessage());
    }
}
