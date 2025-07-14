package com.netplay.example.client.actions;

import com.netplay.example.shared.Constants;
import com.netplay.example.shared.messages.NetworkMessageNotification;
import com.netplay.shared.actions.NetworkAction;
import com.netplay.shared.messages.NetworkMessage;
import com.netplay.shared.messages.NetworkMessageRegistry;

public class NetworkActionNotification extends NetworkAction {
    public NetworkActionNotification() {
        super(Constants.NETWORK_MESSAGE_NOTIFICATION);
    }

    @Override
    public void perform(NetworkMessage message) throws Exception {
        NetworkMessageNotification notification = NetworkMessageRegistry.getInstance().create(message);
        System.out.println("*** " + notification.getMessage() + " ***");
    }
}