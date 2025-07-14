package com.netplay.example.server.actions;

import com.netplay.shared.actions.NetworkActions;

public class ServerActions extends NetworkActions {
    public ServerActions() {
        registerAction(new NetworkActionChat());
        registerAction(new NetworkActionLogin());
    }
}
