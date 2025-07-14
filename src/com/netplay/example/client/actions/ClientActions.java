package com.netplay.example.client.actions;

import com.netplay.shared.actions.NetworkActions;

public class ClientActions extends NetworkActions {
    public ClientActions() {
        registerAction(new NetworkActionChat());
        registerAction(new NetworkActionNotification());
    }
}
